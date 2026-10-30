package co.uniproject.nucleo.evaluacion.dominio;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de calificacion_criterio (@IdClass). */
public class CalificacionCriterioId implements Serializable {

    private Long idEvaluacion;
    private Long idCriterio;

    public CalificacionCriterioId() {
    }

    public CalificacionCriterioId(Long idEvaluacion, Long idCriterio) {
        this.idEvaluacion = idEvaluacion;
        this.idCriterio = idCriterio;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public Long getIdCriterio() {
        return idCriterio;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CalificacionCriterioId otra
                && Objects.equals(idEvaluacion, otra.idEvaluacion)
                && Objects.equals(idCriterio, otra.idCriterio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEvaluacion, idCriterio);
    }
}
