package co.uniproject.nucleo.ejecucion.dominio;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de dependencia_tarea (@IdClass). */
public class DependenciaTareaId implements Serializable {

    private Long idTareaPredecesora;
    private Long idTareaDependiente;

    public DependenciaTareaId() {
    }

    public DependenciaTareaId(Long idTareaPredecesora, Long idTareaDependiente) {
        this.idTareaPredecesora = idTareaPredecesora;
        this.idTareaDependiente = idTareaDependiente;
    }

    public Long getIdTareaPredecesora() {
        return idTareaPredecesora;
    }

    public Long getIdTareaDependiente() {
        return idTareaDependiente;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DependenciaTareaId otra
                && Objects.equals(idTareaPredecesora, otra.idTareaPredecesora)
                && Objects.equals(idTareaDependiente, otra.idTareaDependiente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTareaPredecesora, idTareaDependiente);
    }
}
