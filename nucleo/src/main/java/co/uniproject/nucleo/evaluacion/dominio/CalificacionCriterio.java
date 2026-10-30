package co.uniproject.nucleo.evaluacion.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Tabla Calificacion_Criterio del diagrama de BD. */
@Entity
@Table(name = "calificacion_criterio")
@IdClass(CalificacionCriterioId.class)
public class CalificacionCriterio {

    @Id
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @Id
    @Column(name = "id_criterio")
    private Long idCriterio;

    @Column(name = "nota")
    private BigDecimal nota;

    @Column(name = "retroalimentacion")
    private String retroalimentacion;

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public void setIdEvaluacion(Long idEvaluacion) {
        this.idEvaluacion = idEvaluacion;
    }

    public Long getIdCriterio() {
        return idCriterio;
    }

    public void setIdCriterio(Long idCriterio) {
        this.idCriterio = idCriterio;
    }

    public BigDecimal getNota() {
        return nota;
    }

    public void setNota(BigDecimal nota) {
        this.nota = nota;
    }

    public String getRetroalimentacion() {
        return retroalimentacion;
    }

    public void setRetroalimentacion(String retroalimentacion) {
        this.retroalimentacion = retroalimentacion;
    }
}
