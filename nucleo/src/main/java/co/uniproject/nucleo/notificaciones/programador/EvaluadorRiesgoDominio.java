package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.eventos.datos.ReferenciaTarea;
import co.uniproject.eventos.datos.TareaRutaCriticaRiesgo;
import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.ejecucion.dependencias.MotorDependencias;
import co.uniproject.nucleo.ejecucion.dependencias.NivelRiesgo;
import co.uniproject.nucleo.ejecucion.dependencias.RiesgoTarea;
import co.uniproject.nucleo.ejecucion.dependencias.TareaPlan;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * RF9 con datos reales (decisiones 20-3 y 20-4). Por cada hito de equipo EN_CURSO ya iniciado corre MotorDependencias.
 * En riesgo = ruta crítica + nivel ALTO + con encargado + equipo con miembros. Las demás no completadas vuelven con
 * enRiesgo=false para que RevisionRiesgo baje su estado. Un hito que falla no detiene a los demás.
 */
@Component
public class EvaluadorRiesgoDominio implements EvaluadorRiesgo {

    private static final Logger log = LoggerFactory.getLogger(EvaluadorRiesgoDominio.class);

    private static final String HITOS_EN_CURSO = "SELECT he.id_hito_equipo, p.id_proyecto, p.nombre AS proyecto, "
            + "e.id_equipo, e.nombre_equipo, h.nombre_hito, h.fecha_limite, d.id_usuario AS id_docente, "
            + "d.nombre AS docente, d.telegram_chat_id AS chat_docente FROM hito_equipo he "
            + "JOIN hito h ON h.id_hito = he.id_hito JOIN equipo e ON e.id_equipo = he.id_equipo "
            + "JOIN proyecto p ON p.id_proyecto = h.id_proyecto JOIN usuario d ON d.id_usuario = p.id_docente "
            + "WHERE he.estado = 'EN_CURSO' AND h.fecha_inicio <= :hoy ORDER BY he.id_hito_equipo";

    private static final String TAREAS = "SELECT t.id_tarea, t.titulo, t.id_encargado, u.nombre AS encargado, "
            + "t.horas_estimadas, t.porcentaje_avance, t.fecha_limite FROM tarea t "
            + "LEFT JOIN usuario u ON u.id_usuario = t.id_encargado WHERE t.id_hito_equipo = :hitoEquipo "
            + "ORDER BY t.id_tarea";

    /** Solo dependencias entre tareas del mismo hito de equipo (decisión 20-3). */
    private static final String DEPENDENCIAS = "SELECT d.id_tarea_predecesora, d.id_tarea_dependiente "
            + "FROM dependencia_tarea d JOIN tarea a ON a.id_tarea = d.id_tarea_predecesora "
            + "JOIN tarea b ON b.id_tarea = d.id_tarea_dependiente "
            + "WHERE a.id_hito_equipo = :hitoEquipo AND b.id_hito_equipo = :hitoEquipo";

    private static final String INTEGRANTES = "SELECT u.id_usuario, u.nombre, u.telegram_chat_id FROM miembro_equipo m "
            + "JOIN usuario u ON u.id_usuario = m.id_usuario WHERE m.id_equipo = :equipo ORDER BY u.id_usuario";

    private record HitoEnCurso(long idHitoEquipo, long idProyecto, String proyecto, long idEquipo, String equipo,
                               String hito, LocalDate fechaLimite, Persona docente) {}

    private record FilaTarea(long id, String titulo, Long idEncargado, String encargado, BigDecimal horas, int avance,
                             LocalDate fechaLimite) {}

    private final JdbcClient jdbc;
    private final Clock reloj;

