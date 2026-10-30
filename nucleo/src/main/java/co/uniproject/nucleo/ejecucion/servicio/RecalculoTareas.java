package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.ejecucion.dependencias.MotorDependencias;
import co.uniproject.nucleo.ejecucion.dependencias.RiesgoTarea;
import co.uniproject.nucleo.ejecucion.dependencias.TareaPlan;
import co.uniproject.nucleo.ejecucion.dominio.DependenciaTarea;
import co.uniproject.nucleo.ejecucion.dominio.Tarea;
import co.uniproject.nucleo.ejecucion.repositorio.DependenciaTareaRepositorio;
import co.uniproject.nucleo.ejecucion.repositorio.TareaRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Decisión 17a-3: deja en las tareas del hito de equipo el último cálculo de MotorDependencias (ruta crítica, holgura,
 * riesgo y bloqueo). Llamar dentro de la transacción del cambio, DESPUÉS de guardar tareas o dependencias.
 */
@Component
public class RecalculoTareas {

    private final TareaRepositorio tareas;
    private final DependenciaTareaRepositorio dependencias;
    private final ConsultasEjecucion consultas;
    private final Clock reloj;

    public RecalculoTareas(TareaRepositorio tareas, DependenciaTareaRepositorio dependencias,
                           ConsultasEjecucion consultas, Clock reloj) {
        this.tareas = tareas;
        this.dependencias = dependencias;
        this.consultas = consultas;
        this.reloj = reloj;
    }

    public void recalcular(long idHitoEquipo) {
        ContextoHito hito = consultas.hito(idHitoEquipo).orElseThrow();
        List<Tarea> lista = tareas.findByIdHitoEquipo(idHitoEquipo);
        if (lista.isEmpty()) {
            return;
        }
        Map<Long, Set<Long>> previas = prerrequisitos(lista);
        Instant ahora = Instant.now(reloj);
        List<TareaPlan> planes = new ArrayList<>(lista.size());
        Map<Long, Instant> limites = new HashMap<>();
        for (Tarea t : lista) {
            planes.add(plan(t, previas));
            limites.put(t.getIdTarea(), FechasDominio.venceEn(
                    t.getFechaLimite() != null ? t.getFechaLimite() : hito.fechaLimite()));
        }
        Set<Long> criticas = new HashSet<>(MotorDependencias.rutaCritica(planes).criticas());
        Map<Long, RiesgoTarea> riesgos = MotorDependencias.evaluarRiesgo(planes, ahora, limites).stream()
                .collect(Collectors.toMap(RiesgoTarea::tareaId, Function.identity()));
        Set<Long> bloqueadas = MotorDependencias.bloqueadas(planes);
        for (Tarea t : lista) {
            long id = t.getIdTarea();
            t.setEnRutaCritica(criticas.contains(id));
            t.setHolguraHoras(horas(riesgos.get(id).holgura()));
            t.setNivelRiesgo(riesgos.get(id).nivel());
            boolean bloqueada = bloqueadas.contains(id);
            boolean estaba = Boolean.TRUE.equals(t.getBloqueadaPorDependencia());
            if (bloqueada && !estaba) {
                t.setBloqueadaPorDependencia(true);
                t.setFechaBloqueo(ahora);
            } else if (!bloqueada && estaba) {
                t.setBloqueadaPorDependencia(false);
                t.setFechaBloqueo(null);
            }
        }
        tareas.flush();
    }

    /** Prerrequisitos de cada tarea de la lista (solo los que también están en la lista), con una consulta. */
    public Map<Long, Set<Long>> prerrequisitos(List<Tarea> lista) {
        Set<Long> ids = lista.stream().map(Tarea::getIdTarea).collect(Collectors.toSet());
        Map<Long, Set<Long>> previas = new HashMap<>();
        for (DependenciaTarea d : dependencias.findByIdTareaDependienteIn(ids)) {
            if (ids.contains(d.getIdTareaPredecesora())) {
                previas.computeIfAbsent(d.getIdTareaDependiente(), k -> new TreeSet<>()).add(d.getIdTareaPredecesora());
            }
        }
        return previas;
    }

    public static TareaPlan plan(Tarea t, Map<Long, Set<Long>> previas) {
        return new TareaPlan(t.getIdTarea(), duracion(t.getHorasEstimadas()), t.getPorcentajeAvance(),
                previas.getOrDefault(t.getIdTarea(), Set.of()));
    }

    /** horas_estimadas numeric(6,2) → segundos exactos (1.25 h = 4500 s). */
    public static Duration duracion(BigDecimal horas) {
        return Duration.ofSeconds(horas.multiply(BigDecimal.valueOf(3600)).longValueExact());
    }

    /** Duración → horas con 2 decimales (puede ser negativa: la holgura de una tarea que ya no alcanza). */
    public static BigDecimal horas(Duration d) {
        return BigDecimal.valueOf(d.toMillis()).divide(BigDecimal.valueOf(3_600_000), 2, RoundingMode.HALF_UP);
    }
}
