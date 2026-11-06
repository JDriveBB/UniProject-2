package co.uniproject.nucleo.ejecucion;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.sql.Types;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReasignacionIntegracionTest extends ApiIntegracionBase {

    private record Escenario(long idHitoEquipo, String docente, String lider, String integrante, String otro,
                             long idLider, long idIntegrante, long idOtro, long tarea) {}

    /** Hito vigente, equipo (líder + 2) y la tarea "Modelo de datos" (6 h) a cargo de "Integrante Uno". */
    private Escenario escenario() {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Reasignacion", docente, "DOCENTE");
        String lider = correoUnico("lider");
        long idLider = crearUsuario("Lider Reasignacion", lider, "ESTUDIANTE");
        String integrante = correoUnico("int");
        long idIntegrante = crearUsuario("Integrante Uno", integrante, "ESTUDIANTE");
        String otro = correoUnico("otro");
        long idOtro = crearUsuario("Integrante Dos", otro, "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Reasignacion', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(30)).param("f", hoy.plusDays(60))
                .query(Long.class).single();
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'Hito 1', 1, :i, :f) RETURNING id_hito")
                .param("p", proyecto).param("i", hoy.minusDays(10)).param("f", hoy.plusDays(20))
                .query(Long.class).single();
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo R') "
                        + "RETURNING id_equipo")
                .param("p", proyecto).query(Long.class).single();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :l, 'LIDER'), "
                        + "(:e, :a, 'INTEGRANTE'), (:e, :b, 'INTEGRANTE')")
                .param("e", equipo).param("l", idLider).param("a", idIntegrante).param("b", idOtro).update();
        long he = jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) VALUES (:h, :e) RETURNING id_hito_equipo")
                .param("h", hito).param("e", equipo).query(Long.class).single();
        long tarea = tarea(he, idIntegrante, "Modelo de datos", "6");
        return new Escenario(he, docente, lider, integrante, otro, idLider, idIntegrante, idOtro, tarea);
    }

    private long tarea(long idHitoEquipo, Long encargado, String titulo, String horas) {
        return jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                        + "VALUES (:he, :u, :t, :h) RETURNING id_tarea")
                .param("he", idHitoEquipo).param("u", encargado, Types.BIGINT).param("t", titulo)
                .param("h", new BigDecimal(horas)).query(Long.class).single();
    }

    private int contar(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(Integer.class).single();
    }

    private static String total(long idNuevo) {
        return "{\"idNuevoEncargado\":" + idNuevo + ",\"tipo\":\"TOTAL\"}";
    }

    private static String rutaReasignacion(long idTarea) {
        return "/api/tareas/" + idTarea + "/reasignacion";
    }

    @Test
    void laReasignacionTotalCambiaElEncargadoYAvisaALosDos() {
        Escenario e = escenario();
        long t = e.tarea();
        long ev = jdbc.sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url) "
                        + "VALUES (:t, :u, 'ENLACE', 'https://github.com/equipo/v1') RETURNING id_evidencia")
                .param("t", t).param("u", e.idIntegrante()).query(Long.class).single();
        jdbc.sql("INSERT INTO solicitud_correccion (id_tarea, id_evidencia, id_lider, id_estudiante, observaciones) "
                        + "VALUES (:t, :ev, :l, :i, 'Falta el diagrama')")
                .param("t", t).param("ev", ev).param("l", e.idLider()).param("i", e.idIntegrante()).update();
        HttpResponse<String> r = sesion(e.lider()).post(rutaReasignacion(t), "{\"idNuevoEncargado\":" + e.idOtro()
                + ",\"tipo\":\"TOTAL\",\"motivo\":\"Cambio de carga\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat(tieneValor(r.body(), "idEncargado", e.idOtro())).isTrue();
        assertThat(jdbc.sql("SELECT valor_anterior || '>' || valor_nuevo FROM log_auditoria "
                        + "WHERE entidad_afectada = 'tarea' AND id_registro_afectado = :id "
                        + "AND accion = 'REASIGNAR_TAREA' AND campo_modificado = 'id_encargado'")
                .param("id", t).query(String.class).single()).isEqualTo(e.idIntegrante() + ">" + e.idOtro());
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'tarea' "
                + "AND id_registro_afectado = :id AND campo_modificado = 'motivo'", t)).isEqualTo(1);
        assertThat(jdbc.sql("SELECT coalesce(l.valor_nuevo, '-') || '|' || x.valor_nuevo FROM log_auditoria l "
                        + "JOIN texto_auditoria x ON x.id_log = l.id_log WHERE l.entidad_afectada = 'tarea' "
                        + "AND l.id_registro_afectado = :id AND l.campo_modificado = 'motivo'")
                .param("id", t).query(String.class).single()).isEqualTo("-|Cambio de carga");
        assertThat(contar("SELECT count(*) FROM evento_saliente WHERE tipo = 'tarea.reasignada' "
                + "AND (carga->'data'->'tarea'->>'id')::bigint = :id", t)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM notificacion WHERE tipo = 'tarea.reasignada' "
                + "AND id_referencia = :id", t)).isEqualTo(2);
        assertThat(contar("SELECT id_estudiante FROM solicitud_correccion WHERE id_tarea = :id", t))
                .isEqualTo((int) e.idOtro());
    }

    @Test
    void laReasignacionParcialDivideLasHorasYCopiaLasDependencias() {
        Escenario e = escenario();
        long t = e.tarea();
        long previa = tarea(e.idHitoEquipo(), e.idLider(), "Requisitos", "2");
        long siguiente = tarea(e.idHitoEquipo(), e.idOtro(), "Pruebas", "1");
        jdbc.sql("INSERT INTO dependencia_tarea (id_tarea_predecesora, id_tarea_dependiente) VALUES (:p, :t), (:t, :s)")
                .param("p", previa).param("t", t).param("s", siguiente).update();
        jdbc.sql("UPDATE tarea SET es_prerrequisito_critico = true WHERE id_tarea = :t").param("t", t).update();
        HttpResponse<String> r = sesion(e.lider()).post(rutaReasignacion(t), "{\"idNuevoEncargado\":" + e.idOtro()
                + ",\"tipo\":\"PARCIAL\",\"tituloParcial\":\"Modelo de datos: vistas\",\"horasParcial\":2.5}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        long nueva = jdbc.sql("SELECT id_tarea FROM tarea WHERE id_tarea_origen = :t").param("t", t)
                .query(Long.class).single();
        assertThat(tieneValor(r.body(), "idTareaOrigen", t)).isTrue();
        assertThat(jdbc.sql("SELECT id_encargado || ':' || horas_estimadas FROM tarea WHERE id_tarea = :t")
                .param("t", t).query(String.class).single()).isEqualTo(e.idIntegrante() + ":3.50");
        assertThat(jdbc.sql("SELECT id_encargado || ':' || horas_estimadas || ':' || es_prerrequisito_critico "
                        + "|| ':' || estado FROM tarea WHERE id_tarea = :t")
                .param("t", nueva).query(String.class).single()).isEqualTo(e.idOtro() + ":2.50:true:PENDIENTE");
        assertThat(jdbc.sql("SELECT count(*) FROM dependencia_tarea WHERE (id_tarea_predecesora = :p "
                        + "AND id_tarea_dependiente = :n) OR (id_tarea_predecesora = :n AND id_tarea_dependiente = :s)")
                .param("p", previa).param("n", nueva).param("s", siguiente).query(Integer.class).single())
                .isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM evento_saliente WHERE tipo = 'tarea.reasignada' "
                + "AND (carga->'data'->'tarea'->>'id')::bigint = :id", nueva)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'tarea' "
                + "AND id_registro_afectado = :id AND accion = 'CREAR'", nueva)).isEqualTo(1);
    }

    @Test
    void validaQuienYComoSeReasigna() {
        Escenario e = escenario();
        long t = e.tarea();
        Navegador lider = sesion(e.lider());
        assertThat(sesion(e.integrante()).post(rutaReasignacion(t), total(e.idOtro())).statusCode()).isEqualTo(403);
        HttpResponse<String> mismo = lider.post(rutaReasignacion(t), total(e.idIntegrante()));
        assertThat(mismo.statusCode()).isEqualTo(409);
        assertThat(mismo.body()).contains("\"codigo\":\"MISMO_ENCARGADO\"");
        long ajeno = crearUsuario("Ajeno", correoUnico("ajeno"), "ESTUDIANTE");
        HttpResponse<String> fuera = lider.post(rutaReasignacion(t), total(ajeno));
        assertThat(fuera.statusCode()).isEqualTo(409);
        assertThat(fuera.body()).contains("\"codigo\":\"ENCARGADO_NO_ES_MIEMBRO\"");
        HttpResponse<String> incompleta = lider.post(rutaReasignacion(t), "{\"idNuevoEncargado\":" + e.idOtro()
                + ",\"tipo\":\"PARCIAL\",\"horasParcial\":2}");
        assertThat(incompleta.statusCode()).isEqualTo(400);
        assertThat(incompleta.body()).contains("\"codigo\":\"DATOS_PARCIAL_INCOMPLETOS\"");
        HttpResponse<String> horas = lider.post(rutaReasignacion(t), "{\"idNuevoEncargado\":" + e.idOtro()
                + ",\"tipo\":\"PARCIAL\",\"tituloParcial\":\"Todo\",\"horasParcial\":6}");
        assertThat(horas.statusCode()).isEqualTo(400);
        assertThat(horas.body()).contains("\"codigo\":\"HORAS_PARCIAL_INVALIDAS\"");
        long libre = tarea(e.idHitoEquipo(), null, "Libre", "1");
        HttpResponse<String> sinEncargado = lider.post(rutaReasignacion(libre), total(e.idOtro()));
        assertThat(sinEncargado.statusCode()).isEqualTo(409);
        assertThat(sinEncargado.body()).contains("\"codigo\":\"SIN_ENCARGADO\"");
        jdbc.sql("UPDATE tarea SET estado = 'COMPLETADA', porcentaje_avance = 100, fecha_completada = now() "
                + "WHERE id_tarea = :t").param("t", t).update();
        HttpResponse<String> completada = lider.post(rutaReasignacion(t), total(e.idOtro()));
        assertThat(completada.statusCode()).isEqualTo(409);
        assertThat(completada.body()).contains("\"codigo\":\"TAREA_COMPLETADA\"");
        jdbc.sql("UPDATE hito_equipo SET estado = 'ENVIADO' WHERE id_hito_equipo = :he")
                .param("he", e.idHitoEquipo()).update();
        HttpResponse<String> enviado = lider.post(rutaReasignacion(libre), total(e.idOtro()));
        assertThat(enviado.statusCode()).isEqualTo(409);
        assertThat(enviado.body()).contains("\"codigo\":\"HITO_NO_EDITABLE\"");
        assertThat(contar("SELECT count(*) FROM evento_saliente WHERE tipo = 'tarea.reasignada' "
                + "AND (carga->'data'->'equipo'->>'id')::bigint = (SELECT id_equipo FROM hito_equipo "
                + "WHERE id_hito_equipo = :id)", e.idHitoEquipo())).isZero();
    }

    @Test
    void soloElEncargadoPideApoyoYElEquipoLoVe() {
        Escenario e = escenario();
        String ruta = "/api/tareas/" + e.tarea() + "/apoyo";
        HttpResponse<String> r = sesion(e.integrante()).post(ruta, "{\"mensaje\":\"No entiendo la normalización\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        assertThat(r.body()).contains("\"autor\":\"Integrante Uno\"").contains("\"titulo\":\"Modelo de datos\"");
        assertThat(sesion(e.lider()).post(ruta, "{\"mensaje\":\"Yo también\"}").statusCode()).isEqualTo(403);
        assertThat(sesion(e.otro()).post(ruta, "{\"mensaje\":\"Yo también\"}").statusCode()).isEqualTo(403);
        assertThat(sesion(e.integrante()).post(ruta, "{\"mensaje\":\"  \"}").statusCode()).isEqualTo(400);
        HttpResponse<String> lista = sesion(e.docente()).get("/api/hitos-equipo/" + e.idHitoEquipo() + "/apoyos");
        assertThat(lista.statusCode()).isEqualTo(200);
        assertThat(lista.body()).contains("No entiendo la normalización");
        String ajeno = correoUnico("ajeno");
        crearUsuario("Ajeno", ajeno, "ESTUDIANTE");
        assertThat(sesion(ajeno).get("/api/hitos-equipo/" + e.idHitoEquipo() + "/apoyos").statusCode())
                .isEqualTo(403);
        long idSolicitud = numero(r.body(), "idSolicitud");
        assertThat(jdbc.sql("SELECT campo_modificado || '|' || valor_nuevo FROM log_auditoria "
                        + "WHERE entidad_afectada = 'tarea' AND id_registro_afectado = :id AND accion = 'SOLICITAR_APOYO'")
                .param("id", e.tarea()).query(String.class).single()).isEqualTo("solicitud_apoyo|" + idSolicitud);
        assertThat(contar("SELECT count(*) FROM solicitud_apoyo WHERE id_tarea = :id "
                + "AND mensaje = 'No entiendo la normalización'", e.tarea())).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM evento_saliente WHERE (carga->'data'->'tarea'->>'id')::bigint = :id",
                e.tarea())).isZero();
    }

    @Test
    void laCargaMuestraHorasRestantesPorIntegrante() {
        Escenario e = escenario();
        jdbc.sql("UPDATE tarea SET estado = 'EN_PROGRESO', porcentaje_avance = 50 WHERE id_tarea = :t")
                .param("t", e.tarea()).update();
        long hecha = tarea(e.idHitoEquipo(), e.idOtro(), "Hecha", "2");
        jdbc.sql("UPDATE tarea SET estado = 'COMPLETADA', porcentaje_avance = 100, fecha_completada = now() "
                + "WHERE id_tarea = :t").param("t", hecha).update();
        HttpResponse<String> r = sesion(e.lider()).get("/api/hitos-equipo/" + e.idHitoEquipo() + "/carga");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat(r.body())
                .contains("\"nombre\":\"Integrante Uno\",\"rolEquipo\":\"INTEGRANTE\",\"tareasPendientes\":1,"
                        + "\"tareasCompletadas\":0,\"horasRestantes\":3.00")
                .contains("\"nombre\":\"Integrante Dos\",\"rolEquipo\":\"INTEGRANTE\",\"tareasPendientes\":0,"
                        + "\"tareasCompletadas\":1,\"horasRestantes\":0.00")
                .contains("\"nombre\":\"Lider Reasignacion\",\"rolEquipo\":\"LIDER\",\"tareasPendientes\":0,"
                        + "\"tareasCompletadas\":0,\"horasRestantes\":0.00");
    }

    @Test
    void rechazaMensajeDeApoyoConSoloEspaciosUnicode() {
        Escenario e = escenario();
        String ruta = "/api/tareas/" + e.tarea() + "/apoyo";
        HttpResponse<String> r = sesion(e.integrante()).post(ruta, "{\"mensaje\":\"\\u2003\\u2003\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(400);
        assertThat(contar("SELECT count(*) FROM solicitud_apoyo WHERE id_tarea = :id", e.tarea())).isZero();
    }
}
