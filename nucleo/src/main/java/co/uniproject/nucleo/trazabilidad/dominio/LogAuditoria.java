package co.uniproject.nucleo.trazabilidad.dominio;

import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/** Tabla Log_Auditoria del diagrama de BD. Solo lectura en JPA: se inserta con RegistroAuditoria (restricción 4e). */
@Entity
@Immutable
@Table(name = "log_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long idLog;

    @Column(name = "id_usuario_accion")
    private Long idUsuarioAccion;

    @Column(name = "entidad_afectada")
    private String entidadAfectada;

    @Column(name = "id_registro_afectado")
    private Long idRegistroAfectado;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion")
    private AccionAuditoria accion;

    @Column(name = "campo_modificado")
    private String campoModificado;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_nuevo")
    private String valorNuevo;

    @Column(name = "fecha_hora")
    private Instant fechaHora;

    public Long getIdLog() {
        return idLog;
    }

    public Long getIdUsuarioAccion() {
        return idUsuarioAccion;
    }

    public String getEntidadAfectada() {
        return entidadAfectada;
    }

    public Long getIdRegistroAfectado() {
        return idRegistroAfectado;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public String getCampoModificado() {
        return campoModificado;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }
}
