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

/** Tabla Usuario del diagrama de BD. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_plataforma")
    private RolPlataforma rolPlataforma;

    @Column(name = "autoriza_tratamiento_datos")
    private Boolean autorizaTratamientoDatos = false;

    @Column(name = "fecha_autorizacion")
    private Instant fechaAutorizacion;

    @Column(name = "fecha_registro")
    private Instant fechaRegistro;

    @Column(name = "telegram_chat_id", insertable = false, updatable = false)
    private Long telegramChatId;

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public RolPlataforma getRolPlataforma() {
        return rolPlataforma;
    }

    public void setRolPlataforma(RolPlataforma rolPlataforma) {
        this.rolPlataforma = rolPlataforma;
    }

    public Boolean getAutorizaTratamientoDatos() {
        return autorizaTratamientoDatos;
    }

    public void setAutorizaTratamientoDatos(Boolean autorizaTratamientoDatos) {
        this.autorizaTratamientoDatos = autorizaTratamientoDatos;
    }

    public Instant getFechaAutorizacion() {
        return fechaAutorizacion;
    }

    public void setFechaAutorizacion(Instant fechaAutorizacion) {
        this.fechaAutorizacion = fechaAutorizacion;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Instant fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    /** Solo lectura: lo escribe ServicioVinculacion (JdbcClient). */
    public Long getTelegramChatId() {
        return telegramChatId;
    }
}
