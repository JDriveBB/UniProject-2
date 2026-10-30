package co.uniproject.nucleo.ejecucion.dependencias;

import java.time.Duration;
import java.time.Instant;

/** finProyectado: cuándo terminaría con lo que le falta; holgura: fin tardío permitido - finProyectado. */
public record RiesgoTarea(long tareaId, NivelRiesgo nivel, Instant finProyectado, Duration holgura) {}
