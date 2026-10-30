package co.uniproject.nucleo.evaluacion.calificacion;

import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.nucleo.comun.FechasDominio;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Decisiones E1 y R-8: una evaluación es DEFINITIVA si se registró después del vencimiento (es_definitiva) o si el
 * hito ya venció al leerla; si no, PROVISIONAL. Al vencer no se escribe nada: el carácter se calcula al leer.
 */
public final class CaracterEvaluacion {

    private CaracterEvaluacion() {
    }

    public static CaracterNota de(boolean registradaDefinitiva, LocalDate fechaLimiteHito, Instant ahora) {
        boolean vencido = !ahora.isBefore(FechasDominio.venceEn(fechaLimiteHito));
        return registradaDefinitiva || vencido ? CaracterNota.DEFINITIVA : CaracterNota.PROVISIONAL;
    }
}
