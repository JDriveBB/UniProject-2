package co.uniproject.nucleo.configuracionacademica;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.seguridad.cuentas.ServicioSupresion;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Listado de participantes de los cursos del proyecto disponibles (sin equipo en el proyecto). */
class EstudiantesDisponiblesIntegracionTest extends ApiIntegracionBase {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Navegador docente() {
        String email = correoUnico("docente");
        crearUsuario("Docente Prueba", email, "DOCENTE");
        return sesion(email);
    }

    private long crearProyecto(Navegador docente) {
        HttpResponse<String> r = docente.post(
                "/api/proyectos",
                "{\"nombre\":\"Proyecto de prueba\","
                        + "\"fechaInicio\":\"2026-08-03\",\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idProyecto");
    }

    private long crearEquipo(Navegador docente, long idProyecto, String nombre) {
        HttpResponse<String> r =
                docente.post("/api/proyectos/" + idProyecto + "/equipos", "{\"nombreEquipo\":\"" + nombre + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idEquipo");
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
    void listaSoloParticipantesDelProyectoSinEquipo() {
        Navegador docente = docente();
        Navegador otraDocente = docente();

        long idProyecto = crearProyecto(docente);
        long idEquipo = crearEquipo(docente, idProyecto, "Equipo A");
        long cursoAsignado = crearCurso(docente, "Curso Asignado");
        long cursoSinAsignar = crearCurso(docente, "Curso Sin Asignar");
        long cursoAjeno = crearCurso(otraDocente, "Curso Ajeno");

        String libreZ = correoUnico("librez");
        crearUsuario("Zulma Libre", libreZ, "ESTUDIANTE");
        String libreA = correoUnico("librea");
        crearUsuario("Alicia Libre", libreA, "ESTUDIANTE");
        String conEquipo = correoUnico("conequipo");
        crearUsuario("Carlos Con Equipo", conEquipo, "ESTUDIANTE");
        String otroCursoMismoDoc = correoUnico("otrocurso");
        crearUsuario("Beto Otro Curso", otroCursoMismoDoc, "ESTUDIANTE");
        String otroDocenteEst = correoUnico("otrodoc");
        crearUsuario("Diana Otro Docente", otroDocenteEst, "ESTUDIANTE");
        String suprimido = correoUnico("suprimido");
        long idSuprimido = crearUsuario("Suprimido Prueba", suprimido, "ESTUDIANTE");

        agregarAlCurso(docente, cursoAsignado, libreZ, libreA, conEquipo, suprimido);
        agregarAlCurso(docente, cursoSinAsignar, otroCursoMismoDoc);
        agregarAlCurso(otraDocente, cursoAjeno, otroDocenteEst);
        asignarCursos(docente, idProyecto, cursoAsignado);
        agregarMiembro(docente, idEquipo, conEquipo);

        HttpResponse<String> supresion =
                sesion(suprimido).post("/api/perfil/supresion", "{\"contrasena\":\"" + CLAVE + "\"}");
        assertThat(supresion.statusCode()).as(supresion.body()).isEqualTo(204);

        HttpResponse<String> respuesta = docente.get("/api/proyectos/" + idProyecto + "/estudiantes-disponibles");
        assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(200);

        Map<String, String> disponibles = leerDisponibles(respuesta.body());
        assertThat(disponibles)
                .containsEntry(libreA, "Alicia Libre")
                .containsEntry(libreZ, "Zulma Libre")
                .doesNotContainKeys(
                        conEquipo,
                        otroCursoMismoDoc,
                        otroDocenteEst,
                        suprimido,
                        ServicioSupresion.correoAnonimo(idSuprimido));
        List<String> orden = new ArrayList<>(disponibles.keySet());
        assertThat(orden).containsExactly(libreA, libreZ);

        agregarMiembro(docente, idEquipo, libreZ);
        HttpResponse<String> trasAgregar = docente.get("/api/proyectos/" + idProyecto + "/estudiantes-disponibles");
        assertThat(trasAgregar.statusCode()).as(trasAgregar.body()).isEqualTo(200);
        assertThat(leerDisponibles(trasAgregar.body())).containsOnlyKeys(libreA);
    }

    @Test
    void soloLaDocenteDuenaDelProyectoPuedeListarDisponibles() {
        Navegador docente = docente();
        long idProyecto = crearProyecto(docente);
        long idEquipo = crearEquipo(docente, idProyecto, "Equipo A");
        long curso = crearCurso(docente, "Curso Permisos");
        String estudianteCorreo = correoUnico("est");
        crearUsuario("Estudiante Prueba", estudianteCorreo, "ESTUDIANTE");
        agregarAlCurso(docente, curso, estudianteCorreo);
        asignarCursos(docente, idProyecto, curso);
        agregarMiembro(docente, idEquipo, estudianteCorreo);

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
