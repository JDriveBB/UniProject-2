package co.uniproject.nucleo.configuracionacademica.votacion;

import java.time.Instant;
import java.util.List;

/** Estado visible de la última ronda de votación del equipo (sin correos ni votos de terceros). */
public record RondaVista(
        long idRonda,
        OrigenRonda origen,
        Instant abreEn,
        Instant cierraEn,
        Instant cerradaEn,
        boolean abierta,
        ResultadoRonda resultado,
        Long idLiderElegido,
        String nombreLiderElegido,
        int votosEmitidos,
        int votantes,
        Long miVoto,
        boolean puedoVotar,
        List<CandidatoVista> candidatos) {}
