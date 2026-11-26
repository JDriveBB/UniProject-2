package co.uniproject.nucleo.configuracionacademica.cursos;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/** Integración de la asignación de cursos a proyectos, propagación a participantes y reglas de equipos. */
class CursosProyectoIntegracionTest extends ApiIntegracionBase {

    private Navegador docente() {
        String email = correoUnico("docproy");
        crearUsuario("Docente Proyecto", email, "DOCENTE");
        return sesion(email);
    }

    private long crearProyecto(Navegador docente, String nombre, Boolean equiposMixtos) {
        String extra = equiposMixtos == null ? "" : ",\"equiposMixtos\":" + equiposMixtos;
        HttpResponse<String> r = docente.post(
                "/api/proyectos",
                "{\"nombre\":\"" + nombre + "\",\"fechaInicio\":\"2026-08-03\","
                        + "\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false" + extra + "}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idProyecto");
    }

    private long crearEquipo(Navegador docente, long idProyecto, String nombre) {
        HttpResponse<String> r =
                docente.post("/api/proyectos/" + idProyecto + "/equipos", "{\"nombreEquipo\":\"" + nombre + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idEquipo");
    }

    @Test
    void asignarDosCursosPideConfirmacionYNoCambiaNada() {
        Navegador docente = docente();
        long c1 = crearCurso(docente, "Curso 1");
        long c2 = crearCurso(docente, "Curso 2");
        long p = crearProyecto(docente, "Proyecto Doble", null);
        String rep = correoUnico("rep");
        long idRep = crearUsuario("Estudiante Repetido", rep, "ESTUDIANTE");
        agregarAlCurso(docente, c1, rep);
        agregarAlCurso(docente, c2, rep);

        HttpResponse<String> previa = docente.put(
                "/api/proyectos/" + p + "/cursos", "{\"idsCursos\":[" + c2 + "," + c1 + "],\"confirmar\":false}");
        assertThat(previa.statusCode()).as(previa.body()).isEqualTo(200);
        assertThat(previa.body())
                .contains("\"aplicado\":false")
                .contains("\"email\":\"" + rep + "\"")
                .contains("\"idCursoQueCuenta\":" + c1);
        assertThat(jdbc.sql("SELECT count(*) FROM curso_proyecto WHERE id_proyecto = :p")
                        .param("p", p)
                        .query(Integer.class)
                        .single())
                .isZero();

        HttpResponse<String> confirmado = docente.put(
                "/api/proyectos/" + p + "/cursos", "{\"idsCursos\":[" + c2 + "," + c1 + "],\"confirmar\":true}");
        assertThat(confirmado.statusCode()).as(confirmado.body()).isEqualTo(200);
        assertThat(confirmado.body()).contains("\"aplicado\":true");
        assertThat(jdbc.sql("SELECT id_curso FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", p)
                        .param("u", idRep)
                        .query(Long.class)
                        .list())
                .containsExactly(c1);

        // Asignar un solo curso no pide confirmación.
        long pUno = crearProyecto(docente, "Proyecto Uno", null);
        HttpResponse<String> uno =
                docente.put("/api/proyectos/" + pUno + "/cursos", "{\"idsCursos\":[" + c1 + "],\"confirmar\":false}");
        assertThat(uno.statusCode()).as(uno.body()).isEqualTo(200);
        assertThat(uno.body()).contains("\"aplicado\":true");
    }

    @Test
    void elDocenteEligeConQueCursoCuentaElEstudiante() {
        Navegador docente = docente();
        Navegador otro = docente();
        long c1 = crearCurso(docente, "Curso Uno");
        long c2 = crearCurso(docente, "Curso Dos");
        long c3 = crearCurso(docente, "Curso Tres");
        long p = crearProyecto(docente, "Proyecto Eleccion", null);
        String est = correoUnico("eleccion");
        long idEst = crearUsuario("Est Eleccion", est, "ESTUDIANTE");
        agregarAlCurso(docente, c1, est);
        agregarAlCurso(docente, c2, est);
        asignarCursos(docente, p, c1, c2, c3);

        HttpResponse<String> ok =
                docente.put("/api/proyectos/" + p + "/estudiantes/" + idEst + "/curso", "{\"idCurso\":" + c2 + "}");
        assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);
        assertThat(tieneValor(ok.body(), "idCurso", c2)).isTrue();

        HttpResponse<String> noValido =
                docente.put("/api/proyectos/" + p + "/estudiantes/" + idEst + "/curso", "{\"idCurso\":" + c3 + "}");
        assertThat(noValido.statusCode()).as(noValido.body()).isEqualTo(409);
        assertThat(noValido.body()).contains("\"codigo\":\"CURSO_NO_VALIDO\"");

        String ajeno = correoUnico("ajeno");
        long idAjeno = crearUsuario("Ajeno", ajeno, "ESTUDIANTE");
        assertThat(docente.put("/api/proyectos/" + p + "/estudiantes/" + idAjeno + "/curso", "{\"idCurso\":" + c1 + "}")
                        .statusCode())
                .isEqualTo(404);
        assertThat(otro.put("/api/proyectos/" + p + "/estudiantes/" + idEst + "/curso", "{\"idCurso\":" + c1 + "}")
                        .statusCode())
                .isEqualTo(403);
    }

