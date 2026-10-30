package co.uniproject.nucleo.ejecucion;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TareasIntegracionTest extends ApiIntegracionBase {

    private record Escenario(long idHitoEquipo, String docente, String lider, String integrante, String otro,
                             long idLider, long idIntegrante, long idOtro) {}

    /** Proyecto con un hito vigente (de hoy-10 a hoy+20) y un equipo: líder + dos integrantes. Todo por SQL. */
    private Escenario escenario() {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Tareas", docente, "DOCENTE");
        String lider = correoUnico("lider");
        long idLider = crearUsuario("Lider Tareas", lider, "ESTUDIANTE");
        String integrante = correoUnico("int");
        long idIntegrante = crearUsuario("Integrante Uno", integrante, "ESTUDIANTE");
        String otro = correoUnico("otro");
        long idOtro = crearUsuario("Integrante Dos", otro, "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Tareas', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(30)).param("f", hoy.plusDays(60))
                .query(Long.class).single();
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'Hito 1', 1, :i, :f) RETURNING id_hito")
                .param("p", proyecto).param("i", hoy.minusDays(10)).param("f", hoy.plusDays(20))
                .query(Long.class).single();
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo A') "
                        + "RETURNING id_equipo")
                .param("p", proyecto).query(Long.class).single();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :l, 'LIDER'), "
                        + "(:e, :a, 'INTEGRANTE'), (:e, :b, 'INTEGRANTE')")
                .param("e", equipo).param("l", idLider).param("a", idIntegrante).param("b", idOtro).update();
        long he = jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) VALUES (:h, :e) RETURNING id_hito_equipo")
                .param("h", hito).param("e", equipo).query(Long.class).single();
        return new Escenario(he, docente, lider, integrante, otro, idLider, idIntegrante, idOtro);
    }

    private static String tarea(String titulo, String horas, Long idEncargado) {
        return "{\"titulo\":\"" + titulo + "\",\"horasEstimadas\":" + horas
                + (idEncargado == null ? "" : ",\"idEncargado\":" + idEncargado) + "}";
    }

    private static String avance(int porcentaje) {
        return "{\"porcentajeAvance\":" + porcentaje + "}";
    }

    private long crearTarea(Navegador lider, long idHitoEquipo, String titulo, String horas, Long idEncargado) {
        HttpResponse<String> r = lider.post("/api/hitos-equipo/" + idHitoEquipo + "/tareas",
                tarea(titulo, horas, idEncargado));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idTarea");
    }

    private int contar(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(Integer.class).single();
    }

    private boolean bandera(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(Boolean.class).single();
    }

    @Test
    void elLiderCreaTareasYElEquipoLasVe() {
        Escenario e = escenario();
        Navegador lider = sesion(e.lider());
        long a = crearTarea(lider, e.idHitoEquipo(), "Modelo de datos", "8", e.idIntegrante());
        crearTarea(lider, e.idHitoEquipo(), "Prototipo", "4.5", null);
        Navegador integrante = sesion(e.integrante());
        HttpResponse<String> tablero = integrante.get("/api/hitos-equipo/" + e.idHitoEquipo() + "/tareas");
        assertThat(tablero.statusCode()).isEqualTo(200);
        assertThat(tablero.body()).contains("Modelo de datos").contains("Prototipo").contains("\"editable\":true")
                .contains("\"encargado\":\"Integrante Uno\"");
        assertThat(integrante.post("/api/hitos-equipo/" + e.idHitoEquipo() + "/tareas", tarea("Intento", "1", null))
                .statusCode()).isEqualTo(403);
        String ajeno = correoUnico("ajeno");
        crearUsuario("Ajeno", ajeno, "ESTUDIANTE");
        assertThat(sesion(ajeno).get("/api/hitos-equipo/" + e.idHitoEquipo() + "/tareas").statusCode()).isEqualTo(403);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'tarea' "
                + "AND id_registro_afectado = :id AND accion IN ('CREAR', 'ASIGNAR_TAREA')", a)).isEqualTo(2);
        assertThat(bandera("SELECT estado = 'PENDIENTE' AND fecha_cambio_estado IS NOT NULL FROM tarea "
                + "WHERE id_tarea = :id", a)).isTrue();
    }

    @Test
    void validaHorasEncargadoFechaYReasignacion() {
        Escenario e = escenario();
        Navegador lider = sesion(e.lider());
        String ruta = "/api/hitos-equipo/" + e.idHitoEquipo() + "/tareas";
        HttpResponse<String> horas = lider.post(ruta, tarea("Sin horas", "0", null));
        assertThat(horas.statusCode()).isEqualTo(400);
        assertThat(horas.body()).contains("\"codigo\":\"VALIDACION\"").contains("horasEstimadas");
        String ajeno = correoUnico("ajeno");
        long idAjeno = crearUsuario("Ajeno", ajeno, "ESTUDIANTE");
        HttpResponse<String> encargado = lider.post(ruta, tarea("De otro", "2", idAjeno));
        assertThat(encargado.statusCode()).isEqualTo(409);
        assertThat(encargado.body()).contains("\"codigo\":\"ENCARGADO_NO_ES_MIEMBRO\"");
        String fuera = "{\"titulo\":\"Tarde\",\"horasEstimadas\":2,\"fechaLimite\":\""
                + LocalDate.now(FechasDominio.ZONA).plusDays(90) + "\"}";
        HttpResponse<String> fecha = lider.post(ruta, fuera);
        assertThat(fecha.statusCode()).isEqualTo(400);
        assertThat(fecha.body()).contains("\"codigo\":\"FECHA_FUERA_DEL_HITO\"");
        long id = crearTarea(lider, e.idHitoEquipo(), "Asignada", "3", e.idIntegrante());
        HttpResponse<String> cambio = lider.put("/api/tareas/" + id, tarea("Asignada", "3", e.idOtro()));
        assertThat(cambio.statusCode()).isEqualTo(409);
        assertThat(cambio.body()).contains("\"codigo\":\"USAR_REASIGNACION\"");
        HttpResponse<String> edicion = lider.put("/api/tareas/" + id, tarea("Asignada v2", "5", e.idIntegrante()));
        assertThat(edicion.statusCode()).as(edicion.body()).isEqualTo(200);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'tarea' "
                + "AND id_registro_afectado = :id AND accion = 'ACTUALIZAR'", id)).isEqualTo(2);
    }

    @Test
    void lasDependenciasBloqueanYLosCiclosSeExcluyen() {
        Escenario e = escenario();
        Navegador lider = sesion(e.lider());
        long a = crearTarea(lider, e.idHitoEquipo(), "A", "8", e.idIntegrante());
        long b = crearTarea(lider, e.idHitoEquipo(), "B", "4", e.idOtro());
        long c = crearTarea(lider, e.idHitoEquipo(), "C", "2", null);
        HttpResponse<String> r1 = lider.post("/api/tareas/" + b + "/dependencias", "{\"prerrequisitos\":[" + a + "]}");
        assertThat(r1.statusCode()).as(r1.body()).isEqualTo(200);
        assertThat(r1.body()).contains("\"aceptados\":[" + a + "]").contains("\"ciclicos\":[]");
        assertThat(bandera("SELECT bloqueada_por_dependencia AND fecha_bloqueo IS NOT NULL FROM tarea "
                + "WHERE id_tarea = :id", b)).isTrue();
        HttpResponse<String> r2 = lider.post("/api/tareas/" + a + "/dependencias",
                "{\"prerrequisitos\":[" + b + "," + c + "]}");
        assertThat(r2.statusCode()).isEqualTo(200);
        assertThat(r2.body()).contains("\"aceptados\":[" + c + "]").contains("\"ciclicos\":[" + b + "]");
        assertThat(contar("SELECT count(*) FROM dependencia_tarea WHERE id_tarea_dependiente = :id", a)).isEqualTo(1);
        assertThat(sesion(e.integrante()).post("/api/tareas/" + b + "/dependencias",
                "{\"prerrequisitos\":[" + c + "]}").statusCode()).isEqualTo(403);
        assertThat(lider.delete("/api/tareas/" + b + "/dependencias/" + a).statusCode()).isEqualTo(200);
        assertThat(bandera("SELECT bloqueada_por_dependencia FROM tarea WHERE id_tarea = :id", b)).isFalse();
        assertThat(lider.delete("/api/tareas/" + b + "/dependencias/" + a).statusCode()).isEqualTo(404);
    }

    @Test
    void lasTareasBloqueadasNoAvanzanYAlCompletarSeDesbloquean() {
        Escenario e = escenario();
        Navegador lider = sesion(e.lider());
        long a = crearTarea(lider, e.idHitoEquipo(), "Base", "8", e.idOtro());
        long b = crearTarea(lider, e.idHitoEquipo(), "Encima", "4", e.idIntegrante());
        assertThat(lider.post("/api/tareas/" + b + "/dependencias", "{\"prerrequisitos\":[" + a + "]}").statusCode())
                .isEqualTo(200);
        Navegador integrante = sesion(e.integrante());
        HttpResponse<String> bloqueada = integrante.put("/api/tareas/" + b + "/avance", avance(30));
        assertThat(bloqueada.statusCode()).isEqualTo(409);
        assertThat(bloqueada.body()).contains("\"codigo\":\"TAREA_BLOQUEADA\"").contains("\"detalles\":[\"Base\"]");
        HttpResponse<String> completa = lider.put("/api/tareas/" + a + "/avance",
                "{\"porcentajeAvance\":100,\"observacion\":\"Listo\"}");
        assertThat(completa.statusCode()).as(completa.body()).isEqualTo(200);
        assertThat(completa.body()).contains("\"estado\":\"COMPLETADA\"");
        assertThat(bandera("SELECT bloqueada_por_dependencia FROM tarea WHERE id_tarea = :id", b)).isFalse();
        HttpResponse<String> avanza = integrante.put("/api/tareas/" + b + "/avance", avance(50));
        assertThat(avanza.statusCode()).isEqualTo(200);
        assertThat(avanza.body()).contains("\"estado\":\"EN_PROGRESO\"").contains("\"porcentajeAvance\":50");
        assertThat(contar("SELECT count(*) FROM registro_avance WHERE id_tarea = :id", b)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'tarea' "
                + "AND id_registro_afectado = :id AND accion IN ('ACTUALIZAR_AVANCE', 'CAMBIAR_ESTADO')", a))
                .isEqualTo(2);
    }

    @Test
    void soloElEncargadoYElLiderActualizanElAvance() {
        Escenario e = escenario();
        long t = crearTarea(sesion(e.lider()), e.idHitoEquipo(), "Propia", "2", e.idIntegrante());
        assertThat(sesion(e.otro()).put("/api/tareas/" + t + "/avance", avance(10)).statusCode()).isEqualTo(403);
        assertThat(sesion(e.docente()).put("/api/tareas/" + t + "/avance", avance(10)).statusCode()).isEqualTo(403);
        assertThat(sesion(e.integrante()).put("/api/tareas/" + t + "/avance", avance(10)).statusCode()).isEqualTo(200);
        assertThat(sesion(e.lider()).put("/api/tareas/" + t + "/avance", avance(20)).statusCode()).isEqualTo(200);
        assertThat(sesion(e.integrante()).put("/api/tareas/" + t + "/avance", avance(101)).statusCode()).isEqualTo(400);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE accion = 'ACCESO_DENEGADO' "
                + "AND valor_nuevo = 'PUT /api/tareas/' || :id || '/avance'", t)).isEqualTo(2);
    }

    @Test
    void calculaYGuardaLaRutaCritica() {
        Escenario e = escenario();
        Navegador lider = sesion(e.lider());
        long a = crearTarea(lider, e.idHitoEquipo(), "A", "8", e.idIntegrante());
        long b = crearTarea(lider, e.idHitoEquipo(), "B", "4", e.idIntegrante());
        long c = crearTarea(lider, e.idHitoEquipo(), "C", "2", e.idOtro());
        assertThat(lider.post("/api/tareas/" + b + "/dependencias", "{\"prerrequisitos\":[" + a + "]}").statusCode())
                .isEqualTo(200);
        HttpResponse<String> r = sesion(e.docente()).get("/api/hitos-equipo/" + e.idHitoEquipo() + "/ruta-critica");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"criticas\":[" + a + "," + b + "]");
        assertThat(jdbc.sql("SELECT string_agg(id_tarea::text, ',' ORDER BY id_tarea) FROM tarea "
                        + "WHERE id_hito_equipo = :id AND en_ruta_critica")
                .param("id", e.idHitoEquipo()).query(String.class).single()).isEqualTo(a + "," + b);
        assertThat(jdbc.sql("SELECT nivel_riesgo FROM tarea WHERE id_tarea = :id").param("id", c)
                .query(String.class).single()).isEqualTo("BAJO");
    }

    @Test
    void unHitoEnviadoCongelaSusTareas() {
        Escenario e = escenario();
        Navegador lider = sesion(e.lider());
        long t = crearTarea(lider, e.idHitoEquipo(), "Congelada", "2", e.idLider());
        jdbc.sql("UPDATE hito_equipo SET estado = 'ENVIADO' WHERE id_hito_equipo = :id")
                .param("id", e.idHitoEquipo()).update();
        HttpResponse<String> crear = lider.post("/api/hitos-equipo/" + e.idHitoEquipo() + "/tareas",
                tarea("Nueva", "1", null));
        assertThat(crear.statusCode()).isEqualTo(409);
        assertThat(crear.body()).contains("\"codigo\":\"HITO_NO_EDITABLE\"");
        assertThat(lider.put("/api/tareas/" + t + "/avance", avance(40)).statusCode()).isEqualTo(409);
        assertThat(lider.get("/api/hitos-equipo/" + e.idHitoEquipo() + "/tareas").body()).contains("\"editable\":false");
        jdbc.sql("UPDATE hito_equipo SET estado = 'EVALUADO' WHERE id_hito_equipo = :id")
                .param("id", e.idHitoEquipo()).update();
        assertThat(lider.put("/api/tareas/" + t + "/avance", avance(40)).statusCode()).isEqualTo(200);
    }

    @Test
    void cadaEstudianteConsultaSusTareas() {
        Escenario e = escenario();
        Navegador lider = sesion(e.lider());
        long propia = crearTarea(lider, e.idHitoEquipo(), "Mia", "2", e.idIntegrante());
        long ajena = crearTarea(lider, e.idHitoEquipo(), "Suya", "2", e.idOtro());
        Navegador integrante = sesion(e.integrante());
        assertThat(integrante.put("/api/tareas/" + propia + "/avance",
                "{\"porcentajeAvance\":25,\"observacion\":\"Primer borrador\"}").statusCode()).isEqualTo(200);
        String mias = integrante.get("/api/tareas/mias").body();
        assertThat(tieneValor(mias, "idTarea", propia)).isTrue();
        assertThat(tieneValor(mias, "idTarea", ajena)).isFalse();
        HttpResponse<String> detalle = integrante.get("/api/tareas/" + propia);
        assertThat(detalle.statusCode()).isEqualTo(200);
        assertThat(detalle.body()).contains("Primer borrador").contains("\"porcentajeAvance\":25");
    }

    @Test
    void conUnaCorreccionPendienteNoSePuedeCompletar() {
        Escenario e = escenario();
        long t = crearTarea(sesion(e.lider()), e.idHitoEquipo(), "Con correccion", "2", e.idIntegrante());
        long ev = jdbc.sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url) "
                        + "VALUES (:t, :u, 'ENLACE', 'https://example.org/v1') RETURNING id_evidencia")
                .param("t", t).param("u", e.idIntegrante()).query(Long.class).single();
        jdbc.sql("INSERT INTO solicitud_correccion (id_tarea, id_evidencia, id_lider, id_estudiante, observaciones) "
                        + "VALUES (:t, :ev, :l, :u, 'Falta la portada')")
                .param("t", t).param("ev", ev).param("l", e.idLider()).param("u", e.idIntegrante()).update();
        Navegador integrante = sesion(e.integrante());
        HttpResponse<String> completa = integrante.put("/api/tareas/" + t + "/avance", avance(100));
        assertThat(completa.statusCode()).isEqualTo(409);
        assertThat(completa.body()).contains("\"codigo\":\"CORRECCION_PENDIENTE\"");
        assertThat(integrante.put("/api/tareas/" + t + "/avance", avance(90)).statusCode()).isEqualTo(200);
    }
}
