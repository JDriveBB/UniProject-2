package co.uniproject.nucleo.configuracionacademica;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ConfiguracionAcademicaIntegracionTest extends ApiIntegracionBase {

    private static String proyecto(String nombre, boolean usaMonitores) {
        return "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Curso de prueba\",\"fechaInicio\":\"2026-08-03\","
                + "\"fechaFin\":\"2026-11-27\",\"usaMonitores\":" + usaMonitores + "}";
    }

    private static String hito(String nombre, int orden, String inicio, String limite) {
        return "{\"nombreHito\":\"" + nombre + "\",\"orden\":" + orden + ",\"fechaInicio\":\"" + inicio
                + "\",\"fechaLimite\":\"" + limite + "\"}";
    }

    private static String criterio(String descripcion, String peso, String maximo) {
        return "{\"descripcion\":\"" + descripcion + "\",\"pesoPorcentaje\":" + peso + ",\"puntajeMaximo\":" + maximo
                + "}";
    }

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
        HttpResponse<String> r = docente.post("/api/proyectos", proyecto("Proyecto de prueba", usaMonitores));
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

    /** Crea la cuenta de estudiante desde una habilitación pendiente de curso, con la contraseña CLAVE. */
    private void registrarEnCurso(Navegador docente, long idCurso, String email, String nombre) {
        String codigo = enlaceCurso(docente, idCurso, email);
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        HttpResponse<String> r = n.post(
                "/api/cuentas/registro",
                "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email
                        + "\",\"contrasena\":\"" + CLAVE + "\",\"autorizaTratamientoDatos\":true,\"codigo\":\"" + codigo
                        + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
    }

    /** Crea la cuenta de monitor desde una habilitación pendiente del proyecto, con la contraseña CLAVE. */
    private void registrarMonitor(Navegador docente, long idProyecto, String email, String nombre) {
        String codigo = enlace(docente, idProyecto, email);
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        HttpResponse<String> r = n.post(
                "/api/cuentas/registro",
                "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email
                        + "\",\"contrasena\":\"" + CLAVE + "\",\"autorizaTratamientoDatos\":true,\"codigo\":\"" + codigo
                        + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
    }

    @Test
    void elDocenteCreaYListaSusProyectos() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        assertThat(tieneValor(docente.get("/api/proyectos").body(), "idProyecto", id))
                .isTrue();
        HttpResponse<String> detalle = docente.get("/api/proyectos/" + id);
        assertThat(detalle.statusCode()).isEqualTo(200);
        assertThat(detalle.body())
                .contains("\"nombre\":\"Proyecto de prueba\"")
                .contains("\"fechaFin\":\"2026-11-27\"");

        String ajeno = correoUnico("est");
        crearUsuario("Estudiante Ajeno", ajeno, "ESTUDIANTE");
        Navegador estudiante = sesion(ajeno);
        assertThat(estudiante.get("/api/proyectos").body()).isEqualTo("[]");
        HttpResponse<String> prohibido = estudiante.get("/api/proyectos/" + id);
        assertThat(prohibido.statusCode()).isEqualTo(403);
        assertThat(prohibido.body()).contains("\"codigo\":\"ACCESO_DENEGADO\"");
        assertThat(estudiante.post("/api/proyectos", proyecto("Intento", false)).statusCode())
                .isEqualTo(403);
    }

    @Test
    void validaFechasYCampos() {
        Navegador docente = docente();
        HttpResponse<String> fechas = docente.post(
                "/api/proyectos",
                "{\"nombre\":\"X\",\"fechaInicio\":\"2026-11-27\","
                        + "\"fechaFin\":\"2026-08-03\",\"usaMonitores\":false}");
        assertThat(fechas.statusCode()).isEqualTo(400);
        assertThat(fechas.body()).contains("\"codigo\":\"FECHAS_INVALIDAS\"");
        HttpResponse<String> sinNombre = docente.post(
                "/api/proyectos",
                "{\"fechaInicio\":\"2026-08-03\",\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false}");
        assertThat(sinNombre.statusCode()).isEqualTo(400);
        assertThat(sinNombre.body()).contains("\"codigo\":\"VALIDACION\"").contains("nombre");
        long id = crearProyecto(docente, false);
        HttpResponse<String> fuera =
                docente.post("/api/proyectos/" + id + "/hitos", hito("H1", 1, "2026-07-01", "2026-08-20"));
        assertThat(fuera.statusCode()).isEqualTo(400);
        assertThat(fuera.body()).contains("\"codigo\":\"FECHAS_FUERA_DEL_PROYECTO\"");
    }

    @Test
    void hitosYEquiposCreanLosHitosDeCadaEquipo() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        crearEquipo(docente, id, "Equipo 1");
        HttpResponse<String> h1 =
                docente.post("/api/proyectos/" + id + "/hitos", hito("Diagramas", 1, "2026-08-03", "2026-09-15"));
        assertThat(h1.statusCode()).isEqualTo(201);
        assertThat(h1.body())
                .contains("\"diasInactividadAlerta\":null")
                .contains("\"notificarDocenteInactividad\":false");
        crearEquipo(docente, id, "Equipo 2");
        assertThat(docente.post("/api/proyectos/" + id + "/hitos", hito("Prototipo", 2, "2026-09-16", "2026-10-30"))
                        .statusCode())
                .isEqualTo(201);
        assertThat(jdbc.sql("SELECT count(*) FROM hito_equipo he JOIN hito h ON h.id_hito = he.id_hito "
                                + "WHERE h.id_proyecto = :p AND he.estado = 'EN_CURSO'")
                        .param("p", id)
                        .query(Integer.class)
                        .single())
                .isEqualTo(4);

        HttpResponse<String> ordenRepetido =
                docente.post("/api/proyectos/" + id + "/hitos", hito("Otro", 2, "2026-09-16", "2026-10-30"));
        assertThat(ordenRepetido.statusCode()).isEqualTo(409);
        assertThat(ordenRepetido.body()).contains("\"codigo\":\"ORDEN_REPETIDO\"");
        HttpResponse<String> equipoRepetido =
                docente.post("/api/proyectos/" + id + "/equipos", "{\"nombreEquipo\":\" equipo 1 \"}");
        assertThat(equipoRepetido.statusCode()).isEqualTo(409);
        assertThat(equipoRepetido.body()).contains("\"codigo\":\"EQUIPO_REPETIDO\"");

        long idHito = numero(h1.body(), "idHito");
        HttpResponse<String> cambio = docente.put(
                "/api/hitos/" + idHito,
                "{\"nombreHito\":\"Diagramas UML\",\"orden\":1,"
                        + "\"fechaInicio\":\"2026-08-03\",\"fechaLimite\":\"2026-09-20\",\"diasInactividadAlerta\":5,"
                        + "\"notificarDocenteInactividad\":true}");
        assertThat(cambio.statusCode()).isEqualTo(200);
        assertThat(cambio.body())
                .contains("\"diasInactividadAlerta\":5")
                .contains("\"notificarDocenteInactividad\":true");
        assertThat(docente.get("/api/proyectos/" + id).body())
                .contains("Diagramas UML")
                .contains("Equipo 2")
                .contains("\"estado\":\"EN_CURSO\"");
    }

    @Test
    void laRubricaNoSuperaCien() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        long idHito = numero(
                docente.post("/api/proyectos/" + id + "/hitos", hito("H", 1, "2026-08-03", "2026-09-15"))
                        .body(),
                "idHito");
        String base = "/api/hitos/" + idHito + "/criterios";
        HttpResponse<String> c1 = docente.post(base, criterio("Diagramas", "60", "5"));
        assertThat(c1.statusCode()).isEqualTo(201);
        assertThat(docente.post(base, criterio("Exposicion", "40", "5")).statusCode())
                .isEqualTo(201);
        HttpResponse<String> exceso = docente.post(base, criterio("Extra", "0.5", "5"));
        assertThat(exceso.statusCode()).isEqualTo(409);
        assertThat(exceso.body()).contains("\"codigo\":\"PESOS_EXCEDEN_100\"").contains("100.5");
        long idCriterio = numero(c1.body(), "idCriterio");
        assertThat(docente.put(base + "/" + idCriterio, criterio("Diagramas UML", "50", "5"))
                        .statusCode())
                .isEqualTo(200);
        assertThat(docente.post(base, criterio("Extra", "10", "5")).statusCode())
                .isEqualTo(201);
        assertThat(docente.delete(base + "/" + idCriterio).statusCode()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT sum(peso_porcentaje) FROM criterio_rubrica WHERE id_hito = :h")
                        .param("h", idHito)
                        .query(BigDecimal.class)
                        .single())
                .isEqualByComparingTo("50");

        Navegador otro = docente();
        assertThat(otro.post(base, criterio("Ajeno", "1", "5")).statusCode()).isEqualTo(403);
        assertThat(otro.delete("/api/hitos/999999/criterios/1").statusCode()).isEqualTo(403);
    }

    @Test
    void laRubricaUsadaNoSeCambia() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        crearEquipo(docente, id, "Equipo R");
        long idHito = numero(
                docente.post("/api/proyectos/" + id + "/hitos", hito("H", 1, "2026-08-03", "2026-09-15"))
                        .body(),
                "idHito");
        String base = "/api/hitos/" + idHito + "/criterios";
        long idCriterio =
                numero(docente.post(base, criterio("Unico", "100", "5")).body(), "idCriterio");
        long idDocente = jdbc.sql("SELECT id_docente FROM proyecto WHERE id_proyecto = :p")
                .param("p", id)
                .query(Long.class)
                .single();
        jdbc.sql("WITH he AS (SELECT id_hito_equipo FROM hito_equipo WHERE id_hito = :h), "
                        + "env AS (INSERT INTO envio_hito (id_hito_equipo, id_usuario_envia) "
                        + "SELECT id_hito_equipo, :d FROM he RETURNING id_envio), "
                        + "ev AS (INSERT INTO evaluacion_hito (id_envio, id_docente, nota_grupal) "
                        + "SELECT id_envio, :d, 4.00 FROM env RETURNING id_evaluacion) "
                        + "INSERT INTO calificacion_criterio (id_evaluacion, id_criterio, nota) "
                        + "SELECT id_evaluacion, :c, 4.00 FROM ev")
                .param("h", idHito)
                .param("d", idDocente)
                .param("c", idCriterio)
                .update();

        HttpResponse<String> cambio = docente.put(base + "/" + idCriterio, criterio("Unico", "90", "5"));
        assertThat(cambio.statusCode()).isEqualTo(409);
        assertThat(cambio.body()).contains("\"codigo\":\"RUBRICA_EN_USO\"");
        assertThat(docente.delete(base + "/" + idCriterio).statusCode()).isEqualTo(409);
        assertThat(docente.post(base, criterio("Nuevo", "1", "5")).statusCode()).isEqualTo(409);
    }

    @Test
    void correosHabilitadosYMiembros() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        long idCurso = crearCurso(docente, "Curso Miembros");
        asignarCursos(docente, id, idCurso);
        long idEquipo1 = crearEquipo(docente, id, "Equipo A");
        long idEquipo2 = crearEquipo(docente, id, "Equipo B");
        String a = correoUnico("est");
        String b = correoUnico("est");
        HttpResponse<String> estPorProyecto =
                docente.post("/api/proyectos/" + id + "/correos", correos("ESTUDIANTE", a));
        assertThat(estPorProyecto.statusCode()).as(estPorProyecto.body()).isEqualTo(400);
        assertThat(estPorProyecto.body()).contains("\"codigo\":\"ROL_INVALIDO\"");

        HttpResponse<String> alta = docente.post(
                "/api/cursos/" + idCurso + "/correos",
                "{\"emails\":[\"" + a.toUpperCase(Locale.ROOT) + "\",\"" + a + "\",\"" + b + "\"]}");
        assertThat(alta.statusCode()).isEqualTo(201);
        assertThat(alta.body())
                .contains("\"email\":\"" + a + "\"")
                .contains("\"email\":\"" + b + "\"")
                .contains("\"yaEnCurso\":[]")
                .contains("\"conOtroRol\":[]");
        HttpResponse<String> otraVez =
                docente.post("/api/cursos/" + idCurso + "/correos", "{\"emails\":[\"" + a + "\"]}");
        assertThat(otraVez.body()).contains("\"yaEnCurso\":[\"" + a + "\"]");
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_curso = :c")
                        .param("c", idCurso)
                        .query(Integer.class)
                        .single())
                .isEqualTo(2);

        registrarEnCurso(docente, idCurso, a, "Ana Prueba");
        HttpResponse<String> miembro =
                docente.post("/api/equipos/" + idEquipo1 + "/miembros", "{\"email\":\"" + a + "\"}");
        assertThat(miembro.statusCode()).isEqualTo(201);
        assertThat(miembro.body()).contains("\"nombre\":\"Ana Prueba\"").contains("\"rolEquipo\":\"INTEGRANTE\"");
        assertThat(docente.post("/api/equipos/" + idEquipo2 + "/miembros", "{\"email\":\"" + a + "\"}")
                        .body())
                .contains("\"codigo\":\"YA_TIENE_EQUIPO\"");
        assertThat(docente.post("/api/equipos/" + idEquipo2 + "/miembros", "{\"email\":\"" + b + "\"}")
                        .body())
                .contains("\"codigo\":\"ESTUDIANTE_SIN_CURSO\"");
        assertThat(docente.post(
                                "/api/equipos/" + idEquipo2 + "/miembros",
                                "{\"email\":\"" + correoUnico("nadie") + "\"}")
                        .body())
                .contains("\"codigo\":\"ESTUDIANTE_SIN_CURSO\"");

        Navegador ana = sesion(a);
        assertThat(tieneValor(ana.get("/api/proyectos").body(), "idProyecto", id))
                .isTrue();
        assertThat(ana.get("/api/proyectos/" + id).statusCode()).isEqualTo(200);
        assertThat(ana.post("/api/proyectos/" + id + "/equipos", "{\"nombreEquipo\":\"Pirata\"}")
                        .statusCode())
                .isEqualTo(403);
        assertThat(ana.get("/api/proyectos/" + id + "/correos").statusCode()).isEqualTo(403);

        long idAna = idUsuario(a);
        HttpResponse<String> fuera = docente.delete("/api/equipos/" + idEquipo1 + "/miembros/" + idAna);
        assertThat(fuera.statusCode()).isEqualTo(200);
        assertThat(fuera.body()).doesNotContain("Ana Prueba");
        assertThat(docente.delete("/api/equipos/" + idEquipo1 + "/miembros/" + idAna)
                        .statusCode())
                .isEqualTo(404);
    }

    @Test
    void unSoloLiderPorEquipo() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        long idCurso = crearCurso(docente, "Curso Lider");
        asignarCursos(docente, id, idCurso);
        long idEquipo = crearEquipo(docente, id, "Equipo L");
        String a = correoUnico("est");
        String b = correoUnico("est");
        agregarAlCurso(docente, idCurso, a, b);
        registrarEnCurso(docente, idCurso, a, "Lider Uno");
        registrarEnCurso(docente, idCurso, b, "Lider Dos");
        assertThat(docente.post("/api/equipos/" + idEquipo + "/miembros", "{\"email\":\"" + a + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertThat(docente.post("/api/equipos/" + idEquipo + "/miembros", "{\"email\":\"" + b + "\"}")
                        .statusCode())
                .isEqualTo(201);
        long idA = idUsuario(a);
        long idB = idUsuario(b);
        String lider = "/api/equipos/" + idEquipo + "/lider";
        HttpResponse<String> primero = docente.put(lider, "{\"idUsuario\":" + idA + "}");
        assertThat(primero.statusCode()).isEqualTo(200);
        assertThat(primero.body()).contains("\"rolEquipo\":\"LIDER\"");
        assertThat(docente.put(lider, "{\"idUsuario\":" + idB + "}").statusCode())
                .isEqualTo(200);
        assertThat(jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", idEquipo)
                        .query(Long.class)
                        .list())
                .containsExactly(idB);
        HttpResponse<String> ajeno = docente.put(lider, "{\"idUsuario\":999999}");
        assertThat(ajeno.statusCode()).isEqualTo(409);
        assertThat(ajeno.body()).contains("\"codigo\":\"NO_ES_MIEMBRO\"");
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'equipo' "
                                + "AND id_registro_afectado = :e AND campo_modificado = 'lider'")
                        .param("e", idEquipo)
                        .query(Integer.class)
                        .single())
                .isEqualTo(2);
    }

    @Test
    void monitoresDelProyecto() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        String monitor = correoUnico("mon");
        long idMonitor = crearUsuario("Monitor Prueba", monitor, "MONITOR");
        String estudiante = correoUnico("est");
        crearUsuario("Estudiante Prueba", estudiante, "ESTUDIANTE");
        HttpResponse<String> sinMonitores =
                docente.post("/api/proyectos/" + id + "/correos", correos("MONITOR", monitor));
        assertThat(sinMonitores.statusCode()).isEqualTo(409);
        assertThat(sinMonitores.body()).contains("\"codigo\":\"PROYECTO_SIN_MONITORES\"");
        assertThat(docente.put("/api/proyectos/" + id, proyecto("Proyecto de prueba", true))
                        .statusCode())
                .isEqualTo(200);
        HttpResponse<String> alta =
                docente.post("/api/proyectos/" + id + "/correos", correos("MONITOR", monitor, estudiante));
        assertThat(alta.statusCode()).isEqualTo(201);
        assertThat(alta.body()).contains("\"registrado\":true").contains("\"conOtroRol\":[\"" + estudiante + "\"]");
        assertThat(docente.get("/api/proyectos/" + id + "/monitores").body()).contains("Monitor Prueba");

        Navegador mon = sesion(monitor);
        assertThat(mon.get("/api/proyectos/" + id).statusCode()).isEqualTo(200);
        assertThat(docente.delete("/api/proyectos/" + id + "/monitores/" + idMonitor)
                        .statusCode())
                .isEqualTo(204);
        assertThat(mon.get("/api/proyectos/" + id).statusCode()).isEqualTo(403);
        assertThat(docente.post("/api/proyectos/" + id + "/correos", correos("DOCENTE", "x@u.edu.co"))
                        .body())
                .contains("\"codigo\":\"ROL_INVALIDO\"");
    }

    @Test
    void laAlertaDeInactividadEsOpcional() {
        Navegador docente = docente();
        long id = crearProyecto(docente, false);
        HttpResponse<String> h1 = docente.post(
                "/api/proyectos/" + id + "/hitos",
                "{\"nombreHito\":\"Con alerta\","
                        + "\"orden\":1,\"fechaInicio\":\"2026-08-03\",\"fechaLimite\":\"2026-09-15\","
                        + "\"diasInactividadAlerta\":4,\"notificarDocenteInactividad\":true}");
        assertThat(h1.statusCode()).isEqualTo(201);
        assertThat(h1.body()).contains("\"diasInactividadAlerta\":4");
        long idHito = numero(h1.body(), "idHito");

        HttpResponse<String> mod = docente.put(
                "/api/hitos/" + idHito,
                "{\"nombreHito\":\"Con alerta\","
                        + "\"orden\":1,\"fechaInicio\":\"2026-08-03\",\"fechaLimite\":\"2026-09-15\","
                        + "\"diasInactividadAlerta\":null,\"notificarDocenteInactividad\":false}");
        assertThat(mod.statusCode()).isEqualTo(200);
        assertThat(mod.body()).contains("\"diasInactividadAlerta\":null");

        assertThat(jdbc.sql("SELECT count(*) FROM hito WHERE id_hito = :id AND dias_inactividad_alerta IS NULL")
                        .param("id", idHito)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT coalesce(valor_anterior,'-') || '>' || coalesce(valor_nuevo,'-') "
                                + "FROM log_auditoria WHERE entidad_afectada = 'hito' AND id_registro_afectado = :id "
                                + "AND campo_modificado = 'dias_inactividad_alerta'")
                        .param("id", idHito)
                        .query(String.class)
                        .single())
                .isEqualTo("4>-");

        HttpResponse<String> inv = docente.put(
                "/api/hitos/" + idHito,
                "{\"nombreHito\":\"Con alerta\","
                        + "\"orden\":1,\"fechaInicio\":\"2026-08-03\",\"fechaLimite\":\"2026-09-15\","
                        + "\"diasInactividadAlerta\":0,\"notificarDocenteInactividad\":false}");
        assertThat(inv.statusCode()).isEqualTo(400);
        assertThat(inv.body()).contains("\"codigo\":\"VALIDACION\"");
    }

    @Test
    void unMonitorQuitadoSePuedeVolverAPonerYOtroTambien() {
        Navegador docente = docente();
        long id = crearProyecto(docente, true);
        String correoUno = correoUnico("mon");
        String correoDos = correoUnico("mon");
        long idUno = crearUsuario("Monitor Uno", correoUno, "MONITOR");
        long idDos = crearUsuario("Monitor Dos", correoDos, "MONITOR");

        HttpResponse<String> hUno = docente.post("/api/proyectos/" + id + "/correos", correos("MONITOR", correoUno));
        assertThat(hUno.statusCode()).isEqualTo(201);
        assertThat(docente.delete("/api/proyectos/" + id + "/monitores/" + idUno)
                        .statusCode())
                .isEqualTo(204);

        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_proyecto = :p AND email = :e")
                        .param("p", id)
                        .param("e", correoUno)
                        .query(Integer.class)
                        .single())
                .isEqualTo(0);
        assertThat(docente.get("/api/proyectos/" + id + "/monitores").body()).doesNotContain("Monitor Uno");
        Navegador sesionUno = sesion(correoUno);
        assertThat(sesionUno.get("/api/proyectos/" + id).statusCode()).isEqualTo(403);

        HttpResponse<String> hAmbos =
                docente.post("/api/proyectos/" + id + "/correos", correos("MONITOR", correoUno, correoDos));
        assertThat(hAmbos.statusCode()).isEqualTo(201);
        assertThat(hAmbos.body()).contains("\"yaHabilitados\":[]");
        assertThat(docente.get("/api/proyectos/" + id + "/monitores").body())
                .contains("Monitor Uno")
                .contains("Monitor Dos");
        assertThat(sesionUno.get("/api/proyectos/" + id).statusCode()).isEqualTo(200);

        long idDocente = jdbc.sql("SELECT id_docente FROM proyecto WHERE id_proyecto = :p")
                .param("p", id)
                .query(Long.class)
                .single();
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'correo_habilitado' "
                                + "AND accion = 'ELIMINAR' AND id_usuario_accion = :doc")
                        .param("doc", idDocente)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        assertThat(docente.delete("/api/proyectos/" + id + "/monitores/" + idDos)
                        .statusCode())
                .isEqualTo(204);
        assertThat(docente.delete("/api/proyectos/" + id + "/monitores/" + idDos)
                        .statusCode())
                .isEqualTo(404);
    }

    @Test
    void seRetiraUnCorreoHabilitadoPendiente() {
        Navegador docente = docente();
        long id = crearProyecto(docente, true);
        String correoMon = correoUnico("pend");
        HttpResponse<String> rHab = docente.post("/api/proyectos/" + id + "/correos", correos("MONITOR", correoMon));
        assertThat(rHab.statusCode()).isEqualTo(201);
        long idCorreo = numero(rHab.body(), "idCorreoHabilitado");
        String codigo = enlace(docente, id, correoMon);

        assertThat(docente.delete("/api/proyectos/" + id + "/correos/" + idCorreo)
                        .statusCode())
                .isEqualTo(204);
        assertThat(docente.get("/api/proyectos/" + id + "/correos").body()).doesNotContain(correoMon);

        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        HttpResponse<String> reg = n.post(
                "/api/cuentas/registro",
                "{\"nombre\":\"Monitor Pendiente\",\"email\":\""
                        + correoMon + "\",\"contrasena\":\"" + CLAVE
                        + "\",\"autorizaTratamientoDatos\":true,\"codigo\":\""
                        + codigo + "\"}");
        assertThat(reg.statusCode()).isEqualTo(409);
        assertThat(reg.body()).contains("\"codigo\":\"REGISTRO_NO_DISPONIBLE\"");

        long idDocente = jdbc.sql("SELECT id_docente FROM proyecto WHERE id_proyecto = :p")
                .param("p", id)
                .query(Long.class)
                .single();
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'correo_habilitado' "
                                + "AND id_registro_afectado = :id AND accion = 'ELIMINAR' AND id_usuario_accion = :doc "
                                + "AND campo_modificado IS NULL AND valor_anterior IS NULL AND valor_nuevo IS NULL")
                        .param("id", idCorreo)
                        .param("doc", idDocente)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        HttpResponse<String> rHab2 = docente.post("/api/proyectos/" + id + "/correos", correos("MONITOR", correoMon));
        assertThat(rHab2.statusCode()).isEqualTo(201);
        assertThat(rHab2.body()).contains("\"yaHabilitados\":[]");
        assertThat(docente.delete("/api/proyectos/" + id + "/correos/" + idCorreo)
                        .statusCode())
                .isEqualTo(404);

        String correoMonPend = correoUnico("monpend");
        HttpResponse<String> rMonPend =
                docente.post("/api/proyectos/" + id + "/correos", correos("MONITOR", correoMonPend));
        assertThat(rMonPend.statusCode()).isEqualTo(201);
        long idMonPend = numero(rMonPend.body(), "idCorreoHabilitado");
        assertThat(docente.delete("/api/proyectos/" + id + "/correos/" + idMonPend)
                        .statusCode())
                .isEqualTo(204);
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_correo_habilitado = :id")
                        .param("id", idMonPend)
                        .query(Integer.class)
                        .single())
                .isEqualTo(0);
        assertThat(docente.get("/api/proyectos/" + id + "/monitores").body()).isEqualTo("[]");
    }

    @Test
    void noSeRetiraUnCorreoYaRegistradoNiAjeno() {
        Navegador docenteA = docente();
        long idA = crearProyecto(docenteA, true);
        String correoReg = correoUnico("reg");
        HttpResponse<String> rHabA = docenteA.post("/api/proyectos/" + idA + "/correos", correos("MONITOR", correoReg));
        long idUsado = numero(rHabA.body(), "idCorreoHabilitado");
        registrarMonitor(docenteA, idA, correoReg, "Monitor Registrado");

        HttpResponse<String> delUsado = docenteA.delete("/api/proyectos/" + idA + "/correos/" + idUsado);
        assertThat(delUsado.statusCode()).isEqualTo(409);
        assertThat(delUsado.body()).contains("\"codigo\":\"HABILITACION_USADA\"");
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_correo_habilitado = :id "
                                + "AND fecha_uso IS NOT NULL")
                        .param("id", idUsado)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        Navegador docenteB = docente();
        long idBProyecto = crearProyecto(docenteB, true);
        HttpResponse<String> rHabB =
                docenteB.post("/api/proyectos/" + idBProyecto + "/correos", correos("MONITOR", correoUnico("pendb")));
        long idB = numero(rHabB.body(), "idCorreoHabilitado");

        assertThat(docenteB.delete("/api/proyectos/" + idA + "/correos/" + idUsado)
                        .statusCode())
                .isEqualTo(403);

        assertThat(docenteA.delete("/api/proyectos/" + idA + "/correos/" + idB).statusCode())
                .isEqualTo(404);
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_correo_habilitado = :id")
                        .param("id", idB)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        assertThat(docenteA.delete("/api/proyectos/" + idA + "/correos/999999999")
                        .statusCode())
                .isEqualTo(404);

        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'correo_habilitado' "
                                + "AND accion = 'ELIMINAR' AND id_registro_afectado IN (:u, :b)")
                        .param("u", idUsado)
                        .param("b", idB)
                        .query(Integer.class)
                        .single())
                .isEqualTo(0);
    }

    @Test
    void laConfiguracionQuedaAuditadaSinCorreos() {
        Navegador docente = docente();
        long id = crearProyecto(docente, true);
        assertThat(docente.put(
                                "/api/proyectos/" + id,
                                "{\"nombre\":\"Proyecto de prueba\",\"descripcion\":\"Curso de prueba\","
                                        + "\"fechaInicio\":\"2026-08-03\",\"fechaFin\":\"2026-12-04\",\"usaMonitores\":true}")
                        .statusCode())
                .isEqualTo(200);
        habilitar(docente, id, "MONITOR", correoUnico("mon"));
        assertThat(jdbc.sql(
                                "SELECT accion || ':' || coalesce(campo_modificado, '-') || ':' || coalesce(valor_anterior, '-') "
                                        + "|| ':' || coalesce(valor_nuevo, '-') FROM log_auditoria WHERE entidad_afectada = 'proyecto' "
                                        + "AND id_registro_afectado = :p ORDER BY id_log")
                        .param("p", id)
                        .query(String.class)
                        .list())
                .containsExactly("CREAR:-:-:-", "ACTUALIZAR:fecha_fin:2026-11-27:2026-12-04");
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE accion = 'HABILITAR_CORREO' "
                                + "AND valor_nuevo = 'MONITOR' AND id_registro_afectado IN "
                                + "(SELECT id_correo_habilitado FROM correo_habilitado WHERE id_proyecto = :p)")
                        .param("p", id)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria "
                                + "WHERE coalesce(valor_anterior, '') || coalesce(valor_nuevo, '') LIKE '%@%'")
                        .query(Integer.class)
                        .single())
                .isZero();
    }
}
