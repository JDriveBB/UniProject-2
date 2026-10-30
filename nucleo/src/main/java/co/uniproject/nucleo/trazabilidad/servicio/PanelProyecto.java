package co.uniproject.nucleo.trazabilidad.servicio;

import java.time.Instant;
import java.util.List;

/** RF10: un renglón por equipo, ordenados por nombre. */
public record PanelProyecto(long idProyecto, String nombre, Instant generadoEn, List<PanelEquipo> equipos) {}
