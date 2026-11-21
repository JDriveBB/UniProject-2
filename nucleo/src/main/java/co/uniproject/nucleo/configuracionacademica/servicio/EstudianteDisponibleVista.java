package co.uniproject.nucleo.configuracionacademica.servicio;

/**
 * Cuenta de ESTUDIANTE sin equipo ni habilitación en el proyecto (T-046), solo para el docente dueño. Lleva solo
 * correo y nombre (Ley 1581).
 */
public record EstudianteDisponibleVista(String email, String nombre) {}
