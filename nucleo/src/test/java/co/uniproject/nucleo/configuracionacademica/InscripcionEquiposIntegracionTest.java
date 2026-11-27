package co.uniproject.nucleo.configuracionacademica;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Integración de autoinscripción a equipos, fecha de cierre, salida, visibilidad y sincronización de curso. */
class InscripcionEquiposIntegracionTest extends ApiIntegracionBase {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Navegador docente() {
        String email = correoUnico("docinsc");
        crearUsuario("Docente Inscripcion", email, "DOCENTE");
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
    void elDocenteFijaElCierreYLasHorasDeRonda() {
        Navegador docente = docente();
        long c = crearCurso(docente, "Curso Config");
        long p = crearProyecto(docente, "Proyecto Config", false);
        asignarCursos(docente, p, c);
        String estEmail = correoUnico("estcfg");
        crearUsuario("Est Config", estEmail, "ESTUDIANTE");
        agregarAlCurso(docente, c, estEmail);
        Navegador est = sesion(estEmail);

        Instant futuro = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        HttpResponse<String> ok = docente.put(
                "/api/proyectos/" + p + "/inscripcion", "{\"cierreEquipos\":\"" + futuro + "\",\"horasRonda\":24}");
        assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);
        assertThat(ok.body()).contains("\"abierta\":true").contains("\"horasRonda\":24");

        HttpResponse<String> detalle = docente.get("/api/proyectos/" + p);
        assertThat(detalle.statusCode()).isEqualTo(200);
        assertThat(detalle.body())
                .contains("\"cierreEquipos\":\"" + futuro + "\"")
                .contains("\"horasRonda\":24");

        // Fecha pasada -> 400 CIERRE_EN_EL_PASADO.
        Instant pasado = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        HttpResponse<String> rPasado = docente.put(
                "/api/proyectos/" + p + "/inscripcion", "{\"cierreEquipos\":\"" + pasado + "\",\"horasRonda\":24}");
        assertThat(rPasado.statusCode()).as(rPasado.body()).isEqualTo(400);
        assertThat(rPasado.body()).contains("\"codigo\":\"CIERRE_EN_EL_PASADO\"");

        // horasRonda 0 o 337 -> 400 VALIDACION.
        for (int horas : new int[] {0, 337}) {
            HttpResponse<String> inv = docente.put(
                    "/api/proyectos/" + p + "/inscripcion",
                    "{\"cierreEquipos\":\"" + futuro + "\",\"horasRonda\":" + horas + "}");
            assertThat(inv.statusCode()).as(inv.body()).isEqualTo(400);
            assertThat(inv.body()).contains("\"codigo\":\"VALIDACION\"");
        }

        // Estudiante -> 403.
        assertThat(est.put(
                                "/api/proyectos/" + p + "/inscripcion",
                                "{\"cierreEquipos\":\"" + futuro + "\",\"horasRonda\":24}")
                        .statusCode())
                .isEqualTo(403);

