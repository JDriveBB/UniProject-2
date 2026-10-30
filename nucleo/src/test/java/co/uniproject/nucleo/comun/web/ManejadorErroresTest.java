package co.uniproject.nucleo.comun.web;

import co.uniproject.nucleo.comun.error.AccesoDenegadoException;
import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManejadorErroresTest {

    @RestController
    static class ControladorPrueba {

        record Cuerpo(@NotBlank String titulo) {}

        @GetMapping("/prueba/prohibido")
        void prohibido() {
            throw new AccesoDenegadoException();
        }

        @GetMapping("/prueba/inexistente")
        void inexistente() {
            throw new RecursoNoEncontradoException("La tarea no existe");
        }

        @GetMapping("/prueba/regla")
        void regla() {
            throw new ReglaNegocioException("PRERREQUISITOS_INCOMPLETOS", "Hay prerrequisitos críticos sin completar",
                    List.of("Tarea A", "Tarea B"));
        }

        @GetMapping("/prueba/invalida")
        void invalida() {
            throw new SolicitudInvalidaException("ARCHIVO_GRANDE", "El archivo supera 10 MB");
        }

        @PostMapping("/prueba/validar")
        void validar(@Valid @RequestBody Cuerpo cuerpo) {
        }
    }

    private RegistroAuditoria auditoria;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        auditoria = mock(RegistroAuditoria.class);
        mvc = MockMvcBuilders.standaloneSetup(new ControladorPrueba())
                .setControllerAdvice(new ManejadorErrores(auditoria))
                .build();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accesoDenegadoResponde403YLoAudita() throws Exception {
        UsuarioAutenticado ana = new UsuarioAutenticado(7L, "Ana", "ana@u.edu.co", RolPlataforma.ESTUDIANTE);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(ana, null, List.of()));
        mvc.perform(get("/prueba/prohibido"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"))
                .andExpect(jsonPath("$.mensaje").value(startsWith("No tienes permiso")));
        verify(auditoria).registrar(new EntradaAuditoria(7L, "http", null, AccionAuditoria.ACCESO_DENEGADO, null, null,
                "GET /prueba/prohibido"));
    }

    @Test
    void accesoDenegadoSinSesionSeAuditaSinUsuario() throws Exception {
        mvc.perform(get("/prueba/prohibido")).andExpect(status().isForbidden());
        verify(auditoria).registrar(EntradaAuditoria.accesoDenegado(null, "GET", "/prueba/prohibido"));
    }

    @Test
    void noEncontradoResponde404() throws Exception {
        mvc.perform(get("/prueba/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("La tarea no existe"))
                .andExpect(jsonPath("$.detalles").isEmpty());
        verifyNoInteractions(auditoria);
    }

    @Test
    void reglaDeNegocioResponde409ConDetalles() throws Exception {
        mvc.perform(get("/prueba/regla"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRERREQUISITOS_INCOMPLETOS"))
                .andExpect(jsonPath("$.detalles[0]").value("Tarea A"))
                .andExpect(jsonPath("$.detalles[1]").value("Tarea B"));
    }

    @Test
    void solicitudInvalidaResponde400() throws Exception {
        mvc.perform(get("/prueba/invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_GRANDE"));
    }

    @Test
    void validacionYCuerpoIlegibleResponden400() throws Exception {
        mvc.perform(post("/prueba/validar").contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles[0]").value(startsWith("titulo: ")));
        mvc.perform(post("/prueba/validar").contentType(MediaType.APPLICATION_JSON).content("no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CUERPO_INVALIDO"));
    }
}
