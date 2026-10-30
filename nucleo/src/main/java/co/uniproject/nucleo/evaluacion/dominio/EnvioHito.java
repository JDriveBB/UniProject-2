package co.uniproject.nucleo.evaluacion.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Tabla Envio_Hito del diagrama de BD. */
@Entity
@Table(name = "envio_hito")
public class EnvioHito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_envio")
    private Long idEnvio;

    @Column(name = "id_hito_equipo")
    private Long idHitoEquipo;

    @Column(name = "id_usuario_envia")
    private Long idUsuarioEnvia;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio;

    public Long getIdEnvio() {
        return idEnvio;
    }

    public void setIdEnvio(Long idEnvio) {
        this.idEnvio = idEnvio;
    }

    public Long getIdHitoEquipo() {
        return idHitoEquipo;
    }

    public void setIdHitoEquipo(Long idHitoEquipo) {
        this.idHitoEquipo = idHitoEquipo;
    }

    public Long getIdUsuarioEnvia() {
        return idUsuarioEnvia;
    }

    public void setIdUsuarioEnvia(Long idUsuarioEnvia) {
        this.idUsuarioEnvia = idUsuarioEnvia;
    }

    public Instant getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(Instant fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
