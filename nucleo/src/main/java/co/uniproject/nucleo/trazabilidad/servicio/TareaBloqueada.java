package co.uniproject.nucleo.trazabilidad.servicio;

import java.time.Instant;

public record TareaBloqueada(long idTarea, String titulo, Instant fechaBloqueo, long horasBloqueada) {}
