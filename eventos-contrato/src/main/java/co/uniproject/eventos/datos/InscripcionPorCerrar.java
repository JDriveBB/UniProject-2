package co.uniproject.eventos.datos;

import java.time.Instant;
import java.util.List;

/** Faltan menos de 24 horas para el cierre de inscripción a equipos; lo reciben los participantes sin equipo. */
public record InscripcionPorCerrar(Referencia proyecto, Instant cierre, List<Persona> estudiantes)
        implements DatosEvento {}
