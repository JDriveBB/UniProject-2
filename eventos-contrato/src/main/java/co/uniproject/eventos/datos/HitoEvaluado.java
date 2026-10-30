package co.uniproject.eventos.datos;

import java.util.List;

public record HitoEvaluado(Referencia proyecto, Referencia equipo, Referencia hito,
        CaracterNota caracterNota, List<Persona> integrantes) implements DatosEvento {}
