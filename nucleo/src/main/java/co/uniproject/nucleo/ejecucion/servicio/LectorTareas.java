package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.ejecucion.dependencias.NivelRiesgo;
import co.uniproject.nucleo.ejecucion.dominio.EstadoTarea;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Vistas de tareas con dos consultas (tareas con su encargado + dependencias), sin N+1. */
@Component
public class LectorTareas {

    private static final String TAREAS = "SELECT t.id_tarea, t.id_hito_equipo, t.titulo, t.descripcion, "
            + "t.id_encargado, u.nombre AS encargado, t.id_tarea_origen, t.estado, t.porcentaje_avance, "
            + "t.horas_estimadas, t.fecha_limite, coalesce(t.fecha_limite, h.fecha_limite) AS fecha_limite_efectiva, "
            + "t.fecha_completada, t.es_prerrequisito_critico, t.bloqueada_por_dependencia, t.fecha_bloqueo, "
            + "t.en_ruta_critica, t.holgura_horas, t.nivel_riesgo FROM tarea t "
            + "JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo JOIN hito h ON h.id_hito = he.id_hito "
            + "LEFT JOIN usuario u ON u.id_usuario = t.id_encargado ";

    /** Solo tareas cuyo encargado sigue en el equipo; primero las pendientes, por fecha límite. */
    private static final String MIAS = "SELECT t.id_tarea, t.titulo, t.estado, t.porcentaje_avance, "
            + "coalesce(t.fecha_limite, h.fecha_limite) AS fecha_limite_efectiva, t.bloqueada_por_dependencia, "
            + "t.nivel_riesgo, he.id_hito_equipo, h.nombre_hito, p.nombre AS proyecto FROM tarea t "
            + "JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo JOIN hito h ON h.id_hito = he.id_hito "
            + "JOIN proyecto p ON p.id_proyecto = h.id_proyecto "
            + "JOIN miembro_equipo m ON m.id_equipo = he.id_equipo AND m.id_usuario = t.id_encargado "
            + "WHERE t.id_encargado = :usuario ORDER BY (t.estado = 'COMPLETADA'), fecha_limite_efectiva, t.id_tarea";

    private static final String AVANCES = "SELECT ra.id_registro, ra.id_usuario, u.nombre, ra.porcentaje_avance, "
            + "ra.observacion, ra.fecha_hora FROM registro_avance ra JOIN usuario u ON u.id_usuario = ra.id_usuario "
            + "WHERE ra.id_tarea = :tarea ORDER BY ra.fecha_hora DESC, ra.id_registro DESC LIMIT 50";

    private final JdbcClient jdbc;

    public LectorTareas(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<TareaVista> delHito(long idHitoEquipo) {
        return armar(jdbc.sql(TAREAS + "WHERE t.id_hito_equipo = :id ORDER BY t.id_tarea").param("id", idHitoEquipo));
    }

    public TareaVista una(long idTarea) {
        List<TareaVista> lista = armar(jdbc.sql(TAREAS + "WHERE t.id_tarea = :id").param("id", idTarea));
        if (lista.isEmpty()) {
            throw new RecursoNoEncontradoException("La tarea no existe");
        }
        return lista.getFirst();
    }

    public List<MiTareaVista> mias(long idUsuario) {
        return jdbc.sql(MIAS).param("usuario", idUsuario)
                .query((rs, fila) -> new MiTareaVista(rs.getLong("id_tarea"), rs.getString("titulo"),
                        EstadoTarea.valueOf(rs.getString("estado")), rs.getInt("porcentaje_avance"),
                        rs.getObject("fecha_limite_efectiva", LocalDate.class),
                        rs.getBoolean("bloqueada_por_dependencia"), nivel(rs.getString("nivel_riesgo")),
                        rs.getLong("id_hito_equipo"), rs.getString("nombre_hito"), rs.getString("proyecto")))
                .list();
    }

    public List<AvanceVista> avances(long idTarea) {
        return jdbc.sql(AVANCES).param("tarea", idTarea)
                .query((rs, fila) -> new AvanceVista(rs.getLong("id_registro"), rs.getLong("id_usuario"),
                        rs.getString("nombre"), rs.getInt("porcentaje_avance"), rs.getString("observacion"),
                        instante(rs, "fecha_hora")))
                .list();
    }

    /** timestamptz → Instant (null si la columna es nula). Lo reutilizan 17b, 18 y 19. */
    public static Instant instante(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }

    private List<TareaVista> armar(JdbcClient.StatementSpec consulta) {
        List<TareaVista> filas = consulta.query(LectorTareas::fila).list();
        if (filas.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Long>> previas = new HashMap<>();
        jdbc.sql("SELECT id_tarea_predecesora, id_tarea_dependiente FROM dependencia_tarea "
                        + "WHERE id_tarea_dependiente IN (:ids) ORDER BY id_tarea_predecesora")
                .param("ids", filas.stream().map(TareaVista::idTarea).toList())
                .query((rs, fila) -> new long[] {rs.getLong(1), rs.getLong(2)})
                .list()
                .forEach(par -> previas.computeIfAbsent(par[1], k -> new ArrayList<>()).add(par[0]));
        return filas.stream()
                .map(t -> t.conPrerrequisitos(previas.getOrDefault(t.idTarea(), List.of())))
                .toList();
    }

    private static TareaVista fila(ResultSet rs, int fila) throws SQLException {
        return new TareaVista(rs.getLong("id_tarea"), rs.getLong("id_hito_equipo"), rs.getString("titulo"),
                rs.getString("descripcion"), rs.getObject("id_encargado", Long.class), rs.getString("encargado"),
                rs.getObject("id_tarea_origen", Long.class), EstadoTarea.valueOf(rs.getString("estado")),
                rs.getInt("porcentaje_avance"), rs.getBigDecimal("horas_estimadas"),
                rs.getObject("fecha_limite", LocalDate.class), rs.getObject("fecha_limite_efectiva", LocalDate.class),
                instante(rs, "fecha_completada"), rs.getBoolean("es_prerrequisito_critico"),
                rs.getBoolean("bloqueada_por_dependencia"), instante(rs, "fecha_bloqueo"),
                rs.getBoolean("en_ruta_critica"), rs.getBigDecimal("holgura_horas"),
                nivel(rs.getString("nivel_riesgo")), List.of());
    }

    private static NivelRiesgo nivel(String valor) {
        return valor == null ? null : NivelRiesgo.valueOf(valor);
    }
}
