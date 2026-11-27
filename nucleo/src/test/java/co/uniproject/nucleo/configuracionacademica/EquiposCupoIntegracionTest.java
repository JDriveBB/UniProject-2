package co.uniproject.nucleo.configuracionacademica;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Integración de equipos por curso, cupo, creación en lote, aviso de cupo y destitución de líder. */
class EquiposCupoIntegracionTest extends ApiIntegracionBase {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private Permisos permisos;

    private Navegador docente() {
        String email = correoUnico("doccupo");
        crearUsuario("Docente Cupo", email, "DOCENTE");
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
    void elCursoDelEquipoSeResuelveSegunElProyecto() {
        Navegador docente = docente();
        long c1 = crearCurso(docente, "Curso 1");
        long c2 = crearCurso(docente, "Curso 2");
        long cOtro = crearCurso(docente, "Curso No Asignado");

        // Proyecto no mixto sin cursos -> 409 PROYECTO_SIN_CURSOS.
        long pSin = crearProyecto(docente, "Sin Cursos", false);
        HttpResponse<String> sinCursos =
                docente.post("/api/proyectos/" + pSin + "/equipos", "{\"nombreEquipo\":\"Eq 0\"}");
        assertThat(sinCursos.statusCode()).as(sinCursos.body()).isEqualTo(409);
        assertThat(sinCursos.body()).contains("\"codigo\":\"PROYECTO_SIN_CURSOS\"");

        // Proyecto no mixto con 1 curso -> sin idCurso toma ese curso; cupo null o 3; cupo 0 -> 400 VALIDACION.
        long pUno = crearProyecto(docente, "Un Curso", false);
        asignarCursos(docente, pUno, c1);
        HttpResponse<String> eqNull =
                docente.post("/api/proyectos/" + pUno + "/equipos", "{\"nombreEquipo\":\"Eq Manual\"}");
        assertThat(eqNull.statusCode()).as(eqNull.body()).isEqualTo(201);
        assertThat(tieneValor(eqNull.body(), "idCurso", c1)).isTrue();
        assertThat(eqNull.body()).contains("\"cupo\":null");

        HttpResponse<String> eqCupo =
                docente.post("/api/proyectos/" + pUno + "/equipos", "{\"nombreEquipo\":\"Eq Con Cupo\",\"cupo\":3}");
        assertThat(eqCupo.statusCode()).as(eqCupo.body()).isEqualTo(201);
        assertThat(tieneValor(eqCupo.body(), "idCurso", c1)).isTrue();
        assertThat(eqCupo.body()).contains("\"cupo\":3");

        HttpResponse<String> cupoCero =
                docente.post("/api/proyectos/" + pUno + "/equipos", "{\"nombreEquipo\":\"Eq Cero\",\"cupo\":0}");
        assertThat(cupoCero.statusCode()).as(cupoCero.body()).isEqualTo(400);
        assertThat(cupoCero.body()).contains("\"codigo\":\"VALIDACION\"");

        // Proyecto no mixto con 2 cursos -> sin idCurso 400 CURSO_REQUERIDO; curso no asignado 409 CURSO_NO_ASIGNADO.
        long pDos = crearProyecto(docente, "Dos Cursos", false);
        asignarCursos(docente, pDos, c1, c2);
        HttpResponse<String> sinCurso =
                docente.post("/api/proyectos/" + pDos + "/equipos", "{\"nombreEquipo\":\"Eq Ambiguo\"}");
        assertThat(sinCurso.statusCode()).as(sinCurso.body()).isEqualTo(400);
        assertThat(sinCurso.body()).contains("\"codigo\":\"CURSO_REQUERIDO\"");

        HttpResponse<String> noAsignado = docente.post(
                "/api/proyectos/" + pDos + "/equipos", "{\"nombreEquipo\":\"Eq Ajeno\",\"idCurso\":" + cOtro + "}");
        assertThat(noAsignado.statusCode()).as(noAsignado.body()).isEqualTo(409);
        assertThat(noAsignado.body()).contains("\"codigo\":\"CURSO_NO_ASIGNADO\"");

        // Proyecto mixto -> con idCurso 400 CURSO_EN_PROYECTO_MIXTO; sin idCurso 201 con idCurso null.
        long pMixto = crearProyecto(docente, "Mixto", true);
        asignarCursos(docente, pMixto, c1, c2);
        HttpResponse<String> mixtoConCurso = docente.post(
                "/api/proyectos/" + pMixto + "/equipos", "{\"nombreEquipo\":\"Eq Mixto 1\",\"idCurso\":" + c1 + "}");
        assertThat(mixtoConCurso.statusCode()).as(mixtoConCurso.body()).isEqualTo(400);
        assertThat(mixtoConCurso.body()).contains("\"codigo\":\"CURSO_EN_PROYECTO_MIXTO\"");

        HttpResponse<String> mixtoOk =
                docente.post("/api/proyectos/" + pMixto + "/equipos", "{\"nombreEquipo\":\"Eq Mixto 2\"}");
        assertThat(mixtoOk.statusCode()).as(mixtoOk.body()).isEqualTo(201);
        assertThat(mixtoOk.body()).contains("\"idCurso\":null");
    }

    @Test
    void crearEquiposEnLote() {
        Navegador docente = docente();
        long c = crearCurso(docente, "Curso Lote");
        long p = crearProyecto(docente, "Proyecto Lote", false);
        asignarCursos(docente, p, c);
        HttpResponse<String> hito = docente.post(
                "/api/proyectos/" + p + "/hitos",
                "{\"nombreHito\":\"Hito 1\",\"orden\":1,\"fechaInicio\":\"2026-08-03\",\"fechaLimite\":\"2026-09-15\"}");
        assertThat(hito.statusCode()).as(hito.body()).isEqualTo(201);

        HttpResponse<String> lote = docente.post(
                "/api/proyectos/" + p + "/equipos/lote", "{\"prefijo\":\"Grupo\",\"cantidad\":3,\"cupo\":4}");
        assertThat(lote.statusCode()).as(lote.body()).isEqualTo(201);
        JsonNode lista = JSON.readTree(lote.body());
        assertThat(lista).hasSize(3);
        assertThat(lista.get(0).get("nombreEquipo").asString()).isEqualTo("Grupo 1");
        assertThat(lista.get(1).get("nombreEquipo").asString()).isEqualTo("Grupo 2");
        assertThat(lista.get(2).get("nombreEquipo").asString()).isEqualTo("Grupo 3");
        assertThat(lista.get(0).get("cupo").asInt()).isEqualTo(4);
        assertThat(jdbc.sql("SELECT count(*) FROM hito_equipo he JOIN hito h ON h.id_hito = he.id_hito "
                                + "WHERE h.id_proyecto = :p")
                        .param("p", p)
                        .query(Integer.class)
                        .single())
                .isEqualTo(3);

        // Repetir -> 409 EQUIPO_REPETIDO sin equipos nuevos.
        HttpResponse<String> repetido = docente.post(
                "/api/proyectos/" + p + "/equipos/lote", "{\"prefijo\":\"grupo\",\"cantidad\":2,\"cupo\":4}");
        assertThat(repetido.statusCode()).as(repetido.body()).isEqualTo(409);
        assertThat(repetido.body()).contains("\"codigo\":\"EQUIPO_REPETIDO\"");
        assertThat(jdbc.sql("SELECT count(*) FROM equipo WHERE id_proyecto = :p")
                        .param("p", p)
                        .query(Integer.class)
                        .single())
                .isEqualTo(3);

        // Sin participantes sin equipo y sin cantidad -> 409 SIN_ESTUDIANTES_SIN_EQUIPO.
        HttpResponse<String> sinEst =
                docente.post("/api/proyectos/" + p + "/equipos/lote", "{\"prefijo\":\"Auto\",\"cupo\":3}");
        assertThat(sinEst.statusCode()).as(sinEst.body()).isEqualTo(409);
        assertThat(sinEst.body()).contains("\"codigo\":\"SIN_ESTUDIANTES_SIN_EQUIPO\"");

        // Sin cantidad, 7 participantes sin equipo y cupo 3 -> 3 equipos.
        String[] correos = new String[7];
        for (int i = 0; i < 7; i++) {
            correos[i] = correoUnico("lote");
            crearUsuario("Est Lote " + i, correos[i], "ESTUDIANTE");
        }
        agregarAlCurso(docente, c, correos);
        HttpResponse<String> auto =
                docente.post("/api/proyectos/" + p + "/equipos/lote", "{\"prefijo\":\"Auto\",\"cupo\":3}");
        assertThat(auto.statusCode()).as(auto.body()).isEqualTo(201);
        assertThat(JSON.readTree(auto.body())).hasSize(3);

        // Un estudiante -> 403.
        Navegador est = sesion(correos[0]);
        assertThat(est.post(
                                "/api/proyectos/" + p + "/equipos/lote",
                                "{\"prefijo\":\"Pirata\",\"cantidad\":1,\"cupo\":2}")
                        .statusCode())
                .isEqualTo(403);
    }

    @Test
    void elDocenteCambiaElCupo() {
        Navegador docente = docente();
        Navegador otro = docente();
        long c = crearCurso(docente, "Curso Cupo");
        long p = crearProyecto(docente, "Proyecto Cupo", false);
        asignarCursos(docente, p, c);
        long e = crearEquipoApi(docente, p, "Eq Cupo", c, 5);
        String e1 = correoUnico("ec");
        String e2 = correoUnico("ec");
        String e3 = correoUnico("ec");
        crearUsuario("Est 1", e1, "ESTUDIANTE");
        crearUsuario("Est 2", e2, "ESTUDIANTE");
        crearUsuario("Est 3", e3, "ESTUDIANTE");
        agregarAlCurso(docente, c, e1, e2, e3);
        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + e1 + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + e2 + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + e3 + "\"}")
                        .statusCode())
                .isEqualTo(201);

