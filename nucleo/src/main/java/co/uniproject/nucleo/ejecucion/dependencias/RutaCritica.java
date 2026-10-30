package co.uniproject.nucleo.ejecucion.dependencias;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** orden: topológico; tiempos: por id, en ese orden; criticas: tareas con holgura cero, en ese orden. */
public record RutaCritica(List<Long> orden, Map<Long, Tiempos> tiempos, List<Long> criticas, Duration duracionTotal) {}