        // recordatorio_cierre_en puesto por SQL -> cambiar fecha lo deja en NULL.
        jdbc.sql("UPDATE proyecto SET recordatorio_cierre_en = now() WHERE id_proyecto = :p")
                .param("p", p)
                .update();
        Instant futuro2 = Instant.now().plus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        assertThat(docente.put(
                                "/api/proyectos/" + p + "/inscripcion",
                                "{\"cierreEquipos\":\"" + futuro2 + "\",\"horasRonda\":24}")
                        .statusCode())
                .isEqualTo(200);
        assertThat(jdbc.sql("SELECT count(*) FROM proyecto WHERE id_proyecto = :p AND recordatorio_cierre_en IS NULL")
                        .param("p", p)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        // rondas_programadas_en puesto por SQL -> cambiar fecha 409 INSCRIPCION_YA_CERRADA; solo horas -> 200.
        jdbc.sql("UPDATE proyecto SET rondas_programadas_en = now() WHERE id_proyecto = :p")
                .param("p", p)
                .update();
        Instant futuro3 = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        HttpResponse<String> yaCerrada = docente.put(
                "/api/proyectos/" + p + "/inscripcion", "{\"cierreEquipos\":\"" + futuro3 + "\",\"horasRonda\":24}");
        assertThat(yaCerrada.statusCode()).as(yaCerrada.body()).isEqualTo(409);
        assertThat(yaCerrada.body()).contains("\"codigo\":\"INSCRIPCION_YA_CERRADA\"");

        HttpResponse<String> soloHoras = docente.put(
                "/api/proyectos/" + p + "/inscripcion", "{\"cierreEquipos\":\"" + futuro2 + "\",\"horasRonda\":36}");
        assertThat(soloHoras.statusCode()).as(soloHoras.body()).isEqualTo(200);
        assertThat(soloHoras.body()).contains("\"horasRonda\":36");

        // PUT /api/proyectos/{id} posterior no borra cierre_equipos ni horas_ronda.
        HttpResponse<String> act = docente.put(
                "/api/proyectos/" + p,
                "{\"nombre\":\"Proyecto Config Editado\",\"fechaInicio\":\"2026-08-03\","
                        + "\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false}");
        assertThat(act.statusCode()).as(act.body()).isEqualTo(200);
        assertThat(act.body()).contains("\"cierreEquipos\":\"" + futuro2 + "\"").contains("\"horasRonda\":36");
    }

    @Test
    void elEstudianteSeUneYSeSale() {
        Navegador docente = docente();
        long cA = crearCurso(docente, "Curso A");
        long cB = crearCurso(docente, "Curso B");
        long p = crearProyecto(docente, "Proyecto Insc", false);
        asignarCursos(docente, p, cA, cB);
        abrirInscripcion(docente, p);

        long eqA1 = crearEquipoApi(docente, p, "Eq A1", cA, 2);
        long eqA2 = crearEquipoApi(docente, p, "Eq A2", cA, 1);
        long eqManual = crearEquipoApi(docente, p, "Eq Manual", cA, null);
        long eqB = crearEquipoApi(docente, p, "Eq B", cB, 2);

        String email1 = correoUnico("est1");
        String email2 = correoUnico("est2");
        String ajenoEmail = correoUnico("ajeno");
        long id1 = crearUsuario("Estudiante Uno", email1, "ESTUDIANTE");
        crearUsuario("Estudiante Dos", email2, "ESTUDIANTE");
        crearUsuario("Ajeno", ajenoEmail, "ESTUDIANTE");
        agregarAlCurso(docente, cA, email1, email2);
        Navegador est1 = sesion(email1);
        Navegador est2 = sesion(email2);
        Navegador ajeno = sesion(ajenoEmail);

        // Unirse -> 200 y GET /inscripcion muestra idMiEquipo, integrantes 1 y su nombre.
        HttpResponse<String> unirse = est1.post("/api/equipos/" + eqA1 + "/inscripcion", null);
        assertThat(unirse.statusCode()).as(unirse.body()).isEqualTo(200);
        HttpResponse<String> vista = est1.get("/api/proyectos/" + p + "/inscripcion");
        assertThat(vista.statusCode()).isEqualTo(200);
        assertThat(tieneValor(vista.body(), "idMiEquipo", eqA1)).isTrue();
        assertThat(vista.body()).contains("\"integrantes\":1").contains("\"Estudiante Uno\"");

        // Unirse a otro -> 409 YA_TIENE_EQUIPO.
        HttpResponse<String> yaTiene = est1.post("/api/equipos/" + eqA2 + "/inscripcion", null);
        assertThat(yaTiene.statusCode()).as(yaTiene.body()).isEqualTo(409);
        assertThat(yaTiene.body()).contains("\"codigo\":\"YA_TIENE_EQUIPO\"");

        // Salirse siendo LIDER deja el equipo sin LIDER y 0 filas suyas.
        assertThat(docente.put("/api/equipos/" + eqA1 + "/lider", "{\"idUsuario\":" + id1 + "}")
                        .statusCode())
                .isEqualTo(200);
        HttpResponse<String> salir = est1.delete("/api/equipos/" + eqA1 + "/inscripcion");
        assertThat(salir.statusCode()).as(salir.body()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e AND id_usuario = :u")
                        .param("e", eqA1)
                        .param("u", id1)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", eqA1)
                        .query(Integer.class)
                        .single())
                .isZero();

        // Equipo manual -> 409 EQUIPO_SIN_INSCRIPCION.
        HttpResponse<String> manual = est1.post("/api/equipos/" + eqManual + "/inscripcion", null);
        assertThat(manual.statusCode()).as(manual.body()).isEqualTo(409);
        assertThat(manual.body()).contains("\"codigo\":\"EQUIPO_SIN_INSCRIPCION\"");

        // Equipo lleno -> 409 EQUIPO_LLENO.
        assertThat(est2.post("/api/equipos/" + eqA2 + "/inscripcion", null).statusCode())
                .isEqualTo(200);
        HttpResponse<String> lleno = est1.post("/api/equipos/" + eqA2 + "/inscripcion", null);
        assertThat(lleno.statusCode()).as(lleno.body()).isEqualTo(409);
        assertThat(lleno.body()).contains("\"codigo\":\"EQUIPO_LLENO\"");

        // Equipo de otro curso en proyecto no mixto -> 409 CURSO_DEL_EQUIPO.
        HttpResponse<String> otroCurso = est1.post("/api/equipos/" + eqB + "/inscripcion", null);
        assertThat(otroCurso.statusCode()).as(otroCurso.body()).isEqualTo(409);
        assertThat(otroCurso.body()).contains("\"codigo\":\"CURSO_DEL_EQUIPO\"");

        // Estudiante que no participa -> 403.
        assertThat(ajeno.post("/api/equipos/" + eqA1 + "/inscripcion", null).statusCode())
                .isEqualTo(403);

        // Salirse de un equipo donde no está -> 404.
        assertThat(est1.delete("/api/equipos/" + eqA1 + "/inscripcion").statusCode())
                .isEqualTo(404);

        // En un proyecto mixto se une a cualquier equipo con cupo.
        long pMixto = crearProyecto(docente, "Proyecto Mixto Insc", true);
        asignarCursos(docente, pMixto, cA, cB);
        abrirInscripcion(docente, pMixto);
        long eqM = crearEquipoApi(docente, pMixto, "Eq Mixto", null, 2);
        assertThat(est1.post("/api/equipos/" + eqM + "/inscripcion", null).statusCode())
                .isEqualTo(200);
    }

