package co.uniproject.nucleo.ejecucion.dependencias;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Motor de dependencias (entrega1 sección 2.6; RF3, RF9, restricción 4d, RNF1). Java puro: sin Spring ni base de datos.
 * Recibe las tareas ya cargadas (las de un hito o las de un proyecto) y no guarda estado. O(tareas + dependencias).
 */
public final class MotorDependencias {

    /**
     * Horas de trabajo disponibles por día de calendario (decisión de Santy 2026-11-19, D-10; el estudiante no
     * trabaja 24 h al día).
     */
    public static final int HORAS_TRABAJO_POR_DIA = 4;

    private MotorDependencias() {}

    /** Orden topológico determinista (Kahn; entre las tareas disponibles va primero el id menor). */
    public static List<Long> ordenTopologico(Collection<TareaPlan> tareas) {
        Map<Long, TareaPlan> porId = indexar(tareas);
        Map<Long, List<Long>> sucesores = sucesores(porId);
        Map<Long, Integer> pendientes = new HashMap<>();
        PriorityQueue<Long> disponibles = new PriorityQueue<>();
        for (TareaPlan t : porId.values()) {
            pendientes.put(t.id(), t.prerrequisitos().size());
            if (t.prerrequisitos().isEmpty()) {
                disponibles.add(t.id());
            }
        }
        List<Long> orden = new ArrayList<>(porId.size());
        while (!disponibles.isEmpty()) {
            long id = disponibles.poll();
            orden.add(id);
            for (long s : sucesores.getOrDefault(id, List.of())) {
                if (pendientes.merge(s, -1, Integer::sum) == 0) {
                    disponibles.add(s);
                }
            }
        }
        if (orden.size() != porId.size()) {
            throw new IllegalStateException("Las dependencias forman un ciclo");
        }
        return List.copyOf(orden);
    }

    /** RF3: método de la ruta crítica con las duraciones estimadas completas, desde el inicio del plan. */
    public static RutaCritica rutaCritica(Collection<TareaPlan> tareas) {
        Map<Long, TareaPlan> porId = indexar(tareas);
        List<Long> orden = ordenTopologico(porId.values());
        Map<Long, Duration> duracion = new HashMap<>();
        for (TareaPlan t : porId.values()) {
            duracion.put(t.id(), t.duracionEstimada());
        }
        Map<Long, Duration> finTemprano = adelante(porId, orden, duracion);
        Duration total = Duration.ZERO;
        for (Duration f : finTemprano.values()) {
            total = mayor(total, f);
        }
        Map<Long, Duration> limite = new HashMap<>();
        for (long id : orden) {
            limite.put(id, total);
        }
        Map<Long, Duration> finTardio = atras(porId, orden, duracion, limite);
        Map<Long, Tiempos> tiempos = new LinkedHashMap<>();
        List<Long> criticas = new ArrayList<>();
        for (long id : orden) {
            Duration d = duracion.get(id);
            Duration holgura = finTardio.get(id).minus(finTemprano.get(id));
            tiempos.put(
                    id,
                    new Tiempos(
                            finTemprano.get(id).minus(d),
                            finTemprano.get(id),
                            finTardio.get(id).minus(d),
                            finTardio.get(id),
                            holgura));
            if (holgura.isZero()) {
                criticas.add(id);
            }
        }
        return new RutaCritica(orden, Collections.unmodifiableMap(tiempos), List.copyOf(criticas), total);
    }

    /**
     * RF9: evalúa el riesgo usando {@link #HORAS_TRABAJO_POR_DIA} horas de trabajo por cada día de calendario.
     *
     * @see #evaluarRiesgo(Collection, Instant, Map, int)
     */
    public static List<RiesgoTarea> evaluarRiesgo(
            Collection<TareaPlan> tareas, Instant ahora, Map<Long, Instant> fechaLimitePorTarea) {
        return evaluarRiesgo(tareas, ahora, fechaLimitePorTarea, HORAS_TRABAJO_POR_DIA);
    }

