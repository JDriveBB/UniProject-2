package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.eventos.Topologia;
import co.uniproject.eventos.datos.EquipoInactividad;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.nucleo.comun.FechasDominio;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * RF6 con datos reales (decisiones 20-5 y 20-6). Una consulta trae a cada candidato con su última actividad; aquí se
 * filtra por el periodo de su hito. RevisionInactividad decide si ya se le avisó. Los hitos sin alerta
 * (dias_inactividad_alerta NULL, bloque 30) no se revisan; el panel del docente usa este mismo detector.
 */
@Component
public class DetectorInactividadDominio implements DetectorInactividad {

    private static final String CANDIDATOS = "SELECT DISTINCT ON (p.id_proyecto, m.id_usuario) "
            + "p.id_proyecto, p.nombre AS proyecto, e.id_equipo, e.nombre_equipo, m.id_usuario AS id_estudiante, "
            + "u.nombre AS estudiante, h.dias_inactividad_alerta, h.notificar_docente_inactividad, "
            + "l.id_usuario AS id_lider, lu.nombre AS lider, lu.telegram_chat_id AS chat_lider, "
            + "d.id_usuario AS id_docente, d.nombre AS docente, d.telegram_chat_id AS chat_docente, "
            + "GREATEST(m.fecha_ingreso, h.fecha_inicio::timestamp AT TIME ZONE '" + Topologia.ZONA + "', "
            + "(SELECT max(ra.fecha_hora) FROM registro_avance ra JOIN tarea t ON t.id_tarea = ra.id_tarea "
            + "JOIN hito_equipo x ON x.id_hito_equipo = t.id_hito_equipo "
            + "WHERE x.id_equipo = e.id_equipo AND ra.id_usuario = m.id_usuario), "
            + "(SELECT max(ev.fecha_subida) FROM evidencia ev JOIN tarea t ON t.id_tarea = ev.id_tarea "
            + "JOIN hito_equipo x ON x.id_hito_equipo = t.id_hito_equipo "
            + "WHERE x.id_equipo = e.id_equipo AND ev.id_usuario = m.id_usuario)) AS ultima_actividad "
            + "FROM hito_equipo he JOIN hito h ON h.id_hito = he.id_hito "
            + "JOIN equipo e ON e.id_equipo = he.id_equipo JOIN proyecto p ON p.id_proyecto = h.id_proyecto "
            + "JOIN usuario d ON d.id_usuario = p.id_docente "
            + "JOIN miembro_equipo m ON m.id_equipo = e.id_equipo JOIN usuario u ON u.id_usuario = m.id_usuario "
            + "JOIN miembro_equipo l ON l.id_equipo = e.id_equipo AND l.rol_equipo = 'LIDER' "
            + "JOIN usuario lu ON lu.id_usuario = l.id_usuario "
            + "WHERE he.estado = 'EN_CURSO' AND h.dias_inactividad_alerta IS NOT NULL AND h.fecha_inicio <= :hoy AND h.fecha_limite >= :hoy "
            + "AND EXISTS (SELECT 1 FROM tarea t WHERE t.id_hito_equipo = he.id_hito_equipo "
            + "AND t.id_encargado = m.id_usuario AND t.estado <> 'COMPLETADA') "
            + "ORDER BY p.id_proyecto, m.id_usuario, h.fecha_limite, he.id_hito_equipo";

    private final JdbcClient jdbc;

    public DetectorInactividadDominio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CasoInactividad> buscarInactivos(Instant ahora) {
        return jdbc.sql(CANDIDATOS)
                .param("hoy", FechasDominio.hoy(ahora))
                .query((rs, fila) -> {
                    Instant ultima = rs.getObject("ultima_actividad", OffsetDateTime.class).toInstant();
                    EquipoInactividad datos = new EquipoInactividad(
                            new Referencia(rs.getLong("id_proyecto"), rs.getString("proyecto")),
                            new Referencia(rs.getLong("id_equipo"), rs.getString("nombre_equipo")),
                            new Referencia(rs.getLong("id_estudiante"), rs.getString("estudiante")),
                            ultima,
                            rs.getInt("dias_inactividad_alerta"),
                            new Persona(rs.getLong("id_lider"), rs.getString("lider"),
                                    rs.getObject("chat_lider", Long.class)),
                            new Persona(rs.getLong("id_docente"), rs.getString("docente"),
                                    rs.getObject("chat_docente", Long.class)),
                            rs.getBoolean("notificar_docente_inactividad"));
                    return new CasoInactividad(rs.getLong("id_proyecto"), rs.getLong("id_estudiante"), ultima, datos);
                })
                .list()
                .stream()
                .filter(c -> !c.ultimaActividad().plus(Duration.ofDays(c.datos().periodoDias())).isAfter(ahora))
                .toList();
    }
}
