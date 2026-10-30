package co.uniproject.agente.consumo;

import co.uniproject.agente.destinatarios.Destinatario;
import co.uniproject.agente.destinatarios.ReglasDestinatarios;
import co.uniproject.agente.destinatarios.Variante;
import co.uniproject.agente.idempotencia.RegistroProcesados;
import co.uniproject.agente.plantillas.Aviso;
import co.uniproject.agente.plantillas.PlantillaException;
import co.uniproject.agente.plantillas.RenderizadorAvisos;
import co.uniproject.agente.telegram.EnviadorTelegram;
import co.uniproject.agente.telegram.ResultadoEnvio;
import co.uniproject.agente.vinculacion.ClienteNucleo;
import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsumidorNotificacionesTest {

    private static final long TAG = 7L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-24T19:05:00Z"), ZoneId.of("America/Bogota"));

    private CodecEventos codec;
    private ReglasDestinatarios reglas;
    private RenderizadorAvisos renderizador;
    private EnviadorTelegram enviador;
    private RegistroProcesados registro;
    private ClienteNucleo clienteNucleo;
    private Channel canal;
    private ConsumidorNotificaciones consumidor;

    private Destinatario d1;
    private Destinatario d2;
    private EventoCloud eventoValido;
    private Message mensajeValido;

    @BeforeEach
    void setUp() {
        codec = new CodecEventos();
        reglas = mock(ReglasDestinatarios.class);
        renderizador = mock(RenderizadorAvisos.class);
        enviador = mock(EnviadorTelegram.class);
        registro = mock(RegistroProcesados.class);
        clienteNucleo = mock(ClienteNucleo.class);
        canal = mock(Channel.class);
        consumidor = new ConsumidorNotificaciones(codec, reglas, renderizador, enviador, registro, clienteNucleo);

        d1 = new Destinatario(103L, 900000103L, Variante.NUEVO);
        d2 = new Destinatario(101L, 900000101L, Variante.ANTERIOR);

        eventoValido = EventoCloud.nuevo(EjemplosEventos.tareaReasignada(), RELOJ);
        mensajeValido = MessageBuilder.withBody(codec.serializar(eventoValido).getBytes(UTF_8)).build();

        when(reglas.destinatarios(any())).thenReturn(List.of(d1, d2));
        when(renderizador.renderizar(any(), any())).thenReturn(new Aviso("<b>x</b>", "Ver tarea", "/tareas/45"));
        when(registro.yaProcesado(any(), anyLong())).thenReturn(false);
    }

    @AfterEach
    void verificarCanal() throws IOException {
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
        int acks = mockingDetails(canal).getInvocations().stream()
                .filter(inv -> inv.getMethod().getName().equals("basicAck"))
                .toList().size();
        int rejects = mockingDetails(canal).getInvocations().stream()
                .filter(inv -> inv.getMethod().getName().equals("basicReject"))
                .toList().size();
        assertThat(acks + rejects).isEqualTo(1);
    }

    @Test
    void todosEnviadosConfirma() throws IOException {
        when(enviador.enviar(anyLong(), any())).thenReturn(ResultadoEnvio.ENVIADO);

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(canal).basicAck(TAG, false);
        verify(registro).registrar(eventoValido.id(), 103L);
        verify(registro).registrar(eventoValido.id(), 101L);
    }

    @Test
    void eventoInvalidoVaAFallidos() throws IOException {
        Message mensajeInvalido = MessageBuilder.withBody("{}".getBytes(UTF_8)).build();

        consumidor.recibir(mensajeInvalido, canal, TAG);

        verify(canal).basicReject(TAG, false);
        verifyNoInteractions(reglas);
    }

    @Test
    void sinDestinatariosConfirma() throws IOException {
        when(reglas.destinatarios(any())).thenReturn(List.of());

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(canal).basicAck(TAG, false);
        verifyNoInteractions(enviador);
    }

    @Test
    void duplicadoNoReenvia() throws IOException {
        when(registro.yaProcesado(any(), anyLong())).thenReturn(true);

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(canal).basicAck(TAG, false);
        verifyNoInteractions(enviador);
    }

    @Test
    void envioParcialSoloCompletaLoFaltante() throws IOException {
        when(registro.yaProcesado(eventoValido.id(), 103L)).thenReturn(true);
        when(registro.yaProcesado(eventoValido.id(), 101L)).thenReturn(false);
        when(enviador.enviar(anyLong(), any())).thenReturn(ResultadoEnvio.ENVIADO);

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(enviador, times(1)).enviar(eq(900000101L), any());
        verify(canal).basicAck(TAG, false);
    }

    @Test
    void temporalReencolaYNoSigue() throws IOException {
        when(enviador.enviar(eq(900000103L), any())).thenReturn(ResultadoEnvio.TEMPORAL);

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(canal).basicReject(TAG, true);
        verify(enviador, times(1)).enviar(anyLong(), any());
        verify(registro, never()).registrar(any(), anyLong());
    }

    @Test
    void bloqueadoAvisaAlNucleoYSigue() throws IOException {
        when(enviador.enviar(eq(900000103L), any())).thenReturn(ResultadoEnvio.BLOQUEADO);
        when(enviador.enviar(eq(900000101L), any())).thenReturn(ResultadoEnvio.ENVIADO);

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(clienteNucleo).informarBloqueo(900000103L);
        verify(registro).registrar(eventoValido.id(), 103L);
        verify(registro).registrar(eventoValido.id(), 101L);
        verify(canal).basicAck(TAG, false);
    }

    @Test
    void chatInvalidoSeOmiteYSigue() throws IOException {
        when(enviador.enviar(eq(900000103L), any())).thenReturn(ResultadoEnvio.CHAT_INVALIDO);
        when(enviador.enviar(eq(900000101L), any())).thenReturn(ResultadoEnvio.ENVIADO);

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(registro).registrar(eventoValido.id(), 103L);
        verify(registro).registrar(eventoValido.id(), 101L);
        verifyNoInteractions(clienteNucleo);
        verify(canal).basicAck(TAG, false);
    }

    @Test
    void mensajeInvalidoVaAFallidos() throws IOException {
        when(enviador.enviar(eq(900000103L), any())).thenReturn(ResultadoEnvio.MENSAJE_INVALIDO);

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(canal).basicReject(TAG, false);
    }

    @Test
    void plantillaRotaVaAFallidos() throws IOException {
        when(renderizador.renderizar(any(), any())).thenThrow(new PlantillaException("rota"));

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(canal).basicReject(TAG, false);
        verifyNoInteractions(enviador);
    }

    @Test
    void baseCaidaReencola() throws IOException {
        when(registro.yaProcesado(any(), anyLong())).thenThrow(new RuntimeException("BD caída"));

        consumidor.recibir(mensajeValido, canal, TAG);

        verify(canal).basicReject(TAG, true);
    }

    @Test
    void canalCerradoNoPropagaExcepcion() throws IOException {
        when(enviador.enviar(anyLong(), any())).thenReturn(ResultadoEnvio.ENVIADO);
        doThrow(new IOException("canal cerrado")).when(canal).basicAck(TAG, false);

        assertDoesNotThrow(() -> consumidor.recibir(mensajeValido, canal, TAG));
    }
}
