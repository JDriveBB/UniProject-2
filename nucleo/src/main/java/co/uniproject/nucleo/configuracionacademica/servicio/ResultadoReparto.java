package co.uniproject.nucleo.configuracionacademica.servicio;

import java.util.List;

/** Resultado del reparto automático de estudiantes sin equipo: asignados y los que quedaron sin cupo. */
public record ResultadoReparto(List<AsignacionRepartoVista> asignados, List<EstudianteDisponibleVista> sinCupo) {}
