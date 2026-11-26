package co.uniproject.nucleo.configuracionacademica.votacion;

import java.time.Instant;

/** Fila de la tabla ronda_lider. */
public record RondaLiderFila(
        long idRonda,
        long idEquipo,
        OrigenRonda origen,
        Instant abreEn,
        Instant cierraEn,
        Instant cerradaEn,
        ResultadoRonda resultado,
        Long idLiderElegido) {

    public boolean abierta(Instant ahora) {
        return cerradaEn == null && ahora.isBefore(cierraEn);
    }

    public boolean vencida(Instant ahora) {
        return cerradaEn == null && !ahora.isBefore(cierraEn);
    }
}
