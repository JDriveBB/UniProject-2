package co.uniproject.nucleo.comun.web;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControladorSpaTest extends ApiIntegracionBase {

    /** Texto del index.html de prueba (src/test/resources/static). */
    private static final String MARCA = "uniproject-spa-prueba";

    @Test
    void lasRutasDelFrontendDevuelvenElIndice() {
        Navegador n = new Navegador(puerto);
        for (String ruta : List.of("/ingresar", "/registro", "/mis-tareas", "/proyectos", "/proyectos/5/panel",
                "/proyectos/nuevo", "/tareas/12", "/equipos/3/tablero", "/hitos/7/evaluar", "/hitos/7/evaluacion",
                "/notificaciones", "/perfil", "/admin/docentes", "/panel", "/reportes", "/tablero", "/ruta-critica")) {
            HttpResponse<String> r = n.get(ruta);
            assertThat(r.statusCode()).as(ruta).isEqualTo(200);
            assertThat(r.body()).as(ruta).contains(MARCA);
        }
    }

    @Test
    void laApiYLasRutasDesconocidasNoDevuelvenElSpa() {
        Navegador n = new Navegador(puerto);
        assertThat(n.get("/api/no-existe").statusCode()).isEqualTo(401);
        assertThat(n.get("/api/sesion/yo").statusCode()).isEqualTo(401);
        HttpResponse<String> desconocida = n.get("/no-es-ruta-del-spa");
        assertThat(desconocida.statusCode()).isEqualTo(404);
        assertThat(desconocida.body()).doesNotContain(MARCA);
    }
}
