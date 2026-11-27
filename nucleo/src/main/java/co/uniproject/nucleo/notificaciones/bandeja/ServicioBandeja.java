package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.eventos.RutasWeb;
import co.uniproject.nucleo.comun.error.AccesoDenegadoException;
import co.uniproject.nucleo.notificaciones.dominio.EstadoEnvioNotificacion;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bandeja P10: las 50 notificaciones más recientes del usuario de la sesión. Nadie ve ni marca las de otro. */
@Service
public class ServicioBandeja {

    static final int MAXIMO = 50;

    private final JdbcClient jdbc;

    public ServicioBandeja(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Bandeja bandeja(long idUsuario) {
        List<NotificacionVista> lista = jdbc.sql("SELECT n.id_notificacion, n.tipo, n.mensaje, n.entidad_referencia, "
                        + "n.id_referencia, n.fecha_creacion, n.estado_envio, n.leida, t.id_encargado "
                        + "FROM notificacion n LEFT JOIN tarea t ON n.tipo = 'tarea.reasignada' "
                        + "AND n.entidad_referencia = 'tarea' AND t.id_tarea = n.id_referencia "
                        + "WHERE n.id_destinatario = :usuario ORDER BY n.fecha_creacion DESC, n.id_notificacion DESC LIMIT "
                        + MAXIMO)
                .param("usuario", idUsuario)
                .query((rs, fila) -> {
                    String tipo = rs.getString("tipo");
                    String entidad = rs.getString("entidad_referencia");
                    long id = rs.getLong("id_referencia");
                    long encargado = rs.getLong("id_encargado");
                    boolean esEncargadoActual = !rs.wasNull() && encargado == idUsuario;
                    return new NotificacionVista(
                            rs.getLong("id_notificacion"),
                            tipo,
                            rs.getString("mensaje"),
                            entidad,
                            id,
                            enlace(tipo, entidad, id, esEncargadoActual),
                            rs.getObject("fecha_creacion", OffsetDateTime.class).toInstant(),
                            EstadoEnvioNotificacion.valueOf(rs.getString("estado_envio")),
                            rs.getBoolean("leida"));
                })
                .list();
        long noLeidas = jdbc.sql("SELECT count(*) FROM notificacion WHERE id_destinatario = :usuario AND NOT leida")
                .param("usuario", idUsuario)
                .query(Long.class)
                .single();
        return new Bandeja(noLeidas, lista);
    }

    /** Decisión 16-4: 403 (auditado por ManejadorErrores) si no existe o es de otra persona. */
    @Transactional
    public void marcarLeida(long idUsuario, long idNotificacion) {
        int filas = jdbc.sql("UPDATE notificacion SET leida = true WHERE id_notificacion = :id "
                        + "AND id_destinatario = :usuario")
                .param("id", idNotificacion)
                .param("usuario", idUsuario)
                .update();
        if (filas == 0) {
            throw new AccesoDenegadoException();
        }
    }

    @Transactional
    public int marcarTodasLeidas(long idUsuario) {
        return jdbc.sql("UPDATE notificacion SET leida = true WHERE id_destinatario = :usuario AND NOT leida")
                .param("usuario", idUsuario)
                .update();
    }

    /**
     * Enlace web del aviso: tarea.reasignada al encargado anterior va a /mis-tareas; votación, líder y sin líder a
     * /equipos/{id}/votacion; inscripción por cerrar a /proyectos/{id}/inscripcion; curso a /mis-cursos; proyecto a
     * /proyectos/{id}; equipo al tablero; tarea a /tareas/{id} y hito_equipo a evaluar o evaluación.
     */
    static String enlace(String tipo, String entidad, long id, boolean esEncargadoActual) {
        if ("tarea.reasignada".equals(tipo) && !esEncargadoActual) {
            return RutasWeb.misTareas();
        }
        return switch (tipo) {
            case "equipo.votacion_abierta", "equipo.lider_elegido", "equipo.sin_lider" -> RutasWeb.votacionEquipo(id);
            case "equipo.inscripcion_por_cerrar" -> RutasWeb.inscripcionProyecto(id);
            default ->
                switch (entidad) {
                    case "tarea" -> RutasWeb.tarea(id);
                    case "equipo" -> RutasWeb.tableroEquipo(id);
                    case "curso" -> RutasWeb.misCursos();
                    case "proyecto" -> RutasWeb.proyecto(id);
                    default -> "hito.enviado".equals(tipo) ? RutasWeb.evaluarHito(id) : RutasWeb.evaluacionHito(id);
                };
        };
    }
}
