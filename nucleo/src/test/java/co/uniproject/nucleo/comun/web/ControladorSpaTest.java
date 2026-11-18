package co.uniproject.nucleo.comun.web;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
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
        for (String ruta : List.of("/ingresar", "/registro", "/olvide-contrasena", "/restablecer-contrasena",
                "/mis-tareas", "/proyectos", "/proyectos/5/panel", "/proyectos/nuevo", "/tareas/12",
                "/equipos/3/tablero", "/hitos/7/evaluar", "/hitos/7/evaluacion", "/notificaciones", "/perfil",
                "/admin/docentes", "/panel", "/reportes", "/tablero", "/ruta-critica")) {
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

    /** Bloque 43: el navegador pide HTML; una ruta desconocida que no es del servidor devuelve el SPA CON 404. */
    @Test
    void rutaDesconocidaDelNavegadorDevuelveElSpaCon404() throws IOException, InterruptedException {
        HttpClient cliente = HttpClient.newHttpClient();
        for (String ruta : List.of("/no-es-ruta-del-spa", "/proyectos-x/abc")) {
            HttpResponse<String> r = cliente.send(pedir("GET", ruta), HttpResponse.BodyHandlers.ofString());
            assertThat(r.statusCode()).as(ruta).isEqualTo(404);
            assertThat(r.headers().firstValue("Content-Type").orElse("")).as(ruta).startsWith("text/html");
            assertThat(r.body()).as(ruta).contains(MARCA);
        }
        // Rutas del servidor, recursos con punto y metodos distintos de GET: nunca devuelven el SPA.
        for (String ruta : List.of("/api/no-existe", "/interno/no-existe", "/actuator/no-existe", "/assets/no-existe.js")) {
            HttpResponse<String> r = cliente.send(pedir("GET", ruta), HttpResponse.BodyHandlers.ofString());
            assertThat(r.body()).as(ruta).doesNotContain(MARCA);
        }
        HttpResponse<String> post = cliente.send(pedir("POST", "/no-existe"), HttpResponse.BodyHandlers.ofString());
        assertThat(post.body()).as("POST").doesNotContain(MARCA);
    }

    private HttpRequest pedir(String metodo, String ruta) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Accept", "text/html,application/xhtml+xml");
        return ("POST".equals(metodo) ? b.POST(HttpRequest.BodyPublishers.noBody()) : b.GET()).build();
    }
}
