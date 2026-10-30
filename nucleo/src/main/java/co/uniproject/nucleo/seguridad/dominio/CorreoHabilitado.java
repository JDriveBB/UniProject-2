package co.uniproject.nucleo.seguridad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Tabla Correo_Habilitado (adición al diagrama, decisión K2). */
@Entity
@Table(name = "correo_habilitado")
public class CorreoHabilitado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_correo_habilitado")
    private Long idCorreoHabilitado;

    @Column(name = "id_proyecto")
    private Long idProyecto;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_plataforma")
    private RolPlataforma rolPlataforma;

    @Column(name = "id_habilitado_por")
    private Long idHabilitadoPor;

    @Column(name = "fecha_habilitacion")
    private Instant fechaHabilitacion;

    @Column(name = "id_usuario_registrado")
    private Long idUsuarioRegistrado;

    @Column(name = "fecha_uso")
    private Instant fechaUso;

    public Long getIdCorreoHabilitado() {
        return idCorreoHabilitado;
    }

    public void setIdCorreoHabilitado(Long idCorreoHabilitado) {
        this.idCorreoHabilitado = idCorreoHabilitado;
    }

    public Long getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(Long idProyecto) {
        this.idProyecto = idProyecto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RolPlataforma getRolPlataforma() {
        return rolPlataforma;
    }

    public void setRolPlataforma(RolPlataforma rolPlataforma) {
        this.rolPlataforma = rolPlataforma;
    }

    public Long getIdHabilitadoPor() {
        return idHabilitadoPor;
    }

    public void setIdHabilitadoPor(Long idHabilitadoPor) {
        this.idHabilitadoPor = idHabilitadoPor;
    }

    public Instant getFechaHabilitacion() {
        return fechaHabilitacion;
    }

    public void setFechaHabilitacion(Instant fechaHabilitacion) {
        this.fechaHabilitacion = fechaHabilitacion;
    }

    public Long getIdUsuarioRegistrado() {
        return idUsuarioRegistrado;
    }

    public void setIdUsuarioRegistrado(Long idUsuarioRegistrado) {
        this.idUsuarioRegistrado = idUsuarioRegistrado;
    }

    public Instant getFechaUso() {
        return fechaUso;
    }

    public void setFechaUso(Instant fechaUso) {
        this.fechaUso = fechaUso;
    }
}
