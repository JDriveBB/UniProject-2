package co.uniproject.nucleo.ejecucion.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla Hito_Equipo del diagrama de BD. */
@Entity
@Table(name = "hito_equipo")
public class HitoEquipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hito_equipo")
    private Long idHitoEquipo;

    @Column(name = "id_hito")
    private Long idHito;

    @Column(name = "id_equipo")
    private Long idEquipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoHitoEquipo estado = EstadoHitoEquipo.EN_CURSO;

    public Long getIdHitoEquipo() {
        return idHitoEquipo;
    }

    public void setIdHitoEquipo(Long idHitoEquipo) {
        this.idHitoEquipo = idHitoEquipo;
    }

    public Long getIdHito() {
        return idHito;
    }

    public void setIdHito(Long idHito) {
        this.idHito = idHito;
    }

    public Long getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(Long idEquipo) {
        this.idEquipo = idEquipo;
    }

    public EstadoHitoEquipo getEstado() {
        return estado;
    }

    public void setEstado(EstadoHitoEquipo estado) {
        this.estado = estado;
    }
}
