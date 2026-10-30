package co.uniproject.nucleo.configuracionacademica.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Tabla Criterio_Rubrica del diagrama de BD. */
@Entity
@Table(name = "criterio_rubrica")
public class CriterioRubrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_criterio")
    private Long idCriterio;

    @Column(name = "id_hito")
    private Long idHito;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "peso_porcentaje")
    private BigDecimal pesoPorcentaje;

    @Column(name = "puntaje_maximo")
    private BigDecimal puntajeMaximo;

    public Long getIdCriterio() {
        return idCriterio;
    }

    public void setIdCriterio(Long idCriterio) {
        this.idCriterio = idCriterio;
    }

    public Long getIdHito() {
        return idHito;
    }

    public void setIdHito(Long idHito) {
        this.idHito = idHito;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPesoPorcentaje() {
        return pesoPorcentaje;
    }

    public void setPesoPorcentaje(BigDecimal pesoPorcentaje) {
        this.pesoPorcentaje = pesoPorcentaje;
    }

    public BigDecimal getPuntajeMaximo() {
        return puntajeMaximo;
    }

    public void setPuntajeMaximo(BigDecimal puntajeMaximo) {
        this.puntajeMaximo = puntajeMaximo;
    }
}
