package co.uniproject.nucleo.evaluacion.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Tabla Evaluacion_Hito del diagrama de BD. */
@Entity
@Table(name = "evaluacion_hito")
public class EvaluacionHito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @Column(name = "id_envio")
    private Long idEnvio;

    @Column(name = "id_docente")
    private Long idDocente;

    @Column(name = "nota_grupal")
    private BigDecimal notaGrupal;

    @Column(name = "retroalimentacion_general")
    private String retroalimentacionGeneral;

    @Column(name = "es_definitiva")
    private Boolean esDefinitiva = false;

    @Column(name = "fecha_evaluacion")
    private Instant fechaEvaluacion;

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public void setIdEvaluacion(Long idEvaluacion) {
        this.idEvaluacion = idEvaluacion;
    }

    public Long getIdEnvio() {
        return idEnvio;
    }

    public void setIdEnvio(Long idEnvio) {
        this.idEnvio = idEnvio;
    }

    public Long getIdDocente() {
        return idDocente;
    }

    public void setIdDocente(Long idDocente) {
        this.idDocente = idDocente;
    }

    public BigDecimal getNotaGrupal() {
        return notaGrupal;
    }

    public void setNotaGrupal(BigDecimal notaGrupal) {
        this.notaGrupal = notaGrupal;
    }

    public String getRetroalimentacionGeneral() {
        return retroalimentacionGeneral;
    }

    public void setRetroalimentacionGeneral(String retroalimentacionGeneral) {
        this.retroalimentacionGeneral = retroalimentacionGeneral;
    }

    public Boolean getEsDefinitiva() {
        return esDefinitiva;
    }

    public void setEsDefinitiva(Boolean esDefinitiva) {
        this.esDefinitiva = esDefinitiva;
    }

    public Instant getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    public void setFechaEvaluacion(Instant fechaEvaluacion) {
        this.fechaEvaluacion = fechaEvaluacion;
    }
}
