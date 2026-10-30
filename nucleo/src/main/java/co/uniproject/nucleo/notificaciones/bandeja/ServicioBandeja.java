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
        List<NotificacionVista> lista = jdbc.sql("SELECT id_notificacion, tipo, mensaje, entidad_referencia, "
                        + "id_referencia, fecha_creacion, estado_envio, leida FROM notificacion "
                        + "WHERE id_destinatario = :usuario ORDER BY fecha_creacion DESC, id_notificacion DESC LIMIT "
                        + MAXIMO)
                .param("usuario", idUsuario)
                .query((rs, fila) -> {
                    String tipo = rs.getString("tipo");
                    String entidad = rs.getString("entidad_referencia");
                    long id = rs.getLong("id_referencia");
                    return new NotificacionVista(rs.getLong("id_notificacion"), tipo, rs.getString("mensaje"), entidad,
                            id, enlace(tipo, entidad, id), rs.getObject("fecha_creacion", OffsetDateTime.class).toInstant(),
                            EstadoEnvioNotificacion.valueOf(rs.getString("estado_envio")), rs.getBoolean("leida"));
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

    /** Misma ruta del SPA a la que lleva el aviso de Telegram (RutasWeb). */
    static String enlace(String tipo, String entidad, long id) {
        return switch (entidad) {
            case "tarea" -> RutasWeb.tarea(id);
            case "equipo" -> RutasWeb.tableroEquipo(id);
            default -> "hito.enviado".equals(tipo) ? RutasWeb.evaluarHito(id) : RutasWeb.evaluacionHito(id);
        };
    }
}
