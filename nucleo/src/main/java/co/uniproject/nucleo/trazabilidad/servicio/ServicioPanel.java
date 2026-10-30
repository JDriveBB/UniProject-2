package co.uniproject.nucleo.trazabilidad.servicio;

import co.uniproject.nucleo.notificaciones.programador.CasoInactividad;
import co.uniproject.nucleo.notificaciones.programador.DetectorInactividad;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF10: panel del docente. Tres consultas fijas (decisión 21-2). */
@Service
public class ServicioPanel {

    static final Duration BLOQUEO_MAXIMO = Duration.ofHours(48);

    private static final String EQUIPOS = "SELECT e.id_equipo, e.nombre_equipo, count(t.id_tarea) AS total, "
            + "count(t.id_tarea) FILTER (WHERE t.estado = 'COMPLETADA') AS completadas, "
            + "count(t.id_tarea) FILTER (WHERE t.nivel_riesgo = 'ALTO' AND t.estado <> 'COMPLETADA') AS riesgo_alto, "
            + "coalesce(round(avg(t.porcentaje_avance)), 0) AS avance FROM equipo e "
            + "LEFT JOIN hito_equipo he ON he.id_equipo = e.id_equipo "
            + "LEFT JOIN tarea t ON t.id_hito_equipo = he.id_hito_equipo "
            + "WHERE e.id_proyecto = :proyecto GROUP BY e.id_equipo, e.nombre_equipo "
            + "ORDER BY e.nombre_equipo, e.id_equipo";

    private static final String BLOQUEADAS = "SELECT he.id_equipo, t.id_tarea, t.titulo, t.fecha_bloqueo FROM tarea t "
            + "JOIN hito_equipo he ON he.id_hito_equipo = t.id_hito_equipo JOIN equipo e ON e.id_equipo = he.id_equipo "
            + "WHERE e.id_proyecto = :proyecto AND t.bloqueada_por_dependencia AND t.estado <> 'COMPLETADA' "
            + "AND t.fecha_bloqueo <= :limite ORDER BY t.fecha_bloqueo, t.id_tarea";

    private final Permisos permisos;
    private final DetectorInactividad detector;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioPanel(Permisos permisos, DetectorInactividad detector, JdbcClient jdbc, Clock reloj) {
        this.permisos = permisos;
        this.detector = detector;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PanelProyecto panel(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        Instant ahora = Instant.now(reloj);
        String nombre = jdbc.sql("SELECT nombre FROM proyecto WHERE id_proyecto = :proyecto")
                .param("proyecto", idProyecto)
                .query(String.class)
                .single();
        Map<Long, List<TareaBloqueada>> bloqueadas = new HashMap<>();
        jdbc.sql(BLOQUEADAS)
                .param("proyecto", idProyecto)
                .param("limite", OffsetDateTime.ofInstant(ahora.minus(BLOQUEO_MAXIMO), ZoneOffset.UTC))
                .query((rs, fila) -> {
                    Instant desde = rs.getObject("fecha_bloqueo", OffsetDateTime.class).toInstant();
                    return Map.entry(rs.getLong("id_equipo"), new TareaBloqueada(rs.getLong("id_tarea"),
                            rs.getString("titulo"), desde, Duration.between(desde, ahora).toHours()));
                })
                .list()
                .forEach(par -> bloqueadas.computeIfAbsent(par.getKey(), k -> new ArrayList<>()).add(par.getValue()));
        Map<Long, List<IntegranteInactivo>> inactivos = new HashMap<>();
        for (CasoInactividad c : detector.buscarInactivos(ahora)) {
            if (c.proyectoId() == idProyecto) {
                inactivos.computeIfAbsent(c.datos().equipo().id(), k -> new ArrayList<>())
                        .add(new IntegranteInactivo(c.estudianteId(), c.datos().estudiante().nombre(),
                                c.ultimaActividad(), Duration.between(c.ultimaActividad(), ahora).toDays()));
            }
        }
        List<PanelEquipo> equipos = jdbc.sql(EQUIPOS)
                .param("proyecto", idProyecto)
                .query((rs, fila) -> {
                    long id = rs.getLong("id_equipo");
                    List<IntegranteInactivo> ina = inactivos.getOrDefault(id, List.of());
                    List<TareaBloqueada> blo = bloqueadas.getOrDefault(id, List.of());
                    return new PanelEquipo(id, rs.getString("nombre_equipo"), !ina.isEmpty() || !blo.isEmpty(),
                            rs.getInt("total"), rs.getInt("completadas"), rs.getInt("riesgo_alto"),
                            rs.getInt("avance"), List.copyOf(ina), List.copyOf(blo));
                })
                .list();
        return new PanelProyecto(idProyecto, nombre, ahora, equipos);
    }
}