    public EvaluadorRiesgoDominio(JdbcClient jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Override
    public List<EvaluacionRiesgo> evaluarTareasRutaCritica() {
        Instant ahora = Instant.now(reloj);
        List<HitoEnCurso> hitos = jdbc.sql(HITOS_EN_CURSO)
                .param("hoy", FechasDominio.hoy(ahora))
                .query((rs, fila) -> new HitoEnCurso(rs.getLong("id_hito_equipo"), rs.getLong("id_proyecto"),
                        rs.getString("proyecto"), rs.getLong("id_equipo"), rs.getString("nombre_equipo"),
                        rs.getString("nombre_hito"), rs.getObject("fecha_limite", LocalDate.class),
                        new Persona(rs.getLong("id_docente"), rs.getString("docente"),
                                rs.getObject("chat_docente", Long.class))))
                .list();
        List<EvaluacionRiesgo> resultado = new ArrayList<>();
        for (HitoEnCurso hito : hitos) {
            try {
                resultado.addAll(evaluar(hito, ahora));
            } catch (RuntimeException e) {
                log.error("No se pudo evaluar el riesgo del hito de equipo {}", hito.idHitoEquipo(), e);
            }
        }
        return resultado;
    }

    private List<EvaluacionRiesgo> evaluar(HitoEnCurso hito, Instant ahora) {
        List<FilaTarea> tareas = jdbc.sql(TAREAS)
                .param("hitoEquipo", hito.idHitoEquipo())
                .query((rs, fila) -> new FilaTarea(rs.getLong("id_tarea"), rs.getString("titulo"),
                        rs.getObject("id_encargado", Long.class), rs.getString("encargado"),
                        rs.getBigDecimal("horas_estimadas"), rs.getInt("porcentaje_avance"),
                        rs.getObject("fecha_limite", LocalDate.class)))
                .list();
        if (tareas.isEmpty()) {
            return List.of();
        }
        Map<Long, Set<Long>> prerrequisitos = new HashMap<>();
        jdbc.sql(DEPENDENCIAS)
                .param("hitoEquipo", hito.idHitoEquipo())
                .query((rs, fila) -> new long[] {rs.getLong(1), rs.getLong(2)})
                .list()
                .forEach(a -> prerrequisitos.computeIfAbsent(a[1], k -> new HashSet<>()).add(a[0]));
        List<TareaPlan> planes = new ArrayList<>();
        Map<Long, Instant> limites = new HashMap<>();
        for (FilaTarea t : tareas) {
            planes.add(new TareaPlan(t.id(), duracion(t.horas()), t.avance(),
                    prerrequisitos.getOrDefault(t.id(), Set.of())));
            limites.put(t.id(), FechasDominio.venceEn(t.fechaLimite() != null ? t.fechaLimite() : hito.fechaLimite()));
        }
        Set<Long> criticas = new HashSet<>(MotorDependencias.rutaCritica(planes).criticas());
        Map<Long, NivelRiesgo> niveles = new HashMap<>();
        for (RiesgoTarea r : MotorDependencias.evaluarRiesgo(planes, ahora, limites)) {
            niveles.put(r.tareaId(), r.nivel());
        }
        List<Persona> integrantes = jdbc.sql(INTEGRANTES)
                .param("equipo", hito.idEquipo())
                .query((rs, fila) -> new Persona(rs.getLong("id_usuario"), rs.getString("nombre"),
                        rs.getObject("telegram_chat_id", Long.class)))
                .list();
        List<EvaluacionRiesgo> resultado = new ArrayList<>();
        for (FilaTarea t : tareas) {
            if (t.avance() == 100) {
                continue;
            }
            boolean enRiesgo = criticas.contains(t.id()) && niveles.get(t.id()) == NivelRiesgo.ALTO
                    && t.idEncargado() != null && !integrantes.isEmpty();
            resultado.add(enRiesgo
                    ? new EvaluacionRiesgo(t.id(), true, datos(hito, t, integrantes))
                    : new EvaluacionRiesgo(t.id(), false, null));
        }
        return resultado;
    }

    private static TareaRutaCriticaRiesgo datos(HitoEnCurso hito, FilaTarea t, List<Persona> integrantes) {
        return new TareaRutaCriticaRiesgo(new Referencia(hito.idProyecto(), hito.proyecto()),
                new Referencia(hito.idEquipo(), hito.equipo()), new ReferenciaTarea(t.id(), t.titulo()),
                new Referencia(t.idEncargado(), t.encargado()), t.avance(),
                new Referencia(hito.idHitoEquipo(), hito.hito()), hito.fechaLimite(), integrantes, hito.docente());
    }

    /** horas_estimadas numeric(6,2) → segundos exactos (1.25 h = 4500 s). */
    static Duration duracion(BigDecimal horas) {
        return Duration.ofSeconds(horas.multiply(BigDecimal.valueOf(3600)).longValueExact());
    }
}
