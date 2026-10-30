package co.uniproject.agente.vinculacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ClienteNucleoRestTest {

    private RestClient.Builder builder;
    private MockRestServiceServer servidor;
    private ClienteNucleoRest cliente;
    private static final String C = "A".repeat(32);

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new ClienteNucleoRest(builder, "http://nucleo.test", "token-de-prueba");
    }

    private void esperarVinculacion(org.springframework.test.web.client.ResponseCreator responseCreator) {
        servidor.expect(requestTo("http://nucleo.test/interno/telegram/vinculaciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-UniProject-Token-Interno", "token-de-prueba"))
                .andExpect(content().json("{\"codigo\":\"" + C + "\",\"chatId\":123}"))
                .andRespond(responseCreator);
    }

    @Test
    void vinculado204() {
        esperarVinculacion(withStatus(HttpStatus.NO_CONTENT));

        ResultadoVinculacion resultado = cliente.vincular(C, 123L);

        assertThat(resultado).isEqualTo(ResultadoVinculacion.VINCULADO);
        servidor.verify();
    }

    @Test
    void inexistente404() {
        esperarVinculacion(withStatus(HttpStatus.NOT_FOUND));

        ResultadoVinculacion resultado = cliente.vincular(C, 123L);

        assertThat(resultado).isEqualTo(ResultadoVinculacion.CODIGO_INEXISTENTE);
        servidor.verify();
    }

    @Test
    void vencido410() {
        esperarVinculacion(withStatus(HttpStatus.GONE));

        ResultadoVinculacion resultado = cliente.vincular(C, 123L);

        assertThat(resultado).isEqualTo(ResultadoVinculacion.CODIGO_VENCIDO_O_USADO);
        servidor.verify();
    }

    @Test
    void otrosEstadosYRedSonError() {
        RestClient.Builder b1 = RestClient.builder();
        MockRestServiceServer s1 = MockRestServiceServer.bindTo(b1).build();
        ClienteNucleoRest c1 = new ClienteNucleoRest(b1, "http://nucleo.test", "token-de-prueba");
        s1.expect(requestTo("http://nucleo.test/interno/telegram/vinculaciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-UniProject-Token-Interno", "token-de-prueba"))
                .andExpect(content().json("{\"codigo\":\"" + C + "\",\"chatId\":123}"))
                .andRespond(withServerError());
        assertThat(c1.vincular(C, 123L)).isEqualTo(ResultadoVinculacion.ERROR);
        s1.verify();

        RestClient.Builder b2 = RestClient.builder();
        MockRestServiceServer s2 = MockRestServiceServer.bindTo(b2).build();
        ClienteNucleoRest c2 = new ClienteNucleoRest(b2, "http://nucleo.test", "token-de-prueba");
        s2.expect(requestTo("http://nucleo.test/interno/telegram/vinculaciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-UniProject-Token-Interno", "token-de-prueba"))
                .andExpect(content().json("{\"codigo\":\"" + C + "\",\"chatId\":123}"))
                .andRespond(withUnauthorizedRequest());
        assertThat(c2.vincular(C, 123L)).isEqualTo(ResultadoVinculacion.ERROR);
        s2.verify();

        RestClient.Builder b3 = RestClient.builder();
        MockRestServiceServer s3 = MockRestServiceServer.bindTo(b3).build();
        ClienteNucleoRest c3 = new ClienteNucleoRest(b3, "http://nucleo.test", "token-de-prueba");
        s3.expect(requestTo("http://nucleo.test/interno/telegram/vinculaciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-UniProject-Token-Interno", "token-de-prueba"))
                .andExpect(content().json("{\"codigo\":\"" + C + "\",\"chatId\":123}"))
                .andRespond(withException(new IOException("sin red")));
        assertThat(c3.vincular(C, 123L)).isEqualTo(ResultadoVinculacion.ERROR);
        s3.verify();
    }

    @Test
    void bloqueoEnviaChat() {
        servidor.expect(requestTo("http://nucleo.test/interno/telegram/bloqueos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-UniProject-Token-Interno", "token-de-prueba"))
                .andExpect(content().json("{\"chatId\":123}"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatCode(() -> cliente.informarBloqueo(123L)).doesNotThrowAnyException();
        servidor.verify();
    }

    @Test
    void bloqueoConFalloNoLanza() {
        servidor.expect(requestTo("http://nucleo.test/interno/telegram/bloqueos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-UniProject-Token-Interno", "token-de-prueba"))
                .andExpect(content().json("{\"chatId\":123}"))
                .andRespond(withServerError());

        assertThatCode(() -> cliente.informarBloqueo(123L)).doesNotThrowAnyException();
        servidor.verify();
    }
}
