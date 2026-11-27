package co.uniproject.nucleo.configuracionacademica.cursos;

import java.time.Instant;

/** Estudiante miembro de un curso, visible solo para el docente dueño. */
public record EstudianteCursoVista(long idUsuario, String nombre, String email, Instant fechaIngreso) {}
