package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.eventos.datos.Persona;
import co.uniproject.nucleo.ejecucion.dominio.EstadoHitoEquipo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Lecturas que comparten ejecución (17a, 17b, 18) y evaluación (19). Cada método es UNA consulta (sin N+1). */
@Component
public class ConsultasEjecucion {

    private static final String CONTEXTO = "SELECT he.id_hito_equipo, h.id_hito, h.nombre_hito, h.fecha_inicio, "
            + "h.fecha_limite, he.estado, e.id_equipo, e.nombre_equipo, p.id_proyecto, p.nombre AS nombre_proyecto, "
            + "p.id_docente FROM hito_equipo he JOIN hito h ON h.id_hito = he.id_hito "
            + "JOIN equipo e ON e.id_equipo = he.id_equipo JOIN proyecto p ON p.id_proyecto = h.id_proyecto ";

    private final JdbcClient jdbc;

    public ConsultasEjecucion(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ContextoHito> hito(long idHitoEquipo) {
        return jdbc.sql(CONTEXTO + "WHERE he.id_hito_equipo = :id")
                .param("id", idHitoEquipo)
                .query(ConsultasEjecucion::contexto)
                .optional();
    }

    public Optional<ContextoHito> hitoDeTarea(long idTarea) {
        return jdbc.sql(CONTEXTO + "JOIN tarea t ON t.id_hito_equipo = he.id_hito_equipo WHERE t.id_tarea = :id")
                .param("id", idTarea)
                .query(ConsultasEjecucion::contexto)
                .optional();
    }

    /** id → Persona (nombre y chat de Telegram) para los eventos. Ids sin nulos; repetidos permitidos. */
    public Map<Long, Persona> personas(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql("SELECT id_usuario, nombre, telegram_chat_id FROM usuario WHERE id_usuario IN (:ids)")
                .param("ids", List.copyOf(ids))
                .query((rs, fila) -> new Persona(rs.getLong("id_usuario"), rs.getString("nombre"),
                        rs.getObject("telegram_chat_id", Long.class)))
                .list()
                .stream()
                .collect(Collectors.toMap(Persona::id, Function.identity()));
    }

    /** Miembros del equipo (líder e integrantes), por id. */
    public List<Long> miembros(long idEquipo) {
        return jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :equipo ORDER BY id_usuario")
                .param("equipo", idEquipo)
                .query(Long.class)
                .list();
    }

    /** RF5: hay una solicitud de corrección PENDIENTE en la tarea. */
    public boolean correccionPendiente(long idTarea) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM solicitud_correccion WHERE id_tarea = :tarea "
                        + "AND estado = 'PENDIENTE')")
                .param("tarea", idTarea)
                .query(Boolean.class)
                .single();
    }

    private static ContextoHito contexto(ResultSet rs, int fila) throws SQLException {
        return new ContextoHito(rs.getLong("id_hito_equipo"), rs.getLong("id_hito"), rs.getString("nombre_hito"),
                rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_limite", LocalDate.class),
                EstadoHitoEquipo.valueOf(rs.getString("estado")), rs.getLong("id_equipo"),
                rs.getString("nombre_equipo"), rs.getLong("id_proyecto"), rs.getString("nombre_proyecto"),
                rs.getLong("id_docente"));
    }
}
