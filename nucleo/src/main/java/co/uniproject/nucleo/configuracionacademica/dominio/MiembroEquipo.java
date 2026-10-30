package co.uniproject.nucleo.configuracionacademica.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

/** Tabla Miembro_Equipo del diagrama de BD. */
@Entity
@Table(name = "miembro_equipo")
@IdClass(MiembroEquipoId.class)
public class MiembroEquipo {

    @Id
    @Column(name = "id_equipo")
    private Long idEquipo;

    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_equipo")
    private RolEquipo rolEquipo = RolEquipo.INTEGRANTE;

    @Column(name = "fecha_ingreso")
    private Instant fechaIngreso;

    public Long getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(Long idEquipo) {
        this.idEquipo = idEquipo;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public RolEquipo getRolEquipo() {
        return rolEquipo;
    }

    public void setRolEquipo(RolEquipo rolEquipo) {
        this.rolEquipo = rolEquipo;
    }

    public Instant getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Instant fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }
}
