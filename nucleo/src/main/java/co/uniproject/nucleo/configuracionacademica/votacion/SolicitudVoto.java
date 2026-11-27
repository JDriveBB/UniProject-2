package co.uniproject.nucleo.configuracionacademica.votacion;

import jakarta.validation.constraints.NotNull;

/** Solicitud para emitir o cambiar el voto de líder en la ronda abierta. */
public record SolicitudVoto(@NotNull Long idCandidato) {}
