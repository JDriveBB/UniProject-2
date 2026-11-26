package co.uniproject.nucleo.configuracionacademica.servicio;

/** Participante del proyecto, el curso con que cuenta y su equipo o null. Solo para el docente dueño. */
public record EstudianteProyectoVista(
        long idUsuario, String email, String nombre, long idCurso, String nombreCurso, Long idEquipo) {}
