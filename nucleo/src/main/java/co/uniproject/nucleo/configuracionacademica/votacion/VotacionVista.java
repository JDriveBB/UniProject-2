package co.uniproject.nucleo.configuracionacademica.votacion;

/** Estado de votación de líder de un equipo; ronda es nulo si el equipo nunca tuvo rondas. */
public record VotacionVista(
        long idEquipo,
        Long idLiderActual,
        String nombreLiderActual,
        RondaVista ronda,
        boolean puedeAbrir,
        boolean puedeDesempatar) {}
