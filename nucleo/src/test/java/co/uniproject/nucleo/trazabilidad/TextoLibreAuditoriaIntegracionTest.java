package co.uniproject.nucleo.trazabilidad;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.configuracionacademica.servicio.AuditoriaCambios;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Bloque 29: el texto libre que escriben las personas vive fuera del log inmutable y se borra con la supresión. */
class TextoLibreAuditoriaIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private RegistroAuditoria auditoria;

    @Autowired
    private TransactionTemplate tx;

    private record Escenario(long idEquipo, long idHitoEquipo, String docente, String lider, long idLider,
                             String estudiante, long idEstudiante, long idOtro, long tarea) {}

    /** Proyecto y hito vigentes, equipo (líder + estudiante + otro) y la tarea "Informe" (4 h) del estudiante. */
    private Escenario escenario() {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("doctexto");
        long idDocente = crearUsuario("Docente Texto", docente, "DOCENTE");
        String lider = correoUnico("lidertexto");
        long idLider = crearUsuario("Lider Texto", lider, "ESTUDIANTE");
        String estudiante = correoUnico("esttexto");
        long idEstudiante = crearUsuario("Estudiante Texto", estudiante, "ESTUDIANTE");
        long idOtro = crearUsuario("Otro Texto", correoUnico("otrotexto"), "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Texto', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(30)).param("f", hoy.plusDays(60))
                .query(Long.class).single();
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'Hito 1', 1, :i, :f) RETURNING id_hito")
                .param("p", proyecto).param("i", hoy.minusDays(10)).param("f", hoy.plusDays(20))
                .query(Long.class).single();
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo T') "
                        + "RETURNING id_equipo")
                .param("p", proyecto).query(Long.class).single();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :l, 'LIDER'), "
                        + "(:e, :a, 'INTEGRANTE'), (:e, :b, 'INTEGRANTE')")
                .param("e", equipo).param("l", idLider).param("a", idEstudiante).param("b", idOtro).update();
        long he = jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) VALUES (:h, :e) RETURNING id_hito_equipo")
                .param("h", hito).param("e", equipo).query(Long.class).single();
        long tarea = jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                        + "VALUES (:he, :u, 'Informe', 4) RETURNING id_tarea")
                .param("he", he).param("u", idEstudiante).query(Long.class).single();
        return new Escenario(equipo, he, docente, lider, idLider, estudiante, idEstudiante, idOtro, tarea);
    }

    private int contar(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(Integer.class).single();
    }

    private String texto(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(String.class).single();
    }

    /** Filas del log que tienen el fragmento en el campo o en los valores. */
    private int enElLog(String fragmento) {
        return jdbc.sql("SELECT count(*) FROM log_auditoria WHERE strpos(coalesce(campo_modificado, '') "
                        + "|| coalesce(valor_anterior, '') || coalesce(valor_nuevo, ''), :f) > 0")
                .param("f", fragmento).query(Integer.class).single();
    }

    private static String apoyo(String mensaje) {
        return "{\"mensaje\":\"" + mensaje + "\"}";
    }

    @Test
    void elApoyoGuardaElMensajeEnSuTablaYElLogSoloElId() {
        Escenario e = escenario();
        String mensaje = "Tengo una cita medica " + UUID.randomUUID();
        HttpResponse<String> r = sesion(e.estudiante()).post("/api/tareas/" + e.tarea() + "/apoyo", apoyo(mensaje));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        long id = numero(r.body(), "idSolicitud");
        assertThat(tieneValor(r.body(), "idAutor", e.idEstudiante())).isTrue();
        assertThat(texto("SELECT mensaje FROM solicitud_apoyo WHERE id_solicitud_apoyo = :id", id)).isEqualTo(mensaje);
        assertThat(texto("SELECT campo_modificado || '|' || valor_nuevo FROM log_auditoria "
                + "WHERE accion = 'SOLICITAR_APOYO' AND id_registro_afectado = :id", e.tarea()))
                .isEqualTo("solicitud_apoyo|" + id);
        assertThat(enElLog(mensaje)).isZero();
        HttpResponse<String> lista = sesion(e.docente()).get("/api/hitos-equipo/" + e.idHitoEquipo() + "/apoyos");
        assertThat(lista.statusCode()).isEqualTo(200);
        assertThat(lista.body()).contains(mensaje);
        assertThat(tieneValor(lista.body(), "idSolicitud", id)).isTrue();
    }

    @Test
    void elMotivoYLosTextosEditadosVanAlAnexoYElHistorialLosMuestra() {
        Escenario e = escenario();
        String motivo = "Esta incapacitado " + UUID.randomUUID();
        HttpResponse<String> r = sesion(e.lider()).post("/api/tareas/" + e.tarea() + "/reasignacion",
                "{\"idNuevoEncargado\":" + e.idOtro() + ",\"tipo\":\"TOTAL\",\"motivo\":\"" + motivo + "\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        tx.executeWithoutResult(estado -> {
            AuditoriaCambios.siCambia(auditoria, e.idLider(), "tarea", e.tarea(), "titulo", "Informe", "Informe final");
            AuditoriaCambios.siCambia(auditoria, e.idLider(), "tarea", e.tarea(), "horas_estimadas",
                    new BigDecimal("4.00"), new BigDecimal("5.50"));
        });
        assertThat(enElLog(motivo)).isZero();
        assertThat(enElLog("Informe final")).isZero();
        assertThat(texto("SELECT x.valor_nuevo FROM texto_auditoria x JOIN log_auditoria l ON l.id_log = x.id_log "
                + "WHERE l.id_registro_afectado = :id AND l.campo_modificado = 'motivo'", e.tarea())).isEqualTo(motivo);
        assertThat(texto("SELECT valor_anterior || '>' || valor_nuevo FROM log_auditoria WHERE entidad_afectada = 'tarea' "
                + "AND id_registro_afectado = :id AND campo_modificado = 'horas_estimadas'", e.tarea())).isEqualTo("4>5.5");
        HttpResponse<String> h = sesion(e.lider()).get("/api/equipos/" + e.idEquipo() + "/historial");
        assertThat(h.statusCode()).as(h.body()).isEqualTo(200);
        assertThat(h.body()).contains("\"detalle\":\"motivo: - -> " + motivo + "\"")
                .contains("\"detalle\":\"titulo: Informe -> Informe final\"")
                .contains("\"detalle\":\"horas_estimadas: 4 -> 5.5\"");
    }

    @Test
    void laSupresionBorraLosTextosDelTitularYConservaLosHechos() {
        Escenario e = escenario();
        String mensaje = "Mensaje privado " + UUID.randomUUID();
        assertThat(sesion(e.estudiante()).post("/api/tareas/" + e.tarea() + "/apoyo", apoyo(mensaje)).statusCode())
                .isEqualTo(201);
        String propio = "Texto del titular " + UUID.randomUUID();
        String ajeno = "Texto del lider " + UUID.randomUUID();
        tx.executeWithoutResult(estado -> {
            auditoria.registrarTexto(EntradaAuditoria.cambio(e.idEstudiante(), "tarea", e.tarea(),
                    AccionAuditoria.ACTUALIZAR, "descripcion", null, propio));
            auditoria.registrarTexto(EntradaAuditoria.cambio(e.idLider(), "tarea", e.tarea(),
                    AccionAuditoria.REASIGNAR_TAREA, "motivo", null, ajeno));
        });
        String hechos = "SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'tarea' "
                + "AND id_registro_afectado = :id";
        int antes = contar(hechos, e.tarea());
        HttpResponse<String> s = sesion(e.estudiante()).post("/api/perfil/supresion",
                "{\"contrasena\":\"" + CLAVE + "\"}");
        assertThat(s.statusCode()).as(s.body()).isEqualTo(204);
        assertThat(texto("SELECT coalesce(mensaje, '-') FROM solicitud_apoyo WHERE id_solicitante = :id",
                e.idEstudiante())).isEqualTo("-");
        assertThat(contar("SELECT count(*) FROM texto_auditoria x JOIN log_auditoria l ON l.id_log = x.id_log "
                + "WHERE l.id_usuario_accion = :id", e.idEstudiante())).isZero();
        assertThat(texto("SELECT x.valor_nuevo FROM texto_auditoria x JOIN log_auditoria l ON l.id_log = x.id_log "
                + "WHERE l.id_usuario_accion = :id AND l.campo_modificado = 'motivo'", e.idLider())).isEqualTo(ajeno);
        assertThat(contar(hechos, e.tarea())).isEqualTo(antes);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE accion = 'SOLICITAR_APOYO' "
                + "AND id_usuario_accion = :id", e.idEstudiante())).isEqualTo(1);
        HttpResponse<String> lista = sesion(e.docente()).get("/api/hitos-equipo/" + e.idHitoEquipo() + "/apoyos");
        assertThat(lista.statusCode()).isEqualTo(200);
        assertThat(lista.body()).contains("\"autor\":\"Usuario anonimizado\"").contains("\"mensaje\":null")
                .doesNotContain(mensaje);
    }
}
