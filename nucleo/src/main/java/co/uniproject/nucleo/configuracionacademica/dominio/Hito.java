package co.uniproject.nucleo.configuracionacademica.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Tabla Hito del diagrama de BD. */
@Entity
@Table(name = "hito")
public class Hito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hito")
    private Long idHito;

    @Column(name = "id_proyecto")
    private Long idProyecto;

    @Column(name = "nombre_hito")
    private String nombreHito;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "orden")
    private Integer orden;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "dias_inactividad_alerta")
    private Integer diasInactividadAlerta = 3;

    @Column(name = "notificar_docente_inactividad")
    private Boolean notificarDocenteInactividad = false;

    public Long getIdHito() {
        return idHito;
    }

    public void setIdHito(Long idHito) {
        this.idHito = idHito;
    }

    public Long getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(Long idProyecto) {
        this.idProyecto = idProyecto;
    }

    public String getNombreHito() {
        return nombreHito;
    }

    public void setNombreHito(String nombreHito) {
        this.nombreHito = nombreHito;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public Integer getDiasInactividadAlerta() {
        return diasInactividadAlerta;
    }

    public void setDiasInactividadAlerta(Integer diasInactividadAlerta) {
        this.diasInactividadAlerta = diasInactividadAlerta;
    }

    public Boolean getNotificarDocenteInactividad() {
        return notificarDocenteInactividad;
    }

    public void setNotificarDocenteInactividad(Boolean notificarDocenteInactividad) {
        this.notificarDocenteInactividad = notificarDocenteInactividad;
    }
}
