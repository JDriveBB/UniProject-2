package co.uniproject.nucleo.ejecucion.dependencias;

import java.time.Duration;
import java.time.Instant;

/**
 * Resultado de riesgo de una tarea: {@code finProyectado} es el instante de calendario en que terminaría con lo que
 * le falta según las horas de trabajo diarias; {@code holgura} está en horas de trabajo (fin tardío disponible menos
 * fin temprano en tiempo de trabajo).
 */
public record RiesgoTarea(long tareaId, NivelRiesgo nivel, Instant finProyectado, Duration holgura) {}