    @Test
    void laInscripcionSoloEstaAbiertaHastaElCierre() {
        Navegador docente = docente();
        long c = crearCurso(docente, "Curso Cierre");
        long p = crearProyecto(docente, "Proyecto Cierre", false);
        asignarCursos(docente, p, c);
        long eq = crearEquipoApi(docente, p, "Eq Cierre", c, 3);

        String e1 = correoUnico("ec1");
        String e2 = correoUnico("ec2");
        long id1 = crearUsuario("Est C1", e1, "ESTUDIANTE");
        crearUsuario("Est C2", e2, "ESTUDIANTE");
        agregarAlCurso(docente, c, e1, e2);
        Navegador est1 = sesion(e1);
        Navegador est2 = sesion(e2);

        // Sin fecha -> 409 INSCRIPCION_CERRADA.
        HttpResponse<String> sinFecha = est1.post("/api/equipos/" + eq + "/inscripcion", null);
        assertThat(sinFecha.statusCode()).as(sinFecha.body()).isEqualTo(409);
        assertThat(sinFecha.body()).contains("\"codigo\":\"INSCRIPCION_CERRADA\"");

        abrirInscripcion(docente, p);
        assertThat(est1.post("/api/equipos/" + eq + "/inscripcion", null).statusCode())
                .isEqualTo(200);

        cerrarInscripcionSql(p);
        HttpResponse<String> unirseCerrada = est2.post("/api/equipos/" + eq + "/inscripcion", null);
        assertThat(unirseCerrada.statusCode()).as(unirseCerrada.body()).isEqualTo(409);
        assertThat(unirseCerrada.body()).contains("\"codigo\":\"INSCRIPCION_CERRADA\"");

        HttpResponse<String> salirCerrada = est1.delete("/api/equipos/" + eq + "/inscripcion");
        assertThat(salirCerrada.statusCode()).as(salirCerrada.body()).isEqualTo(409);
        assertThat(salirCerrada.body()).contains("\"codigo\":\"INSCRIPCION_CERRADA\"");

        // El docente sí agrega (201) y quita (200) después del cierre.
        assertThat(docente.post("/api/equipos/" + eq + "/miembros", "{\"email\":\"" + e2 + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertThat(docente.delete("/api/equipos/" + eq + "/miembros/" + id1).statusCode())
                .isEqualTo(200);
    }

    @Test
    void noSeSaleConTareasAsignadas() {
        Navegador docente = docente();
        long c = crearCurso(docente, "Curso Tareas");
        long p = crearProyecto(docente, "Proyecto Tareas", false);
        asignarCursos(docente, p, c);
        assertThat(docente.post(
                                "/api/proyectos/" + p + "/hitos",
                                "{\"nombreHito\":\"Hito 1\",\"orden\":1,\"fechaInicio\":\"2026-08-03\",\"fechaLimite\":\"2026-09-15\"}")
                        .statusCode())
                .isEqualTo(201);
        abrirInscripcion(docente, p);
        long eq = crearEquipoApi(docente, p, "Eq Tareas", c, 3);
        long hitoEquipo = jdbc.sql("SELECT id_hito_equipo FROM hito_equipo WHERE id_equipo = :e")
                .param("e", eq)
                .query(Long.class)
                .single();

        String email = correoUnico("esttar");
        long idEst = crearUsuario("Est Tareas", email, "ESTUDIANTE");
        agregarAlCurso(docente, c, email);
        Navegador est = sesion(email);
        assertThat(est.post("/api/equipos/" + eq + "/inscripcion", null).statusCode())
                .isEqualTo(200);

        jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                        + "VALUES (:he, :u, 'Tarea bloqueante', 4)")
                .param("he", hitoEquipo)
                .param("u", idEst)
                .update();

        HttpResponse<String> salir = est.delete("/api/equipos/" + eq + "/inscripcion");
        assertThat(salir.statusCode()).as(salir.body()).isEqualTo(409);
        assertThat(salir.body()).contains("\"codigo\":\"TIENE_TAREAS\"");

        assertThat(docente.delete("/api/equipos/" + eq + "/miembros/" + idEst).statusCode())
                .isEqualTo(200);
    }

    @Test
    void elParticipanteSinEquipoVeElProyectoYLosEquiposSinCorreos() {
        Navegador docente = docente();
        long cA = crearCurso(docente, "Curso Vista A");
        long cB = crearCurso(docente, "Curso Vista B");
        long p = crearProyecto(docente, "Proyecto Vista", false);
        asignarCursos(docente, p, cA, cB);
        abrirInscripcion(docente, p);

        long eqALibre = crearEquipoApi(docente, p, "Eq A Libre", cA, 2);
        long eqALleno = crearEquipoApi(docente, p, "Eq A Lleno", cA, 1);
        crearEquipoApi(docente, p, "Eq B Otro", cB, 2);

        String eLibre = correoUnico("vlibre");
        String eLleno = correoUnico("vlleno");
        String eAjeno = correoUnico("vajeno");
        crearUsuario("Persona Libre", eLibre, "ESTUDIANTE");
        crearUsuario("Persona Lleno", eLleno, "ESTUDIANTE");
        crearUsuario("Persona Ajena", eAjeno, "ESTUDIANTE");
        agregarAlCurso(docente, cA, eLibre, eLleno);
        assertThat(docente.post("/api/equipos/" + eqALleno + "/miembros", "{\"email\":\"" + eLleno + "\"}")
                        .statusCode())
                .isEqualTo(201);

        Navegador est = sesion(eLibre);
        assertThat(tieneValor(est.get("/api/proyectos").body(), "idProyecto", p))
                .isTrue();
        assertThat(est.get("/api/proyectos/" + p).statusCode()).isEqualTo(200);

        HttpResponse<String> vistaEst = est.get("/api/proyectos/" + p + "/inscripcion");
        assertThat(vistaEst.statusCode()).as(vistaEst.body()).isEqualTo(200);
        assertThat(vistaEst.body()).doesNotContain("@");
        JsonNode root = JSON.readTree(vistaEst.body());
        assertThat(root.get("idCursoQueCuenta").asLong()).isEqualTo(cA);
        assertThat(root.get("equipos")).hasSize(2);
        JsonNode libreNode = root.get("equipos").get(0);
        JsonNode llenoNode = root.get("equipos").get(1);
        assertThat(libreNode.get("idEquipo").asLong()).isEqualTo(eqALibre);
        assertThat(libreNode.get("cupo").asInt()).isEqualTo(2);
        assertThat(libreNode.get("integrantes").asInt()).isEqualTo(0);
        assertThat(libreNode.get("puedeUnirse").asBoolean()).isTrue();
        assertThat(llenoNode.get("idEquipo").asLong()).isEqualTo(eqALleno);
        assertThat(llenoNode.get("integrantes").asInt()).isEqualTo(1);
        assertThat(llenoNode.get("nombres").get(0).asString()).isEqualTo("Persona Lleno");
        assertThat(llenoNode.get("puedeUnirse").asBoolean()).isFalse();

        // El docente ve todos los equipos con idCursoQueCuenta null.
        HttpResponse<String> vistaDoc = docente.get("/api/proyectos/" + p + "/inscripcion");
        assertThat(vistaDoc.statusCode()).isEqualTo(200);
        JsonNode rootDoc = JSON.readTree(vistaDoc.body());
        assertThat(rootDoc.get("idCursoQueCuenta").isNull()).isTrue();
        assertThat(rootDoc.get("equipos")).hasSize(3);

        // Estudiante de otro proyecto -> 403.
        assertThat(sesion(eAjeno).get("/api/proyectos/" + p + "/inscripcion").statusCode())
                .isEqualTo(403);
    }

    @Test
    void quitarDelCursoNoCambiaElCursoDeQuienTieneEquipoDeEseCurso() {
        Navegador docente = docente();
        long cX = crearCurso(docente, "Curso X");
        long cY = crearCurso(docente, "Curso Y");
        long p = crearProyecto(docente, "Proyecto Dos Cursos", false);
        String email = correoUnico("xy");
        long idEst = crearUsuario("Est XY", email, "ESTUDIANTE");
        agregarAlCurso(docente, cX, email);
        agregarAlCurso(docente, cY, email);
        asignarCursos(docente, p, cX, cY);
        long eqX = crearEquipoApi(docente, p, "Eq X", cX, null);
        assertThat(docente.post("/api/equipos/" + eqX + "/miembros", "{\"email\":\"" + email + "\"}")
                        .statusCode())
                .isEqualTo(201);

        // Quitarlo de X -> sigue en el equipo y participante_proyecto.id_curso sigue siendo X.
        assertThat(docente.delete("/api/cursos/" + cX + "/estudiantes/" + idEst).statusCode())
                .isEqualTo(204);
        assertThat(jdbc.sql("SELECT id_curso FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", p)
                        .param("u", idEst)
                        .query(Long.class)
                        .single())
                .isEqualTo(cX);

        // Quitarlo del equipo -> pasa a Y.
        assertThat(docente.delete("/api/equipos/" + eqX + "/miembros/" + idEst).statusCode())
                .isEqualTo(200);
        assertThat(jdbc.sql("SELECT id_curso FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", p)
                        .param("u", idEst)
                        .query(Long.class)
                        .single())
                .isEqualTo(cY);
    }
}
