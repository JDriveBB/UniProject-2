package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitud de apoyo (decisiones 18-6 y 29-2) y carga del equipo (decisión 18-7). El mensaje vive en solicitud_apoyo;
 * el log guarda solo que se creó la solicitud #N.
 */
@Service
public class ServicioApoyo {

    private static final String APOYOS = "SELECT s.id_solicitud_apoyo, t.id_tarea, t.titulo, s.id_solicitante, "
            + "u.nombre AS autor, s.mensaje, s.fecha_hora FROM solicitud_apoyo s "
            + "JOIN tarea t ON t.id_tarea = s.id_tarea "
            + "JOIN usuario u ON u.id_usuario = s.id_solicitante ";

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
        String mensaje = solicitud.mensaje() != null ? solicitud.mensaje().strip() : "";
        if (mensaje.isEmpty()) {
            throw new SolicitudInvalidaException("MENSAJE_VACIO", "El mensaje no puede estar vacío");
        }
        ContextoHito hito = consultas.hitoDeTarea(idTarea).orElseThrow();
        Instant ahora = Instant.now(reloj);
        EdicionHito.exigirAbierta(hito, ahora);
        long id = jdbc.sql("INSERT INTO solicitud_apoyo (id_tarea, id_solicitante, mensaje, fecha_hora) "
                        + "VALUES (:tarea, :usuario, :mensaje, :ahora) RETURNING id_solicitud_apoyo")
                .param("tarea", idTarea)
                .param("usuario", usuario.idUsuario())
                .param("mensaje", mensaje)
                .param("ahora", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .query(Long.class)
                .single();
        auditoria.registrar(EntradaAuditoria.cambio(usuario.idUsuario(), "tarea", idTarea,
                AccionAuditoria.SOLICITAR_APOYO, "solicitud_apoyo", null, String.valueOf(id)));
        return jdbc.sql(APOYOS + "WHERE s.id_solicitud_apoyo = :id")
                .param("id", id)
                .query(ServicioApoyo::apoyo)
                .single();
    }

    @Transactional(readOnly = true)
    public List<ApoyoVista> apoyos(UsuarioAutenticado usuario, long idHitoEquipo) {
        permisos.exigir(permisos.puedeVerHitoEquipo(usuario.idUsuario(), idHitoEquipo));
        return jdbc.sql(APOYOS + "WHERE t.id_hito_equipo = :he ORDER BY s.fecha_hora DESC, s.id_solicitud_apoyo DESC")
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
        return new ApoyoVista(rs.getLong("id_solicitud_apoyo"), rs.getLong("id_tarea"), rs.getString("titulo"),
                rs.getLong("id_solicitante"), rs.getString("autor"), rs.getString("mensaje"),
                LectorTareas.instante(rs, "fecha_hora"));
    }
}
