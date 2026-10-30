package co.uniproject.nucleo.evaluacion.servicio;

import java.math.BigDecimal;
import java.time.Instant;

public record NotaIndividualVista(long idEstudiante, String nombre, BigDecimal factorAjuste, BigDecimal notaSugerida,
                                  BigDecimal notaFinal, Instant fechaConfirmacion) {

    /** Lo que ve el propio estudiante (decisión 19-9): sin ICE ni sugerida. */
    public NotaIndividualVista soloFinal() {
        return new NotaIndividualVista(idEstudiante, nombre, null, null, notaFinal, fechaConfirmacion);
    }
}
