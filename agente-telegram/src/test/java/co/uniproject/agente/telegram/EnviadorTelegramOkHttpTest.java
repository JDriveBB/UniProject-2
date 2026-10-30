package co.uniproject.agente.telegram;

import co.uniproject.agente.plantillas.Aviso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnviadorTelegramOkHttpTest {

    private TelegramClient cliente;
    private EnviadorTelegramOkHttp enviador;
    private final Aviso aviso = new Aviso("<b>Tarea en riesgo</b>\nTexto", "Ver tarea", "/tareas/45");

    @BeforeEach
    void setUp() {
        cliente = mock(TelegramClient.class);
        enviador = new EnviadorTelegramOkHttp(cliente, "123:abc", "http://localhost:8080/", false);
    }

    @Test
    void enviaSinBotonConEnlaceEnTexto() throws Exception {
        when(cliente.execute(any(SendMessage.class))).thenReturn(null);

        ResultadoEnvio resultado = enviador.enviar(900000101L, aviso);

        assertThat(resultado).isEqualTo(ResultadoEnvio.ENVIADO);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(cliente).execute(captor.capture());

        SendMessage mensaje = captor.getValue();
        assertThat(mensaje.getChatId()).isEqualTo("900000101");
        assertThat(mensaje.getParseMode()).isEqualTo(ParseMode.HTML);
        assertThat(mensaje.getText()).isEqualTo("<b>Tarea en riesgo</b>\nTexto\n\nVer tarea: http://localhost:8080/tareas/45");
        assertThat(mensaje.getReplyMarkup()).isNull();
    }

    @Test
    void enviaConBotonCuandoEstaActivado() throws Exception {
        EnviadorTelegramOkHttp conBoton = new EnviadorTelegramOkHttp(cliente, "123:abc", "https://uniproject.example/", true);
        when(cliente.execute(any(SendMessage.class))).thenReturn(null);

        ResultadoEnvio resultado = conBoton.enviar(900000101L, aviso);

        assertThat(resultado).isEqualTo(ResultadoEnvio.ENVIADO);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(cliente).execute(captor.capture());

        SendMessage mensaje = captor.getValue();
        assertThat(mensaje.getText()).isEqualTo(aviso.html());
        assertThat(mensaje.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);

        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) mensaje.getReplyMarkup();
        assertThat(markup.getKeyboard()).hasSize(1);
        InlineKeyboardRow fila = markup.getKeyboard().get(0);
        assertThat(fila).hasSize(1);
        InlineKeyboardButton boton = fila.get(0);
        assertThat(boton.getText()).isEqualTo("Ver tarea");
        assertThat(boton.getUrl()).isEqualTo("https://uniproject.example/tareas/45");
    }

    @Test
    void demasiadasSolicitudesEsTemporal() throws Exception {
        TelegramApiRequestException ex = mock(TelegramApiRequestException.class);
        when(ex.getErrorCode()).thenReturn(429);
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex);

        ResultadoEnvio resultado = enviador.enviar(123L, aviso);

        assertThat(resultado).isEqualTo(ResultadoEnvio.TEMPORAL);
    }

    @Test
    void errorDelServidorEsTemporal() throws Exception {
        TelegramApiRequestException ex = mock(TelegramApiRequestException.class);
        when(ex.getErrorCode()).thenReturn(502);
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex);

        ResultadoEnvio resultado = enviador.enviar(123L, aviso);

        assertThat(resultado).isEqualTo(ResultadoEnvio.TEMPORAL);
    }

    @Test
    void bloqueoEs403() throws Exception {
        TelegramApiRequestException ex = mock(TelegramApiRequestException.class);
        when(ex.getErrorCode()).thenReturn(403);
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex);

        ResultadoEnvio resultado = enviador.enviar(123L, aviso);

        assertThat(resultado).isEqualTo(ResultadoEnvio.BLOQUEADO);
    }

    @Test
    void chatInexistenteEsChatInvalido() throws Exception {
        TelegramApiRequestException ex1 = mock(TelegramApiRequestException.class);
        when(ex1.getErrorCode()).thenReturn(400);
        when(ex1.getApiResponse()).thenReturn("Bad Request: chat not found");
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex1);

        ResultadoEnvio resultado1 = enviador.enviar(123L, aviso);
        assertThat(resultado1).isEqualTo(ResultadoEnvio.CHAT_INVALIDO);

        TelegramApiRequestException ex2 = mock(TelegramApiRequestException.class);
        when(ex2.getErrorCode()).thenReturn(400);
        when(ex2.getApiResponse()).thenReturn("Bad Request: PEER_ID_INVALID");
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex2);

        ResultadoEnvio resultado2 = enviador.enviar(123L, aviso);
        assertThat(resultado2).isEqualTo(ResultadoEnvio.CHAT_INVALIDO);
    }

    @Test
    void htmlOBotonMalFormadoEsMensajeInvalido() throws Exception {
        TelegramApiRequestException ex1 = mock(TelegramApiRequestException.class);
        when(ex1.getErrorCode()).thenReturn(400);
        when(ex1.getApiResponse()).thenReturn("Bad Request: can't parse entities: Unsupported start tag");
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex1);

        ResultadoEnvio resultado1 = enviador.enviar(123L, aviso);
        assertThat(resultado1).isEqualTo(ResultadoEnvio.MENSAJE_INVALIDO);

        TelegramApiRequestException ex2 = mock(TelegramApiRequestException.class);
        when(ex2.getErrorCode()).thenReturn(400);
        when(ex2.getApiResponse()).thenReturn("Bad Request: BUTTON_URL_INVALID");
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex2);

        ResultadoEnvio resultado2 = enviador.enviar(123L, aviso);
        assertThat(resultado2).isEqualTo(ResultadoEnvio.MENSAJE_INVALIDO);
    }

    @Test
    void botonRechazadoPorTelegramEsMensajeInvalido() throws Exception {
        TelegramApiRequestException ex = mock(TelegramApiRequestException.class);
        when(ex.getErrorCode()).thenReturn(400);
        when(ex.getApiResponse()).thenReturn("Bad Request: inline keyboard button URL 'http://localhost:8080/hitos/7/evaluar' is invalid: Wrong HTTP URL");
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex);

        ResultadoEnvio resultado = enviador.enviar(123L, aviso);

        assertThat(resultado).isEqualTo(ResultadoEnvio.MENSAJE_INVALIDO);
    }

    @Test
    void otro400NoReconocidoVaAFallidos() throws Exception {
        TelegramApiRequestException ex1 = mock(TelegramApiRequestException.class);
        when(ex1.getErrorCode()).thenReturn(400);
        when(ex1.getApiResponse()).thenReturn("Bad Request: message is too long");
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex1);

        ResultadoEnvio resultado1 = enviador.enviar(123L, aviso);
        assertThat(resultado1).isEqualTo(ResultadoEnvio.MENSAJE_INVALIDO);

        TelegramApiRequestException ex2 = mock(TelegramApiRequestException.class);
        when(ex2.getErrorCode()).thenReturn(400);
        when(ex2.getApiResponse()).thenReturn(null);
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex2);

        ResultadoEnvio resultado2 = enviador.enviar(123L, aviso);
        assertThat(resultado2).isEqualTo(ResultadoEnvio.MENSAJE_INVALIDO);
    }

    @Test
    void tokenRechazadoYRedSonTemporales() throws Exception {
        TelegramApiRequestException ex1 = mock(TelegramApiRequestException.class);
        when(ex1.getErrorCode()).thenReturn(401);
        when(cliente.execute(any(SendMessage.class))).thenThrow(ex1);

        ResultadoEnvio resultado1 = enviador.enviar(123L, aviso);
        assertThat(resultado1).isEqualTo(ResultadoEnvio.TEMPORAL);

        when(cliente.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("timeout"));

        ResultadoEnvio resultado2 = enviador.enviar(123L, aviso);
        assertThat(resultado2).isEqualTo(ResultadoEnvio.TEMPORAL);
    }

    @Test
    void sinTokenNoLlamaATelegram() {
        EnviadorTelegramOkHttp sinToken = new EnviadorTelegramOkHttp(cliente, "", "http://localhost:8080", false);

        ResultadoEnvio resultado = sinToken.enviar(123L, aviso);

        assertThat(resultado).isEqualTo(ResultadoEnvio.TEMPORAL);
        verifyNoInteractions(cliente);
    }
}
