package co.uniproject.nucleo.configuracionacademica.servicio;

import java.util.List;

/** Hitos por orden (con su rúbrica) y equipos por nombre (con miembros y hitos del equipo). */
public record ProyectoDetalle(ProyectoVista proyecto, List<HitoVista> hitos, List<EquipoVista> equipos) {}
