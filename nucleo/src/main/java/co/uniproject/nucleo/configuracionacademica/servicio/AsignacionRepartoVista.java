package co.uniproject.nucleo.configuracionacademica.servicio;

/** Estudiante asignado a un equipo por el reparto automático. */
public record AsignacionRepartoVista(long idUsuario, String nombre, long idEquipo, String nombreEquipo) {}
