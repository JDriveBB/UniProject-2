package co.uniproject.agente.telegram;

import co.uniproject.agente.vinculacion.ClienteNucleo;
import co.uniproject.agente.vinculacion.ResultadoVinculacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReceptorTelegramTest {

    private TelegramClient cliente;
    private ClienteNucleo nucleo;
    private ReceptorTelegram receptor;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String C = "A".repeat(32);

    @BeforeEach
    void setUp() {
        cliente = mock(TelegramClient.class);
        nucleo = mock(ClienteNucleo.class);
        receptor = new ReceptorTelegram(cliente, nucleo);
    }

    private Update privateUpdate(String texto) throws Exception {
        String json = "{\"update_id\":1,\"message\":{\"message_id\":2,\"date\":1700000000,\"chat\":{\"id\":123,\"type\":\"private\"},\"from\":{\"id\":123,\"is_bot\":false,\"first_name\":\"A\"},\"text\":\"" + texto + "\"}}";
        return mapper.readValue(json, Update.class);
    }

    private void responderVerificado(String textoEsperado) throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(cliente).execute(captor.capture());
        assertThat(captor.getValue().getChatId()).isEqualTo("123");
        assertThat(captor.getValue().getText()).isEqualTo(textoEsperado);
    }

    @Test
    void startConCodigoVincula() throws Exception {
        when(nucleo.vincular(C, 123L)).thenReturn(ResultadoVinculacion.VINCULADO);

        receptor.consume(privateUpdate("/start " + C));

        responderVerificado(TextosBot.VINCULADO);
        verify(nucleo, times(1)).vincular(C, 123L);
    }

    @Test
    void codigoInexistenteOVencido() throws Exception {
        when(nucleo.vincular(C, 123L))
                .thenReturn(ResultadoVinculacion.CODIGO_INEXISTENTE)
                .thenReturn(ResultadoVinculacion.CODIGO_VENCIDO_O_USADO);

        receptor.consume(privateUpdate("/start " + C));

        ArgumentCaptor<SendMessage> captor1 = ArgumentCaptor.forClass(SendMessage.class);
        verify(cliente).execute(captor1.capture());
        assertThat(captor1.getValue().getChatId()).isEqualTo("123");
        assertThat(captor1.getValue().getText()).isEqualTo(TextosBot.CODIGO_INEXISTENTE);

        reset(cliente);

        receptor.consume(privateUpdate("/start " + C));

        ArgumentCaptor<SendMessage> captor2 = ArgumentCaptor.forClass(SendMessage.class);
        verify(cliente).execute(captor2.capture());
        assertThat(captor2.getValue().getChatId()).isEqualTo("123");
        assertThat(captor2.getValue().getText()).isEqualTo(TextosBot.CODIGO_VENCIDO);
    }

    @Test
    void errorDelNucleo() throws Exception {
        when(nucleo.vincular(C, 123L)).thenReturn(ResultadoVinculacion.ERROR);

        receptor.consume(privateUpdate("/start " + C));

        responderVerificado(TextosBot.ERROR);
    }

    @Test
    void startSinCodigo() throws Exception {
        receptor.consume(privateUpdate("/start"));

        responderVerificado(TextosBot.SIN_CODIGO);
        verify(nucleo, never()).vincular(any(), anyLong());
    }

    @Test
    void codigoMalFormadoNoLlamaAlNucleo() throws Exception {
        receptor.consume(privateUpdate("/start abc"));

        responderVerificado(TextosBot.CODIGO_INEXISTENTE);
        verify(nucleo, never()).vincular(any(), anyLong());
    }

    @Test
    void otroTextoSoloAvisos() throws Exception {
        receptor.consume(privateUpdate("hola"));

        responderVerificado(TextosBot.SOLO_AVISOS);
    }

    @Test
    void gruposYUpdatesSinMensajeSeIgnoran() throws Exception {
        String jsonGrupo = "{\"update_id\":1,\"message\":{\"message_id\":2,\"date\":1700000000,\"chat\":{\"id\":-100,\"type\":\"group\"},\"from\":{\"id\":123,\"is_bot\":false,\"first_name\":\"A\"},\"text\":\"/start " + C + "\"}}";
        Update updateGrupo = mapper.readValue(jsonGrupo, Update.class);

        String jsonSinMensaje = "{\"update_id\":9}";
        Update updateSinMensaje = mapper.readValue(jsonSinMensaje, Update.class);

        receptor.consume(updateGrupo);
        receptor.consume(updateSinMensaje);

        verifyNoInteractions(cliente, nucleo);
    }

    @Test
    void falloAlResponderNoPropaga() throws Exception {
        when(cliente.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("red"));

        assertThatCode(() -> receptor.consume(privateUpdate("hola"))).doesNotThrowAnyException();
    }
}
