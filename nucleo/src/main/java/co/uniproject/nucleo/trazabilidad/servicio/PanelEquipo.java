package co.uniproject.nucleo.trazabilidad.servicio;

import java.util.List;

/** enRojo: hay integrantes inactivos o tareas bloqueadas hace más de 48 h (decisión 21-1). avancePromedio en 0..100. */
public record PanelEquipo(long idEquipo, String nombreEquipo, boolean enRojo, int tareasTotales, int tareasCompletadas,
                          int tareasRiesgoAlto, int avancePromedio, List<IntegranteInactivo> integrantesInactivos,
                          List<TareaBloqueada> tareasBloqueadas) {}
