package co.uniproject.nucleo.configuracionacademica.dominio;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de miembro_equipo (@IdClass). */
public class MiembroEquipoId implements Serializable {

    private Long idEquipo;
    private Long idUsuario;

    public MiembroEquipoId() {
    }

    public MiembroEquipoId(Long idEquipo, Long idUsuario) {
        this.idEquipo = idEquipo;
        this.idUsuario = idUsuario;
    }

    public Long getIdEquipo() {
        return idEquipo;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MiembroEquipoId otra
                && Objects.equals(idEquipo, otra.idEquipo)
                && Objects.equals(idUsuario, otra.idUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEquipo, idUsuario);
    }
}
