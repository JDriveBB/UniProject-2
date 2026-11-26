package co.uniproject.nucleo.configuracionacademica.votacion;

/** Integrante elegible en una ronda de votación; votos es nulo mientras la ronda no esté cerrada o esté cancelada. */
public record CandidatoVista(long idUsuario, String nombre, Integer votos, boolean empatado) {}
