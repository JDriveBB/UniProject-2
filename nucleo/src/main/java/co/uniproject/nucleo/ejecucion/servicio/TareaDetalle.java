package co.uniproject.nucleo.ejecucion.servicio;

import java.util.List;

/** Detalle de la tarea con sus últimos 50 registros de avance (más reciente primero). */
public record TareaDetalle(TareaVista tarea, boolean editable, List<AvanceVista> avances) {}
