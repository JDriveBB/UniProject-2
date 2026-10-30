package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.ejecucion.dominio.EstadoHitoEquipo;
import java.time.LocalDate;
import java.util.List;

/** Tablero de un hito de equipo (pantalla del equipo). editable: decisión 17a-1. */
public record TableroHito(long idHitoEquipo, long idHito, String nombreHito, LocalDate fechaLimite,
                          EstadoHitoEquipo estado, boolean editable, long idEquipo, String nombreEquipo,
                          long idProyecto, String nombreProyecto, List<TareaVista> tareas) {

    public static TableroHito de(ContextoHito h, boolean editable, List<TareaVista> tareas) {
        return new TableroHito(h.idHitoEquipo(), h.idHito(), h.nombreHito(), h.fechaLimite(), h.estado(), editable,
                h.idEquipo(), h.nombreEquipo(), h.idProyecto(), h.nombreProyecto(), tareas);
    }
}
