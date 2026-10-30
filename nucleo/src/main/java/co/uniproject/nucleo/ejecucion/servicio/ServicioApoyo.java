package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Solicitud de apoyo (decisión 18-6) y carga del equipo (decisión 18-7). */
@Service
public class ServicioApoyo {

    private static final String APOYOS = "SELECT l.id_log, t.id_tarea, t.titulo, l.id_usuario_accion, "
            + "coalesce(u.nombre, 'Sistema') AS autor, l.valor_nuevo, l.fecha_hora FROM log_auditoria l "
            + "JOIN tarea t ON t.id_tarea = l.id_registro_afectado "
            + "LEFT JOIN usuario u ON u.id_usuario = l.id_usuario_accion "
            + "WHERE l.entidad_afectada = 'tarea' AND l.accion = 'SOLICITAR_APOYO' ";

    private static final String CARGA = "SELECT m.id_usuario, u.nombre, m.rol_equipo, "
            + "count(t.id_tarea) FILTER (WHERE t.estado <> 'COMPLETADA') AS pendientes, "
            + "count(t.id_tarea) FILTER (WHERE t.estado = 'COMPLETADA') AS completadas, "
            + "round(coalesce(sum(t.horas_estimadas * (100 - t.porcentaje_avance) / 100.0) "
            + "FILTER (WHERE t.estado <> 'COMPLETADA'), 0), 2) AS horas_restantes "
            + "FROM miembro_equipo m JOIN usuario u ON u.id_usuario = m.id_usuario "
            + "LEFT JOIN tarea t ON t.id_encargado = m.id_usuario AND t.id_hito_equipo = :he "
            + "WHERE m.id_equipo = :equipo GROUP BY m.id_usuario, u.nombre, m.rol_equipo "
            + "ORDER BY u.nombre, m.id_usuario";

    private final ConsultasEjecucion consultas;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioApoyo(ConsultasEjecucion consultas, Permisos permisos, RegistroAuditoria auditoria,
                         JdbcClient jdbc, Clock reloj) {
        this.consultas = consultas;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional
    public ApoyoVista solicitar(UsuarioAutenticado usuario, long idTarea, SolicitudApoyo solicitud) {
        permisos.exigir(permisos.puedeSolicitarApoyo(usuario.idUsuario(), idTarea));
        ContextoHito hito = consultas.hitoDeTarea(idTarea).orElseThrow();
        EdicionHito.exigirAbierta(hito, Instant.now(reloj));
        auditoria.registrar(EntradaAuditoria.cambio(usuario.idUsuario(), "tarea", idTarea,
                AccionAuditoria.SOLICITAR_APOYO, "mensaje", null, solicitud.mensaje().strip()));
        return jdbc.sql(APOYOS + "AND l.id_registro_afectado = :tarea ORDER BY l.id_log DESC LIMIT 1")
                .param("tarea", idTarea)
                .query(ServicioApoyo::apoyo)
                .single();
    }

    @Transactional(readOnly = true)
    public List<ApoyoVista> apoyos(UsuarioAutenticado usuario, long idHitoEquipo) {
        permisos.exigir(permisos.puedeVerHitoEquipo(usuario.idUsuario(), idHitoEquipo));
        return jdbc.sql(APOYOS + "AND t.id_hito_equipo = :he ORDER BY l.fecha_hora DESC, l.id_log DESC")
                .param("he", idHitoEquipo)
                .query(ServicioApoyo::apoyo)
                .list();
    }

    @Transactional(readOnly = true)
    public List<CargaIntegrante> carga(UsuarioAutenticado usuario, long idHitoEquipo) {
        permisos.exigir(permisos.puedeVerHitoEquipo(usuario.idUsuario(), idHitoEquipo));
        ContextoHito hito = consultas.hito(idHitoEquipo).orElseThrow();
        return jdbc.sql(CARGA)
                .param("he", idHitoEquipo)
                .param("equipo", hito.idEquipo())
                .query((rs, fila) -> new CargaIntegrante(rs.getLong("id_usuario"), rs.getString("nombre"),
                        rs.getString("rol_equipo"), rs.getInt("pendientes"), rs.getInt("completadas"),
                        rs.getBigDecimal("horas_restantes")))
                .list();
    }

    private static ApoyoVista apoyo(ResultSet rs, int fila) throws SQLException {
        return new ApoyoVista(rs.getLong("id_log"), rs.getLong("id_tarea"), rs.getString("titulo"),
                rs.getObject("id_usuario_accion", Long.class), rs.getString("autor"), rs.getString("valor_nuevo"),
                LectorTareas.instante(rs, "fecha_hora"));
    }
}
