package co.uniproject.eventos.datos;

import java.time.Instant;

public record TareaReasignada(Referencia proyecto, Referencia equipo, ReferenciaTarea tarea,
        Persona integranteAnterior, Persona integranteNuevo, Referencia lider,
        Instant fecha) implements DatosEvento {}
