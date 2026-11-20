package co.uniproject.nucleo.seguridad.cuentas;

import java.time.Instant;

/**
 * Código del enlace de invitación de un solo uso recién generado (T-040, D-15 5A). Solo se devuelve al generarlo;
 * toString no muestra el código.
 */
public record EnlaceRegistroVista(String codigo, Instant venceEn) {

    @Override
    public String toString() {
        return "EnlaceRegistroVista[codigo=***, venceEn=" + venceEn + "]";
    }
}
