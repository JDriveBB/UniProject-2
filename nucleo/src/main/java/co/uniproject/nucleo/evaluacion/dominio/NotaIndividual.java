package co.uniproject.nucleo.evaluacion.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Tabla Nota_Individual del diagrama de BD. */
@Entity
@Table(name = "nota_individual")
public class NotaIndividual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_individual")
    private Long idNotaIndividual;

    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @Column(name = "id_estudiante")
    private Long idEstudiante;

    @Column(name = "factor_ajuste")
    private BigDecimal factorAjuste;

    @Column(name = "nota_sugerida")
    private BigDecimal notaSugerida;

    @Column(name = "nota_final")
    private BigDecimal notaFinal;

    @Column(name = "fecha_confirmacion")
    private Instant fechaConfirmacion;

    public Long getIdNotaIndividual() {
        return idNotaIndividual;
    }

    public void setIdNotaIndividual(Long idNotaIndividual) {
        this.idNotaIndividual = idNotaIndividual;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public void setIdEvaluacion(Long idEvaluacion) {
        this.idEvaluacion = idEvaluacion;
    }

    public Long getIdEstudiante() {
        return idEstudiante;
    }

    public void setIdEstudiante(Long idEstudiante) {
        this.idEstudiante = idEstudiante;
    }

    public BigDecimal getFactorAjuste() {
        return factorAjuste;
    }

    public void setFactorAjuste(BigDecimal factorAjuste) {
        this.factorAjuste = factorAjuste;
    }

    public BigDecimal getNotaSugerida() {
        return notaSugerida;
    }

    public void setNotaSugerida(BigDecimal notaSugerida) {
        this.notaSugerida = notaSugerida;
    }

    public BigDecimal getNotaFinal() {
        return notaFinal;
    }

    public void setNotaFinal(BigDecimal notaFinal) {
        this.notaFinal = notaFinal;
    }

    public Instant getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public void setFechaConfirmacion(Instant fechaConfirmacion) {
        this.fechaConfirmacion = fechaConfirmacion;
    }
}