        // Cupo menor que los integrantes actuales (3 -> 2): 200 y nadie sale.
        HttpResponse<String> r1 = docente.put("/api/equipos/" + e + "/cupo", "{\"cupo\":2}");
        assertThat(r1.statusCode()).as(r1.body()).isEqualTo(200);
        assertThat(r1.body()).contains("\"cupo\":2");
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e")
                        .param("e", e)
                        .query(Integer.class)
                        .single())
                .isEqualTo(3);

        // Cupo null -> equipo manual.
        HttpResponse<String> r2 = docente.put("/api/equipos/" + e + "/cupo", "{\"cupo\":null}");
        assertThat(r2.statusCode()).as(r2.body()).isEqualTo(200);
        assertThat(r2.body()).contains("\"cupo\":null");

        // Otro docente -> 403.
        assertThat(otro.put("/api/equipos/" + e + "/cupo", "{\"cupo\":4}").statusCode())
                .isEqualTo(403);

        // Auditoría equipo.cupo.
        assertThat(jdbc.sql("SELECT coalesce(valor_anterior,'-') || '>' || coalesce(valor_nuevo,'-') "
                                + "FROM log_auditoria WHERE entidad_afectada = 'equipo' AND id_registro_afectado = :e "
                                + "AND campo_modificado = 'cupo' ORDER BY id_log")
                        .param("e", e)
                        .query(String.class)
                        .list())
                .containsExactly("5>2", "2>-");
    }

    @Test
    void elDocentePasaElCupoConAviso() {
        Navegador docente = docente();
        long cA = crearCurso(docente, "Curso A");
        long cB = crearCurso(docente, "Curso B");
        long p = crearProyecto(docente, "Proyecto Aviso", false);
        asignarCursos(docente, p, cA, cB);
        long e = crearEquipoApi(docente, p, "Eq A", cA, 1);

        String a1 = correoUnico("a1");
        String a2 = correoUnico("a2");
        String b1 = correoUnico("b1");
        crearUsuario("Ana 1", a1, "ESTUDIANTE");
        crearUsuario("Ana 2", a2, "ESTUDIANTE");
        crearUsuario("Beto 1", b1, "ESTUDIANTE");
        agregarAlCurso(docente, cA, a1, a2);
        agregarAlCurso(docente, cB, b1);

        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + a1 + "\"}")
                        .statusCode())
                .isEqualTo(201);

        HttpResponse<String> lleno = docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + a2 + "\"}");
        assertThat(lleno.statusCode()).as(lleno.body()).isEqualTo(409);
        assertThat(lleno.body()).contains("\"codigo\":\"CUPO_LLENO\"");

        HttpResponse<String> confirmado =
                docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + a2 + "\",\"pasarCupo\":true}");
        assertThat(confirmado.statusCode()).as(confirmado.body()).isEqualTo(201);
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e")
                        .param("e", e)
                        .query(Integer.class)
                        .single())
                .isEqualTo(2);

        HttpResponse<String> otroCurso =
                docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + b1 + "\",\"pasarCupo\":true}");
        assertThat(otroCurso.statusCode()).as(otroCurso.body()).isEqualTo(409);
        assertThat(otroCurso.body()).contains("\"codigo\":\"CURSO_DEL_EQUIPO\"");
    }

    @Test
    void destituirYNombrar() {
        Navegador docente = docente();
        Navegador otro = docente();
        long c = crearCurso(docente, "Curso Lider");
        long p = crearProyecto(docente, "Proyecto Lider", false);
        asignarCursos(docente, p, c);
        assertThat(docente.post(
                                "/api/proyectos/" + p + "/hitos",
                                "{\"nombreHito\":\"Hito 1\",\"orden\":1,\"fechaInicio\":\"2026-08-03\",\"fechaLimite\":\"2026-09-15\"}")
                        .statusCode())
                .isEqualTo(201);
        long e = crearEquipoApi(docente, p, "Eq Lider", c, null);
        long hitoEquipo = jdbc.sql("SELECT id_hito_equipo FROM hito_equipo WHERE id_equipo = :e")
                .param("e", e)
                .query(Long.class)
                .single();

        String emailA = correoUnico("la");
        String emailB = correoUnico("lb");
        long idA = crearUsuario("Lider A", emailA, "ESTUDIANTE");
        long idB = crearUsuario("Lider B", emailB, "ESTUDIANTE");
        agregarAlCurso(docente, c, emailA, emailB);
        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + emailA + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + emailB + "\"}")
                        .statusCode())
                .isEqualTo(201);

        assertThat(docente.put("/api/equipos/" + e + "/lider", "{\"idUsuario\":" + idA + "}")
                        .statusCode())
                .isEqualTo(200);
        assertThat(docente.put("/api/equipos/" + e + "/lider", "{\"idUsuario\":" + idB + "}")
                        .statusCode())
                .isEqualTo(200);
        assertThat(jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", e)
                        .query(Long.class)
                        .list())
                .containsExactly(idB);
        assertThat(permisos.puedeGestionarHitoEquipo(idB, hitoEquipo)).isTrue();

        HttpResponse<String> destituir = docente.delete("/api/equipos/" + e + "/lider");
        assertThat(destituir.statusCode()).as(destituir.body()).isEqualTo(200);
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", e)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(permisos.puedeGestionarHitoEquipo(idB, hitoEquipo)).isFalse();

        HttpResponse<String> sinLider = docente.delete("/api/equipos/" + e + "/lider");
        assertThat(sinLider.statusCode()).as(sinLider.body()).isEqualTo(409);
        assertThat(sinLider.body()).contains("\"codigo\":\"SIN_LIDER\"");

        assertThat(otro.delete("/api/equipos/" + e + "/lider").statusCode()).isEqualTo(403);

        // Nombrar a A y quitarlo del equipo audita lider a null.
        assertThat(docente.put("/api/equipos/" + e + "/lider", "{\"idUsuario\":" + idA + "}")
                        .statusCode())
                .isEqualTo(200);
        assertThat(docente.delete("/api/equipos/" + e + "/miembros/" + idA).statusCode())
                .isEqualTo(200);

        assertThat(jdbc.sql("SELECT coalesce(valor_anterior,'-') || '>' || coalesce(valor_nuevo,'-') "
                                + "FROM log_auditoria WHERE entidad_afectada = 'equipo' AND id_registro_afectado = :e "
                                + "AND campo_modificado = 'lider' ORDER BY id_log")
                        .param("e", e)
                        .query(String.class)
                        .list())
                .containsExactly("-" + ">" + idA, idA + ">" + idB, idB + ">-", "-" + ">" + idA, idA + ">-");
    }
}