    /**
     * RF9: misma pasada CPM, pero desde 'ahora' y con lo que le falta a cada tarea (duración × (100 - avance) / 100).
     * Las fechas límite se convierten a tiempo de trabajo disponible ({@code horasTrabajoPorDia} por cada 24 h de
     * calendario), el CPM, la holgura y el umbral de {@link NivelRiesgo#MEDIO} ({@code < duración / 4}) se calculan
     * en tiempo de trabajo, y {@code finProyectado} vuelve a tiempo de calendario. El fin tardío de cada tarea es el
     * menor entre su propia fecha límite y el inicio tardío de sus sucesoras. Devuelve una entrada por tarea,
     * ordenada por id.
     */
    public static List<RiesgoTarea> evaluarRiesgo(
            Collection<TareaPlan> tareas,
            Instant ahora,
            Map<Long, Instant> fechaLimitePorTarea,
            int horasTrabajoPorDia) {
        if (horasTrabajoPorDia < 1 || horasTrabajoPorDia > 24) {
            throw new IllegalArgumentException("horasTrabajoPorDia fuera de 1..24: " + horasTrabajoPorDia);
        }
        Map<Long, TareaPlan> porId = indexar(tareas);
        if (!fechaLimitePorTarea.keySet().containsAll(porId.keySet())) {
            throw new IllegalArgumentException("Falta la fecha límite de alguna tarea");
        }
        List<Long> orden = ordenTopologico(porId.values());
        Map<Long, Duration> restante = new HashMap<>();
        Map<Long, Duration> limite = new HashMap<>();
        for (TareaPlan t : porId.values()) {
            restante.put(
                    t.id(), t.duracionEstimada().multipliedBy(100 - t.avance()).dividedBy(100));
            limite.put(
                    t.id(),
                    Duration.between(ahora, fechaLimitePorTarea.get(t.id()))
                            .multipliedBy(horasTrabajoPorDia)
                            .dividedBy(24));
        }
        Map<Long, Duration> finTemprano = adelante(porId, orden, restante);
        Map<Long, Duration> finTardio = atras(porId, orden, restante, limite);
        List<RiesgoTarea> resultado = new ArrayList<>();
        for (long id : new TreeSet<>(porId.keySet())) {
            TareaPlan t = porId.get(id);
            Duration holgura = finTardio.get(id).minus(finTemprano.get(id));
            NivelRiesgo nivel;
            if (t.completada()) {
                nivel = NivelRiesgo.BAJO;
            } else if (holgura.isNegative()) {
                nivel = NivelRiesgo.ALTO;
            } else if (holgura.compareTo(t.duracionEstimada().dividedBy(4)) < 0) {
                nivel = NivelRiesgo.MEDIO;
            } else {
                nivel = NivelRiesgo.BAJO;
            }
            Instant finProyectado =
                    ahora.plus(finTemprano.get(id).multipliedBy(24).dividedBy(horasTrabajoPorDia));
            resultado.add(new RiesgoTarea(id, nivel, finProyectado, holgura));
        }
        return List.copyOf(resultado);
    }

    /** entrega1 sección 2.6: una tarea está bloqueada si alguno de sus prerrequisitos no está al 100 %. Ordenado por id. */
    public static Set<Long> bloqueadas(Collection<TareaPlan> tareas) {
        Map<Long, TareaPlan> porId = indexar(tareas);
        Set<Long> resultado = new TreeSet<>();
        for (TareaPlan t : porId.values()) {
            for (long p : t.prerrequisitos()) {
                if (!porId.get(p).completada()) {
                    resultado.add(t.id());
                    break;
                }
            }
        }
        return Collections.unmodifiableSet(resultado);
    }

