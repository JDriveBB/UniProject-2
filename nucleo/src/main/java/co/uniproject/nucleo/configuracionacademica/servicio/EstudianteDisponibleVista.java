package co.uniproject.nucleo.configuracionacademica.servicio;

/** Participante del proyecto sin equipo, con el curso con que cuenta; solo para el docente dueño. */
public record EstudianteDisponibleVista(String email, String nombre, long idCurso, String nombreCurso) {}
