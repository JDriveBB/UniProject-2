package co.uniproject.nucleo.trazabilidad.servicio;

import java.time.Instant;

public record IntegranteInactivo(long idUsuario, String nombre, Instant ultimaActividad, long diasSinActividad) {}
