package co.uniproject.nucleo.configuracionacademica.votacion;

import jakarta.validation.constraints.NotNull;

/** Solicitud del docente para elegir al líder entre los empatados de la última ronda. */
public record SolicitudDesempate(@NotNull Long idUsuario) {}
