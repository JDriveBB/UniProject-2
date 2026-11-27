package co.uniproject.nucleo.configuracionacademica.cursos;

/** Resumen de un curso para listados y asignación a proyectos. */
public record CursoResumen(long idCurso, String nombre, String periodo, boolean archivado) {}
