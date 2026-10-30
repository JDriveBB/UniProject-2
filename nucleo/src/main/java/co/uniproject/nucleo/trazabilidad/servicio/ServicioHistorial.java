package co.uniproject.nucleo.trazabilidad.servicio;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Restricción 4e y RF15: historial inmutable del equipo (decisión 21-3). Una consulta. */
@Service
public class ServicioHistorial {

    private static final String DEL_EQUIPO = "SELECT l.id_log, l.fecha_hora, l.id_usuario_accion, "
            + "coalesce(u.nombre, 'Sistema') AS autor, l.accion, l.entidad_afectada, l.id_registro_afectado, "
            + "l.campo_modificado, l.valor_anterior, l.valor_nuevo "
            + "FROM log_auditoria l LEFT JOIN usuario u ON u.id_usuario = l.id_usuario_accion "
            + "WHERE (l.entidad_afectada IN ('equipo', 'miembro_equipo') AND l.id_registro_afectado = :equipo) "
            + "OR (l.entidad_afectada = 'hito_equipo' AND l.id_registro_afectado IN "
            + "(SELECT id_hito_equipo FROM hito_equipo WHERE id_equipo = :equipo)) "
            + "OR (l.entidad_afectada = 'tarea' AND l.id_registro_afectado IN (SELECT t.id_tarea FROM tarea t "
            + "JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo WHERE he.id_equipo = :equipo)) ";

    private final Permisos permisos;
    private final JdbcClient jdbc;

    public ServicioHistorial(Permisos permisos, JdbcClient jdbc) {
        this.permisos = permisos;
        this.jdbc = jdbc;
    }

    /** Más reciente primero; limite se ajusta a 1..1000. */
    @Transactional(readOnly = true)
    public List<EntradaHistorial> consultar(UsuarioAutenticado usuario, long idEquipo, int limite) {
        permisos.exigir(permisos.puedeVerEquipo(usuario.idUsuario(), idEquipo));
        return entradas(idEquipo, false, Math.clamp(limite, 1, 1000));
    }

    /** Sin permisos: lo llama ServicioReporte, que ya los exigió. */
    List<EntradaHistorial> entradas(long idEquipo, boolean ascendente, int tope) {
        String orden = ascendente ? "ORDER BY l.fecha_hora, l.id_log " : "ORDER BY l.fecha_hora DESC, l.id_log DESC ";
        return jdbc.sql(DEL_EQUIPO + orden + "LIMIT :tope")
                .param("equipo", idEquipo)
                .param("tope", tope)
                .query(ServicioHistorial::fila)
                .list();
    }

    /** Decisión 21-5: nunca nulo. Sin campo: el valor nuevo o "". Con campo: "campo: anterior -> nuevo" ("-" si falta). */
    static String detalle(String campo, String anterior, String nuevo) {
        if (campo == null) {
            return nuevo == null ? "" : nuevo;
        }
        return campo + ": " + (anterior == null ? "-" : anterior) + " -> " + (nuevo == null ? "-" : nuevo);
    }

    private static EntradaHistorial fila(ResultSet rs, int fila) throws SQLException {
        String campo = rs.getString("campo_modificado");
        String anterior = rs.getString("valor_anterior");
        String nuevo = rs.getString("valor_nuevo");
        return new EntradaHistorial(rs.getLong("id_log"), rs.getObject("fecha_hora", OffsetDateTime.class).toInstant(),
                rs.getObject("id_usuario_accion", Long.class), rs.getString("autor"), rs.getString("accion"),
                rs.getString("entidad_afectada"), rs.getObject("id_registro_afectado", Long.class), campo, anterior,
                nuevo, detalle(campo, anterior, nuevo));
    }
}
