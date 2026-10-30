package co.uniproject.nucleo.evaluacion.servicio;

import co.uniproject.nucleo.ejecucion.servicio.LectorTareas;
import co.uniproject.nucleo.evaluacion.calificacion.CalculadoraIce;
import co.uniproject.nucleo.evaluacion.calificacion.CaracterEvaluacion;
import co.uniproject.nucleo.evaluacion.calificacion.CriterioCalificado;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Lecturas de evaluaciones: tres consultas por vista, sin N+1. */
@Component
public class LectorEvaluaciones {

    private static final String CABECERA = "SELECT ev.id_evaluacion, en.id_hito_equipo, ev.id_envio, en.fecha_envio, "
            + "ev.fecha_evaluacion, ev.nota_grupal, ev.retroalimentacion_general, ev.es_definitiva, h.fecha_limite "
            + "FROM evaluacion_hito ev JOIN envio_hito en ON en.id_envio = ev.id_envio "
            + "JOIN hito_equipo he ON he.id_hito_equipo = en.id_hito_equipo JOIN hito h ON h.id_hito = he.id_hito "
            + "WHERE ev.id_evaluacion = :ev";

    private static final String CRITERIOS = "SELECT cc.id_criterio, cr.descripcion, cr.peso_porcentaje, "
            + "cr.puntaje_maximo, cc.nota, cc.retroalimentacion FROM calificacion_criterio cc "
            + "JOIN criterio_rubrica cr ON cr.id_criterio = cc.id_criterio WHERE cc.id_evaluacion = :ev "
            + "ORDER BY cc.id_criterio";

    private static final String NOTAS = "SELECT ni.id_estudiante, u.nombre, ni.factor_ajuste, ni.nota_sugerida, "
            + "ni.nota_final, ni.fecha_confirmacion FROM nota_individual ni "
            + "JOIN usuario u ON u.id_usuario = ni.id_estudiante WHERE ni.id_evaluacion = :ev "
            + "ORDER BY u.nombre, ni.id_estudiante";

    private record Cabecera(long idEvaluacion, long idHitoEquipo, long idEnvio, Instant fechaEnvio,
                            Instant fechaEvaluacion, BigDecimal notaGrupal, String retroalimentacion,
                            boolean esDefinitiva, LocalDate fechaLimite) {}

    private final JdbcClient jdbc;

    public LectorEvaluaciones(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** La evaluación más reciente del hito de equipo. */
    public Optional<Long> ultima(long idHitoEquipo) {
        return jdbc.sql("SELECT ev.id_evaluacion FROM evaluacion_hito ev JOIN envio_hito en ON en.id_envio = ev.id_envio "
                        + "WHERE en.id_hito_equipo = :he ORDER BY ev.fecha_evaluacion DESC, ev.id_evaluacion DESC LIMIT 1")
                .param("he", idHitoEquipo)
                .query(Long.class)
                .optional();
    }

    public Optional<Long> hitoEquipoDe(long idEvaluacion) {
        return jdbc.sql("SELECT en.id_hito_equipo FROM evaluacion_hito ev JOIN envio_hito en ON en.id_envio = ev.id_envio "
                        + "WHERE ev.id_evaluacion = :ev")
                .param("ev", idEvaluacion)
                .query(Long.class)
                .optional();
    }

    /**
     * completa = docente del proyecto: todo. Si no, solo la fila de idUsuario sin ICE ni sugerida (estudiante) o
     * ninguna (monitor), y sin notaCalculada (decisión 19-9).
     */
    public EvaluacionVista leer(long idEvaluacion, long idUsuario, boolean completa, Instant ahora) {
        Cabecera c = jdbc.sql(CABECERA).param("ev", idEvaluacion)
                .query((rs, fila) -> new Cabecera(rs.getLong("id_evaluacion"), rs.getLong("id_hito_equipo"),
                        rs.getLong("id_envio"), LectorTareas.instante(rs, "fecha_envio"),
                        LectorTareas.instante(rs, "fecha_evaluacion"), rs.getBigDecimal("nota_grupal"),
                        rs.getString("retroalimentacion_general"), rs.getBoolean("es_definitiva"),
                        rs.getObject("fecha_limite", LocalDate.class)))
                .single();
        List<CriterioEvaluadoVista> criterios = jdbc.sql(CRITERIOS).param("ev", idEvaluacion)
                .query((rs, fila) -> new CriterioEvaluadoVista(rs.getLong("id_criterio"), rs.getString("descripcion"),
                        rs.getBigDecimal("peso_porcentaje"), rs.getBigDecimal("puntaje_maximo"),
                        rs.getBigDecimal("nota"), rs.getString("retroalimentacion")))
                .list();
        List<NotaIndividualVista> notas = jdbc.sql(NOTAS).param("ev", idEvaluacion)
                .query((rs, fila) -> new NotaIndividualVista(rs.getLong("id_estudiante"), rs.getString("nombre"),
                        rs.getBigDecimal("factor_ajuste"), rs.getBigDecimal("nota_sugerida"),
                        rs.getBigDecimal("nota_final"), LectorTareas.instante(rs, "fecha_confirmacion")))
                .list()
                .stream()
                .filter(n -> completa || n.idEstudiante() == idUsuario)
                .map(n -> completa ? n : n.soloFinal())
                .toList();
        return new EvaluacionVista(c.idEvaluacion(), c.idHitoEquipo(), c.idEnvio(), c.fechaEnvio(),
                c.fechaEvaluacion(), c.notaGrupal(), completa ? calculada(criterios) : null, c.retroalimentacion(),
                CaracterEvaluacion.de(c.esDefinitiva(), c.fechaLimite(), ahora), criterios, notas);
    }

    /** La nota de la rúbrica tal como quedó calificada; null si la rúbrica se editó después y ya no cuadra. */
    static BigDecimal calculada(List<CriterioEvaluadoVista> criterios) {
        try {
            return CalculadoraIce.notaGrupal(criterios.stream()
                    .map(c -> new CriterioCalificado(c.nota(), c.puntajeMaximo(), c.pesoPorcentaje()))
                    .toList());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
