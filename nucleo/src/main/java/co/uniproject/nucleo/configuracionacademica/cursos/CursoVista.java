package co.uniproject.nucleo.configuracionacademica.cursos;

import java.util.List;

/** Vista de un curso con el conteo de estudiantes activos (no anonimizados) y los proyectos asignados. */
public record CursoVista(
        long idCurso,
        String nombre,
        String periodo,
        String descripcion,
        boolean archivado,
        String nombreDocente,
        int estudiantes,
        List<ProyectoResumen> proyectos) {}
