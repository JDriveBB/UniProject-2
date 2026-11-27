package co.uniproject.nucleo.configuracionacademica.cursos;

import co.uniproject.nucleo.seguridad.cuentas.CorreoHabilitadoVista;
import java.util.List;

/** Detalle completo de un curso para el docente dueño: datos del curso, miembros activos y correos pendientes. */
public record CursoDetalle(
        CursoVista curso, List<EstudianteCursoVista> estudiantes, List<CorreoHabilitadoVista> pendientes) {}
