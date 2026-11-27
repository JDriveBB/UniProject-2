package co.uniproject.eventos.datos;

/** El docente agrega a un curso una cuenta que ya existe; lo recibe el estudiante agregado. */
public record CursoEstudianteAgregado(Referencia curso, String periodo, Referencia docente, Persona estudiante)
        implements DatosEvento {}
