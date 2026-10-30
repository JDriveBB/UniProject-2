package co.uniproject.nucleo.ejecucion.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Tabla Solicitud_Correccion del diagrama de BD. */
@Entity
@Table(name = "solicitud_correccion")
public class SolicitudCorreccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @Column(name = "id_tarea")
    private Long idTarea;

    @Column(name = "id_evidencia")
    private Long idEvidencia;

    @Column(name = "id_lider")
    private Long idLider;

    @Column(name = "id_estudiante")
    private Long idEstudiante;

    @Column(name = "observaciones")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoSolicitudCorreccion estado = EstadoSolicitudCorreccion.PENDIENTE;

    @Column(name = "fecha_solicitud")
    private Instant fechaSolicitud;

    @Column(name = "fecha_atencion")
    private Instant fechaAtencion;

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(Long idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public Long getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(Long idTarea) {
        this.idTarea = idTarea;
    }

    public Long getIdEvidencia() {
        return idEvidencia;
    }

    public void setIdEvidencia(Long idEvidencia) {
        this.idEvidencia = idEvidencia;
    }

    public Long getIdLider() {
        return idLider;
    }

    public void setIdLider(Long idLider) {
        this.idLider = idLider;
    }

    public Long getIdEstudiante() {
        return idEstudiante;
    }

    public void setIdEstudiante(Long idEstudiante) {
        this.idEstudiante = idEstudiante;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public EstadoSolicitudCorreccion getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitudCorreccion estado) {
        this.estado = estado;
    }

    public Instant getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(Instant fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public Instant getFechaAtencion() {
        return fechaAtencion;
    }

    public void setFechaAtencion(Instant fechaAtencion) {
        this.fechaAtencion = fechaAtencion;
    }
}
