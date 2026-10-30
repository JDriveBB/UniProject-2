package co.uniproject.nucleo.vinculaciontelegram;

import co.uniproject.eventos.interno.ContratoInterno;
import co.uniproject.nucleo.seguridad.FiltroTokenInterno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ControladorInternoTelegramTest {

    private ServicioVinculacion servicio;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        servicio = Mockito.mock(ServicioVinculacion.class);
        mvc = MockMvcBuilders.standaloneSetup(new ControladorInternoTelegram(servicio))
                .addFilters(new FiltroTokenInterno("token-de-prueba"))
                .build();
    }

    @Test
    void sinTokenEs401() throws Exception {
        mvc.perform(post(ContratoInterno.RUTA_VINCULACIONES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"codigo":"%s","chatId":123}
                    """.formatted("A".repeat(32))))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void tokenIncorrectoEs401() throws Exception {
        mvc.perform(post(ContratoInterno.RUTA_VINCULACIONES)
                .header(ContratoInterno.CABECERA_TOKEN, "otro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"codigo":"%s","chatId":123}
                    """.formatted("A".repeat(32))))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void tokenVacioEnConfiguracionSiempreEs401() throws Exception {
        MockMvc mvcTokenVacio = MockMvcBuilders.standaloneSetup(new ControladorInternoTelegram(servicio))
                .addFilters(new FiltroTokenInterno(""))
                .build();
        mvcTokenVacio.perform(post(ContratoInterno.RUTA_VINCULACIONES)
                .header(ContratoInterno.CABECERA_TOKEN, "")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"codigo":"%s","chatId":123}
                    """.formatted("A".repeat(32))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void vinculadoEs204() throws Exception {
        String codigo = "A".repeat(32);
        when(servicio.confirmar(codigo, 123L)).thenReturn(ResultadoConfirmacion.VINCULADO);

        mvc.perform(post(ContratoInterno.RUTA_VINCULACIONES)
                .header(ContratoInterno.CABECERA_TOKEN, "token-de-prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"codigo":"%s","chatId":123}
                    """.formatted(codigo)))
            .andExpect(status().isNoContent());

        verify(servicio, times(1)).confirmar(codigo, 123L);
    }

    @Test
    void inexistenteEs404YVencidoEs410() throws Exception {
        String codigo = "A".repeat(32);
        when(servicio.confirmar(codigo, 123L))
                .thenReturn(ResultadoConfirmacion.CODIGO_INEXISTENTE)
                .thenReturn(ResultadoConfirmacion.CODIGO_VENCIDO_O_USADO);

        mvc.perform(post(ContratoInterno.RUTA_VINCULACIONES)
                .header(ContratoInterno.CABECERA_TOKEN, "token-de-prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"codigo":"%s","chatId":123}
                    """.formatted(codigo)))
            .andExpect(status().isNotFound());

        mvc.perform(post(ContratoInterno.RUTA_VINCULACIONES)
                .header(ContratoInterno.CABECERA_TOKEN, "token-de-prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"codigo":"%s","chatId":123}
                    """.formatted(codigo)))
            .andExpect(status().isGone());
    }

    @Test
    void cuerpoInvalidoEs400() throws Exception {
        String codigoValido = "A".repeat(32);
        String[] cuerpos = {
            """
            {"codigo":"abc","chatId":123}
            """,
            """
            {"codigo":"%s","chatId":0}
            """.formatted(codigoValido),
            """
            {"codigo":"%s","chatId":-5}
            """.formatted(codigoValido),
            "{no es json"
        };

        for (String cuerpo : cuerpos) {
            mvc.perform(post(ContratoInterno.RUTA_VINCULACIONES)
                    .header(ContratoInterno.CABECERA_TOKEN, "token-de-prueba")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
                .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(servicio);
    }

    @Test
    void bloqueoEs204() throws Exception {
        when(servicio.desvincularPorChat(123L)).thenReturn(1);

        mvc.perform(post(ContratoInterno.RUTA_BLOQUEOS)
                .header(ContratoInterno.CABECERA_TOKEN, "token-de-prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"chatId":123}
                    """))
            .andExpect(status().isNoContent());

        verify(servicio, times(1)).desvincularPorChat(123L);
    }

    @Test
    void bloqueoInvalidoEs400() throws Exception {
        mvc.perform(post(ContratoInterno.RUTA_BLOQUEOS)
                .header(ContratoInterno.CABECERA_TOKEN, "token-de-prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"chatId":0}
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }
}
