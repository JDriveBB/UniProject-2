package co.uniproject.nucleo.configuracionacademica;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.seguridad.cuentas.ServicioSupresion;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** T-046: listado de cuentas de estudiante disponibles para agregar a un equipo (sin habilitar ni en otro equipo). */
class EstudiantesDisponiblesIntegracionTest extends ApiIntegracionBase {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static String correos(String rol, String... emails) {
        return "{\"emails\":[" + Arrays.stream(emails).map(e -> "\"" + e + "\"").collect(Collectors.joining(","))
                + "],\"rol\":\"" + rol + "\"}";
    }

    private Navegador docente() {
        String email = correoUnico("docente");
        crearUsuario("Docente Prueba", email, "DOCENTE");
        return sesion(email);
    }

    private long crearProyecto(Navegador docente, boolean usaMonitores) {
        HttpResponse<String> r = docente.post(
                "/api/proyectos",
                "{\"nombre\":\"Proyecto de prueba\","
                        + "\"fechaInicio\":\"2026-08-03\",\"fechaFin\":\"2026-11-27\",\"usaMonitores\":" + usaMonitores
                        + "}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idProyecto");
    }

    private long crearEquipo(Navegador docente, long idProyecto, String nombre) {
        HttpResponse<String> r =
                docente.post("/api/proyectos/" + idProyecto + "/equipos", "{\"nombreEquipo\":\"" + nombre + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idEquipo");
    }

    private void habilitar(Navegador docente, long idProyecto, String rol, String... emails) {
        HttpResponse<String> r = docente.post("/api/proyectos/" + idProyecto + "/correos", correos(rol, emails));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
    }

    private void agregarMiembro(Navegador docente, long idEquipo, String email) {
        HttpResponse<String> r =
                docente.post("/api/equipos/" + idEquipo + "/miembros", "{\"email\":\"" + email + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
    }

    private static Map<String, String> leerDisponibles(String json) {
        JsonNode lista = JSON.readTree(json);
        assertThat(lista.isArray()).isTrue();
        Map<String, String> porCorreo = new LinkedHashMap<>();
        for (JsonNode elemento : lista) {
            assertThat(elemento.isObject()).isTrue();
            assertThat(elemento.size()).isEqualTo(2);
            assertThat(elemento.has("email")).isTrue();
            assertThat(elemento.has("nombre")).isTrue();
            String email = JSON.treeToValue(elemento.get("email"), String.class);
            String nombre = JSON.treeToValue(elemento.get("nombre"), String.class);
            porCorreo.put(email, nombre);
        }
        return porCorreo;
    }

    @Test
    void listaCuentasDeEstudianteSinEquipoNiHabilitacionYSoloEmailYNombre() {
        String emailDocente = correoUnico("docente");
        crearUsuario("Docente Duena", emailDocente, "DOCENTE");
        Navegador docente = sesion(emailDocente);

        long idProyecto = crearProyecto(docente, false);
        long idA = crearEquipo(docente, idProyecto, "Equipo A");
        long otroProyecto = crearProyecto(docente, false);
        long idOtro = crearEquipo(docente, otroProyecto, "Equipo Otro");

        String libre = correoUnico("libre");
        crearUsuario("Zulma Libre", libre, "ESTUDIANTE");
        String conEquipo = correoUnico("conequipo");
        crearUsuario("Carlos Con Equipo", conEquipo, "ESTUDIANTE");
        String habilitado = correoUnico("habilitado");
        crearUsuario("Beto Habilitado", habilitado, "ESTUDIANTE");
        String enOtro = correoUnico("enotro");
        crearUsuario("Alicia En Otro", enOtro, "ESTUDIANTE");
        String otraDocente = correoUnico("otradocente");
        crearUsuario("Otra Docente", otraDocente, "DOCENTE");
        String monitor = correoUnico("monitor");
        crearUsuario("Mario Monitor", monitor, "MONITOR");
        String suprimido = correoUnico("suprimido");
        long idSuprimido = crearUsuario("Suprimido Prueba", suprimido, "ESTUDIANTE");

        habilitar(docente, idProyecto, "ESTUDIANTE", conEquipo, habilitado);
        agregarMiembro(docente, idA, conEquipo);
        habilitar(docente, otroProyecto, "ESTUDIANTE", enOtro);
        agregarMiembro(docente, idOtro, enOtro);

        HttpResponse<String> supresion =
                sesion(suprimido).post("/api/perfil/supresion", "{\"contrasena\":\"" + CLAVE + "\"}");
        assertThat(supresion.statusCode()).as(supresion.body()).isEqualTo(204);

        HttpResponse<String> respuesta = docente.get("/api/proyectos/" + idProyecto + "/estudiantes-disponibles");
        assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(200);

        Map<String, String> disponibles = leerDisponibles(respuesta.body());
        assertThat(disponibles)
                .containsEntry(libre, "Zulma Libre")
                .containsEntry(enOtro, "Alicia En Otro")
                .doesNotContainKeys(
                        conEquipo,
                        habilitado,
                        emailDocente,
                        otraDocente,
                        monitor,
                        suprimido,
                        ServicioSupresion.correoAnonimo(idSuprimido));
        List<String> orden = new ArrayList<>(disponibles.keySet());
        assertThat(orden.indexOf(enOtro)).isLessThan(orden.indexOf(libre));

        // Como lo hace la web: habilitar con rol ESTUDIANTE y agregar al equipo; deja de aparecer en disponibles.
        habilitar(docente, idProyecto, "ESTUDIANTE", libre);
        agregarMiembro(docente, idA, libre);

        HttpResponse<String> trasAgregar = docente.get("/api/proyectos/" + idProyecto + "/estudiantes-disponibles");
        assertThat(trasAgregar.statusCode()).as(trasAgregar.body()).isEqualTo(200);
        assertThat(leerDisponibles(trasAgregar.body())).doesNotContainKey(libre);
    }

    @Test
    void soloLaDocenteDuenaDelProyectoPuedeListarDisponibles() {
        Navegador docente = docente();
        long idProyecto = crearProyecto(docente, false);
        long idA = crearEquipo(docente, idProyecto, "Equipo A");

        String estudianteCorreo = correoUnico("est");
        crearUsuario("Estudiante Prueba", estudianteCorreo, "ESTUDIANTE");
        habilitar(docente, idProyecto, "ESTUDIANTE", estudianteCorreo);
        agregarMiembro(docente, idA, estudianteCorreo);

        Navegador otraDocente = docente();
        Navegador estudiante = sesion(estudianteCorreo);
        for (Navegador n : new Navegador[] {otraDocente, estudiante}) {
            HttpResponse<String> r = n.get("/api/proyectos/" + idProyecto + "/estudiantes-disponibles");
            assertThat(r.statusCode()).as(r.body()).isEqualTo(403);
            assertThat(r.body()).contains("\"codigo\":\"ACCESO_DENEGADO\"");
        }

        HttpResponse<String> inexistente = docente.get("/api/proyectos/999999999/estudiantes-disponibles");
        assertThat(inexistente.statusCode()).as(inexistente.body()).isEqualTo(403);
        assertThat(inexistente.body()).contains("\"codigo\":\"ACCESO_DENEGADO\"");

        HttpResponse<String> sinSesion =
                new Navegador(puerto).get("/api/proyectos/" + idProyecto + "/estudiantes-disponibles");
        assertThat(sinSesion.statusCode()).isEqualTo(401);
    }
}
