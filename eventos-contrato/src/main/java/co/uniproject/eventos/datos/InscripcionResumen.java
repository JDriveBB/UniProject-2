package co.uniproject.eventos.datos;

import java.time.Instant;

/** Faltan menos de 24 horas para el cierre de inscripción a equipos; lo recibe el docente con los conteos de sin equipo y cupos libres. */
public record InscripcionResumen(Referencia proyecto, Instant cierre, int sinEquipo, int cuposLibres, Persona docente)
        implements DatosEvento {}
