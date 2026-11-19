package co.uniproject.nucleo.trazabilidad.servicio;

import java.util.List;

/**
 * enRojo: hay integrantes inactivos o tareas bloqueadas hace más de 48 h (decisión 21-1). avancePromedio en 0..100
 * (promedio simple de todas las tareas del equipo; se conserva por compatibilidad: la web usa avanceHitoActual).
 * hitoActual: el primer hito del equipo, por orden, en EN_CURSO (null si no hay). avanceHitoActual: avance de sus
 * tareas ponderado por horas estimadas, 0..100 (null sin hito en curso o sin tareas). todoEnviado: el equipo tiene
 * hitos y ninguno está EN_CURSO (D-12).
 */
public record PanelEquipo(
        long idEquipo,
        String nombreEquipo,
        boolean enRojo,
        int tareasTotales,
        int tareasCompletadas,
        int tareasRiesgoAlto,
        int avancePromedio,
        List<IntegranteInactivo> integrantesInactivos,
        List<TareaBloqueada> tareasBloqueadas,
        HitoActualPanel hitoActual,
        Integer avanceHitoActual,
        boolean todoEnviado) {

    /** Hito en curso del equipo en el panel. */
    public record HitoActualPanel(long idHitoEquipo, int orden, String nombreHito) {}
}
