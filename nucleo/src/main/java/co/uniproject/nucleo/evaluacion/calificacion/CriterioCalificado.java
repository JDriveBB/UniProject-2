package co.uniproject.nucleo.evaluacion.calificacion;

import java.math.BigDecimal;

/** Nota de un criterio de la rúbrica (RF8): 0 <= nota <= puntajeMaximo; pesoPorcentaje en (0, 100]. */
public record CriterioCalificado(BigDecimal nota, BigDecimal puntajeMaximo, BigDecimal pesoPorcentaje) {

    public CriterioCalificado {
        if (puntajeMaximo.signum() <= 0) {
            throw new IllegalArgumentException("puntajeMaximo debe ser positivo");
        }
        if (nota.signum() < 0 || nota.compareTo(puntajeMaximo) > 0) {
            throw new IllegalArgumentException("nota fuera de 0..puntajeMaximo");
        }
        if (pesoPorcentaje.signum() <= 0 || pesoPorcentaje.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("pesoPorcentaje fuera de (0, 100]");
        }
    }
}
