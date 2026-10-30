package co.uniproject.eventos.datos;

import java.time.LocalDate;
import java.util.List;

public record TareaRutaCriticaRiesgo(Referencia proyecto, Referencia equipo, ReferenciaTarea tarea,
        Referencia responsable, int avance, Referencia hito, LocalDate fechaLimiteHito,
        List<Persona> integrantes, Persona docente) implements DatosEvento {}