    @Test
    void agregarAlCursoMeteAlEstudianteEnLosProyectosDelCurso() {
        Navegador docente = docente();
        long c = crearCurso(docente, "Curso Propagado");
        long p = crearProyecto(docente, "Proyecto Propagado", null);
        asignarCursos(docente, p, c);

        String email = correoUnico("tardio");
        crearUsuario("Est Tardio", email, "ESTUDIANTE");
        agregarAlCurso(docente, c, email);

        HttpResponse<String> estudiantes = docente.get("/api/proyectos/" + p + "/estudiantes");
        assertThat(estudiantes.statusCode()).as(estudiantes.body()).isEqualTo(200);
        assertThat(estudiantes.body())
                .contains("\"email\":\"" + email + "\"")
                .contains("\"nombreCurso\":\"Curso Propagado\"");

        HttpResponse<String> disponibles = docente.get("/api/proyectos/" + p + "/estudiantes-disponibles");
        assertThat(disponibles.statusCode()).as(disponibles.body()).isEqualTo(200);
        assertThat(disponibles.body()).contains("\"email\":\"" + email + "\"");
    }

    @Test
    void quitarDelCursoSacaDelProyectoSoloSiNoTieneEquipo() {
        Navegador docente = docente();
        long c1 = crearCurso(docente, "Curso A");
        long c2 = crearCurso(docente, "Curso B");
        long p = crearProyecto(docente, "Proyecto Salida", null);

        String sinEq = correoUnico("sineq");
        long idSinEq = crearUsuario("Sin Equipo", sinEq, "ESTUDIANTE");
        String conEq = correoUnico("coneq");
        long idConEq = crearUsuario("Con Equipo", conEq, "ESTUDIANTE");
        String enDos = correoUnico("endos");
        long idEnDos = crearUsuario("En Dos Cursos", enDos, "ESTUDIANTE");

        agregarAlCurso(docente, c1, sinEq, conEq, enDos);
        agregarAlCurso(docente, c2, enDos);
        asignarCursos(docente, p, c1, c2);
        long equipo = crearEquipoApi(docente, p, "Equipo 1", c1, null);
        assertThat(docente.post("/api/equipos/" + equipo + "/miembros", "{\"email\":\"" + conEq + "\"}")
                        .statusCode())
                .isEqualTo(201);

        assertThat(docente.delete("/api/cursos/" + c1 + "/estudiantes/" + idSinEq)
                        .statusCode())
                .isEqualTo(204);
        assertThat(jdbc.sql("SELECT count(*) FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", p)
                        .param("u", idSinEq)
                        .query(Integer.class)
                        .single())
                .isZero();

        assertThat(docente.delete("/api/cursos/" + c1 + "/estudiantes/" + idConEq)
                        .statusCode())
                .isEqualTo(204);
        assertThat(jdbc.sql("SELECT count(*) FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", p)
                        .param("u", idConEq)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(docente.delete("/api/equipos/" + equipo + "/miembros/" + idConEq)
                        .statusCode())
                .isEqualTo(200);
        assertThat(jdbc.sql("SELECT count(*) FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", p)
                        .param("u", idConEq)
                        .query(Integer.class)
                        .single())
                .isZero();

        assertThat(docente.delete("/api/cursos/" + c1 + "/estudiantes/" + idEnDos)
                        .statusCode())
                .isEqualTo(204);
        assertThat(jdbc.sql("SELECT id_curso FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", p)
                        .param("u", idEnDos)
                        .query(Long.class)
                        .single())
                .isEqualTo(c2);
    }

    @Test
    void quitarUnCursoDelProyecto() {
        Navegador docente = docente();
        long c = crearCurso(docente, "Curso Quitar");
        long p = crearProyecto(docente, "Proyecto Quitar", null);

        String libre = correoUnico("libre");
        long idLibre = crearUsuario("Libre", libre, "ESTUDIANTE");
        String conEquipo = correoUnico("coneq");
        long idConEquipo = crearUsuario("Con Equipo", conEquipo, "ESTUDIANTE");
        agregarAlCurso(docente, c, libre, conEquipo);
        asignarCursos(docente, p, c);
        long equipo = crearEquipo(docente, p, "Equipo Q");
        assertThat(docente.post("/api/equipos/" + equipo + "/miembros", "{\"email\":\"" + conEquipo + "\"}")
                        .statusCode())
                .isEqualTo(201);

        asignarCursos(docente, p);

        assertThat(jdbc.sql("SELECT count(*) FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", p)
                        .param("u", idLibre)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", p)
                        .param("u", idConEquipo)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void losEstudiantesYaNoSeHabilitanPorProyecto() {
        Navegador docente = docente();
        long p = crearProyecto(docente, "Proyecto Mixto", true);
        HttpResponse<String> detalle = docente.get("/api/proyectos/" + p);
        assertThat(detalle.body()).contains("\"equiposMixtos\":true");

        HttpResponse<String> habEst = docente.post(
                "/api/proyectos/" + p + "/correos",
                "{\"emails\":[\"" + correoUnico("est") + "\"],\"rol\":\"ESTUDIANTE\"}");
        assertThat(habEst.statusCode()).as(habEst.body()).isEqualTo(400);
        assertThat(habEst.body()).contains("\"codigo\":\"ROL_INVALIDO\"");

        long equipo = crearEquipo(docente, p, "Equipo M");
        String existente = correoUnico("estno");
        crearUsuario("Sin Curso", existente, "ESTUDIANTE");
        HttpResponse<String> sinCursoExistente =
                docente.post("/api/equipos/" + equipo + "/miembros", "{\"email\":\"" + existente + "\"}");
        HttpResponse<String> sinCursoInexistente =
                docente.post("/api/equipos/" + equipo + "/miembros", "{\"email\":\"" + correoUnico("nadie") + "\"}");
        assertThat(sinCursoExistente.statusCode()).isEqualTo(409);
        assertThat(sinCursoExistente.body()).contains("\"codigo\":\"ESTUDIANTE_SIN_CURSO\"");
        assertThat(sinCursoExistente.body()).isEqualTo(sinCursoInexistente.body());

        HttpResponse<String> cambioInmutable = docente.put(
                "/api/proyectos/" + p,
                "{\"nombre\":\"Proyecto Mixto\",\"fechaInicio\":\"2026-08-03\","
                        + "\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false,\"equiposMixtos\":false}");
        assertThat(cambioInmutable.statusCode()).as(cambioInmutable.body()).isEqualTo(409);
        assertThat(cambioInmutable.body()).contains("\"codigo\":\"EQUIPOS_MIXTOS_INMUTABLE\"");

        HttpResponse<String> sinCampo = docente.put(
                "/api/proyectos/" + p,
                "{\"nombre\":\"Proyecto Mixto 2\",\"fechaInicio\":\"2026-08-03\","
                        + "\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false}");
        assertThat(sinCampo.statusCode()).as(sinCampo.body()).isEqualTo(200);
        assertThat(sinCampo.body()).contains("\"equiposMixtos\":true");
    }

    @Test
    void noSeAsignaNiSeVeUnCursoAjeno() {
        Navegador docenteA = docente();
        Navegador docenteB = docente();
        long cursoDeA = crearCurso(docenteA, "Curso de A");
        long pA = crearProyecto(docenteA, "Proyecto de A", null);
        long pB = crearProyecto(docenteB, "Proyecto de B", null);

        HttpResponse<String> asignarConfirmado = docenteB.put(
                "/api/proyectos/" + pB + "/cursos", "{\"idsCursos\":[" + cursoDeA + "],\"confirmar\":true}");
        assertThat(asignarConfirmado.statusCode()).as(asignarConfirmado.body()).isEqualTo(403);
        assertThat(jdbc.sql("SELECT count(*) FROM curso_proyecto WHERE id_proyecto = :p")
                        .param("p", pB)
                        .query(Integer.class)
                        .single())
                .isZero();

        HttpResponse<String> asignarSinConfirmar = docenteB.put(
                "/api/proyectos/" + pB + "/cursos", "{\"idsCursos\":[" + cursoDeA + "],\"confirmar\":false}");
        assertThat(asignarSinConfirmar.statusCode())
                .as(asignarSinConfirmar.body())
                .isEqualTo(403);
        assertThat(jdbc.sql("SELECT count(*) FROM curso_proyecto WHERE id_proyecto = :p")
                        .param("p", pB)
                        .query(Integer.class)
                        .single())
                .isZero();

        assertThat(docenteB.get("/api/proyectos/" + pA + "/cursos").statusCode())
                .isEqualTo(403);
        assertThat(docenteB.put(
                                "/api/proyectos/" + pA + "/cursos",
                                "{\"idsCursos\":[" + cursoDeA + "],\"confirmar\":true}")
                        .statusCode())
                .isEqualTo(403);
    }
}