    /**
     * Caso de uso "Establecer dependencias" (entrega1 sección 2.7): un candidato es cíclico si es la propia tarea o si
     * depende, directa o recursivamente, de ella. Los demás se aceptan (no se influyen entre sí).
     */
    public static ResultadoDependencias agregarPrerrequisitos(
            Collection<TareaPlan> tareas, long tareaId, Set<Long> candidatos) {
        Map<Long, TareaPlan> porId = indexar(tareas);
        if (!porId.containsKey(tareaId)) {
            throw new IllegalArgumentException("Tarea inexistente: " + tareaId);
        }
        List<Long> aceptados = new ArrayList<>();
        List<Long> ciclicos = new ArrayList<>();
        for (long c : new TreeSet<>(candidatos)) {
            if (!porId.containsKey(c)) {
                throw new IllegalArgumentException("Tarea inexistente: " + c);
            }
            if (alcanza(porId, c, tareaId)) {
                ciclicos.add(c);
            } else {
                aceptados.add(c);
            }
        }
        return new ResultadoDependencias(List.copyOf(aceptados), List.copyOf(ciclicos));
    }

    private static Map<Long, TareaPlan> indexar(Collection<TareaPlan> tareas) {
        Map<Long, TareaPlan> porId = new HashMap<>();
        for (TareaPlan t : tareas) {
            if (porId.put(t.id(), t) != null) {
                throw new IllegalArgumentException("Tarea repetida: " + t.id());
            }
        }
        for (TareaPlan t : porId.values()) {
            for (long p : t.prerrequisitos()) {
                if (!porId.containsKey(p)) {
                    throw new IllegalArgumentException("Prerrequisito inexistente: " + p);
                }
            }
        }
        return porId;
    }

    private static Map<Long, List<Long>> sucesores(Map<Long, TareaPlan> porId) {
        Map<Long, List<Long>> sucesores = new HashMap<>();
        for (TareaPlan t : porId.values()) {
            for (long p : t.prerrequisitos()) {
                sucesores.computeIfAbsent(p, k -> new ArrayList<>()).add(t.id());
            }
        }
        return sucesores;
    }

    /** Pasada hacia adelante: fin temprano = máximo fin temprano de los prerrequisitos (o cero) + duración. */
    private static Map<Long, Duration> adelante(
            Map<Long, TareaPlan> porId, List<Long> orden, Map<Long, Duration> duracion) {
        Map<Long, Duration> fin = new HashMap<>();
        for (long id : orden) {
            Duration inicio = Duration.ZERO;
            for (long p : porId.get(id).prerrequisitos()) {
                inicio = mayor(inicio, fin.get(p));
            }
            fin.put(id, inicio.plus(duracion.get(id)));
        }
        return fin;
    }

    /** Pasada hacia atrás: fin tardío = mínimo entre el límite propio y el inicio tardío de cada sucesora. */
    private static Map<Long, Duration> atras(
            Map<Long, TareaPlan> porId, List<Long> orden, Map<Long, Duration> duracion, Map<Long, Duration> limite) {
        Map<Long, List<Long>> sucesores = sucesores(porId);
        Map<Long, Duration> finTardio = new HashMap<>();
        for (int i = orden.size() - 1; i >= 0; i--) {
            long id = orden.get(i);
            Duration fin = limite.get(id);
            for (long s : sucesores.getOrDefault(id, List.of())) {
                fin = menor(fin, finTardio.get(s).minus(duracion.get(s)));
            }
            finTardio.put(id, fin);
        }
        return finTardio;
    }

    /** true si 'destino' es 'origen' o uno de sus prerrequisitos directos o recursivos. */
    private static boolean alcanza(Map<Long, TareaPlan> porId, long origen, long destino) {
        Deque<Long> pila = new ArrayDeque<>();
        Set<Long> vistos = new HashSet<>();
        pila.push(origen);
        while (!pila.isEmpty()) {
            long id = pila.pop();
            if (id == destino) {
                return true;
            }
            if (vistos.add(id)) {
                for (long p : porId.get(id).prerrequisitos()) {
                    pila.push(p);
                }
            }
        }
        return false;
    }

    private static Duration mayor(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static Duration menor(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
