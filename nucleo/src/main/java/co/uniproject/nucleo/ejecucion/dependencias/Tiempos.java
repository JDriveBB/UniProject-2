package co.uniproject.nucleo.ejecucion.dependencias;

import java.time.Duration;

/** Tiempos CPM de una tarea, medidos desde el inicio del plan. holgura = finTardio - finTemprano (cero = crítica). */
public record Tiempos(Duration inicioTemprano, Duration finTemprano, Duration inicioTardio, Duration finTardio,
                      Duration holgura) {}
