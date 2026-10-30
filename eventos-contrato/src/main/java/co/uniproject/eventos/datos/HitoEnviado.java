package co.uniproject.eventos.datos;

import java.time.Instant;

public record HitoEnviado(Referencia proyecto, Referencia equipo, Referencia hito,
        Instant fechaEnvio, Referencia estudiante, Persona docente) implements DatosEvento {}
