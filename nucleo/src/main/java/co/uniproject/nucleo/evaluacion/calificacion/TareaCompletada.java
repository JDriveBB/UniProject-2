package co.uniproject.nucleo.evaluacion.calificacion;

import java.math.BigDecimal;

/**
 * Tarea completada dentro del hito. El crédito es de quien la tenía asignada al completarla (plan-dominio C1).
 * aTiempo: fecha_completada no posterior al fin del día límite (America/Bogota).
 */
public record TareaCompletada(long idEstudiante, BigDecimal horasEstimadas, boolean aTiempo) {

    public TareaCompletada {
        if (horasEstimadas.signum() < 0) {
            throw new IllegalArgumentException("horasEstimadas negativas");
        }
    }
}
