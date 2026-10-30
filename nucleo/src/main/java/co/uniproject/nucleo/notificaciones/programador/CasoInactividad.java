package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.eventos.datos.EquipoInactividad;
import java.time.Instant;

public record CasoInactividad(long proyectoId, long estudianteId, Instant ultimaActividad, EquipoInactividad datos) {}
