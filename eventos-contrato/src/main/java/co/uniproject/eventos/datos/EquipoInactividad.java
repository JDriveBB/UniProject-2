package co.uniproject.eventos.datos;

import java.time.Instant;

public record EquipoInactividad(Referencia proyecto, Referencia equipo, Referencia estudiante,
        Instant ultimaActividad, int periodoDias, Persona lider, Persona docente,
        boolean notificarDocente) implements DatosEvento {}
