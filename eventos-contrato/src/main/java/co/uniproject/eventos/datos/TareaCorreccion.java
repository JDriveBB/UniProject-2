package co.uniproject.eventos.datos;

public record TareaCorreccion(Referencia proyecto, Referencia equipo, ReferenciaTarea tarea,
        Persona responsable, Referencia lider, String resumen) implements DatosEvento {}
