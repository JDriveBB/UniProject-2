package co.uniproject.nucleo.configuracionacademica.servicio;

/**
 * Correo de ESTUDIANTE habilitado en el proyecto (D-11, D-12), solo para el docente dueño. nombre: null sin cuenta.
 * idEquipo: equipo del proyecto donde es integrante, o null. idEquipoInvitado: equipo al que entrará al registrarse.
 */
public record EstudianteProyectoVista(
        String email, String nombre, boolean registrado, Long idEquipo, Long idEquipoInvitado) {}
