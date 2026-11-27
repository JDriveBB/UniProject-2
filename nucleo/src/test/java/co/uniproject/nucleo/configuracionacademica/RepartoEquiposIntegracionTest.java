package co.uniproject.nucleo.configuracionacademica;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Integración del reparto automático de estudiantes sin equipo. */
class RepartoEquiposIntegracionTest extends ApiIntegracionBase {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Navegador docente() {
        String email = correoUnico("docrep");
        crearUsuario("Docente Reparto", email, "DOCENTE");
        return sesion(email);
    }

    private long crearProyecto(Navegador docente, String nombre, boolean equiposMixtos) {
        HttpResponse<String> r = docente.post(
                "/api/proyectos",
                "{\"nombre\":\"" + nombre + "\",\"fechaInicio\":\"2026-08-03\","
                        + "\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false,\"equiposMixtos\":" + equiposMixtos
                        + "}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idProyecto");
    }

    @Test
    void elRepartoRespetaCursoYCupo() {
        Navegador docente = docente();
        long cA = crearCurso(docente, "Curso Rep A");
        long cB = crearCurso(docente, "Curso Rep B");
        long p = crearProyecto(docente, "Proyecto Reparto", false);
        asignarCursos(docente, p, cA, cB);

        long eqA1 = crearEquipoApi(docente, p, "Eq A1", cA, 2);
        long eqA2 = crearEquipoApi(docente, p, "Eq A2", cA, 2);
        long eqAManual = crearEquipoApi(docente, p, "Eq A Manual", cA, null);
        long eqB1 = crearEquipoApi(docente, p, "Eq B1", cB, 1);

        String aYa = correoUnico("aya");
        crearUsuario("Ana Ya", aYa, "ESTUDIANTE");
        agregarAlCurso(docente, cA, aYa);
        assertThat(docente.post("/api/equipos/" + eqA1 + "/miembros", "{\"email\":\"" + aYa + "\"}")
                        .statusCode())
                .isEqualTo(201);

        String a1 = correoUnico("ra1");
        String a2 = correoUnico("ra2");
        String a3 = correoUnico("ra3");
        String a4 = correoUnico("ra4");
        String b1 = correoUnico("rb1");
        String b2 = correoUnico("rb2");
        crearUsuario("A1 Uno", a1, "ESTUDIANTE");
        crearUsuario("A2 Dos", a2, "ESTUDIANTE");
        crearUsuario("A3 Tres", a3, "ESTUDIANTE");
        crearUsuario("A4 Cuatro", a4, "ESTUDIANTE");
        crearUsuario("B1 Uno", b1, "ESTUDIANTE");
        crearUsuario("B2 Dos", b2, "ESTUDIANTE");
        agregarAlCurso(docente, cA, a1, a2, a3, a4);
        agregarAlCurso(docente, cB, b1, b2);

        HttpResponse<String> r = docente.post("/api/proyectos/" + p + "/equipos/reparto", null);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        JsonNode root = JSON.readTree(r.body());
        JsonNode asignados = root.get("asignados");
        JsonNode sinCupo = root.get("sinCupo");
        assertThat(asignados).hasSize(4);
        assertThat(sinCupo).hasSize(2);

        // El equipo vacío (eqA2) recibe primero (A1 Uno).
        assertThat(asignados.get(0).get("nombre").asString()).isEqualTo("A1 Uno");
        assertThat(asignados.get(0).get("idEquipo").asLong()).isEqualTo(eqA2);

        // Ningún equipo pasa su cupo y el manual no cambia.
        assertThat(contarMiembros(eqA1)).isEqualTo(2);
        assertThat(contarMiembros(eqA2)).isEqualTo(2);
        assertThat(contarMiembros(eqAManual)).isZero();
        assertThat(contarMiembros(eqB1)).isEqualTo(1);

        // Un sinCupo de cada curso.
        long sinA = 0;
        long sinB = 0;
        for (JsonNode sc : sinCupo) {
            if (sc.get("idCurso").asLong() == cA) {
                sinA++;
            } else if (sc.get("idCurso").asLong() == cB) {
                sinB++;
            }
        }
        assertThat(sinA).isEqualTo(1);
        assertThat(sinB).isEqualTo(1);

        // Auditoría miembro_equipo CREAR del docente.
        long idDocente = jdbc.sql("SELECT id_docente FROM proyecto WHERE id_proyecto = :p")
                .param("p", p)
                .query(Long.class)
                .single();
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'miembro_equipo' "
                                + "AND accion = 'CREAR' AND id_usuario_accion = :d "
                                + "AND id_registro_afectado IN (:e1, :e2, :eb)")
                        .param("d", idDocente)
                        .param("e1", eqA1)
                        .param("e2", eqA2)
                        .param("eb", eqB1)
                        .query(Integer.class)
                        .single())
                .isEqualTo(5);

        // Repetir -> asignados vacío.
        HttpResponse<String> repetir = docente.post("/api/proyectos/" + p + "/equipos/reparto", null);
        assertThat(repetir.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(repetir.body()).get("asignados")).isEmpty();
    }

    @Test
    void elRepartoEnProyectoMixtoMezclaCursos() {
        Navegador docente = docente();
        long cA = crearCurso(docente, "Mix A");
        long cB = crearCurso(docente, "Mix B");
        long p = crearProyecto(docente, "Proyecto Reparto Mixto", true);
        asignarCursos(docente, p, cA, cB);
        long eq = crearEquipoApi(docente, p, "Eq Mixto", null, 2);

        String a = correoUnico("ma");
        String b = correoUnico("mb");
        crearUsuario("Mix Uno", a, "ESTUDIANTE");
        crearUsuario("Mix Dos", b, "ESTUDIANTE");
        agregarAlCurso(docente, cA, a);
        agregarAlCurso(docente, cB, b);

        HttpResponse<String> r = docente.post("/api/proyectos/" + p + "/equipos/reparto", null);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        JsonNode root = JSON.readTree(r.body());
        assertThat(root.get("asignados")).hasSize(2);
        assertThat(root.get("sinCupo")).isEmpty();
        assertThat(contarMiembros(eq)).isEqualTo(2);
    }

    @Test
    void soloElDocenteDuenoReparte() {
        Navegador docente = docente();
        Navegador otro = docente();
        long c = crearCurso(docente, "Curso Perm Rep");
        long p = crearProyecto(docente, "Proyecto Perm Rep", false);
        asignarCursos(docente, p, c);
        String estEmail = correoUnico("estrep");
        crearUsuario("Est Rep", estEmail, "ESTUDIANTE");
        agregarAlCurso(docente, c, estEmail);
        Navegador est = sesion(estEmail);

        assertThat(otro.post("/api/proyectos/" + p + "/equipos/reparto", null).statusCode())
                .isEqualTo(403);
        assertThat(est.post("/api/proyectos/" + p + "/equipos/reparto", null).statusCode())
                .isEqualTo(403);
    }

    private int contarMiembros(long idEquipo) {
        return jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e")
                .param("e", idEquipo)
                .query(Integer.class)
                .single();
    }
}
