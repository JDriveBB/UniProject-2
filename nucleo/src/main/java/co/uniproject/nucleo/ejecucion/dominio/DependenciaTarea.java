package co.uniproject.nucleo.ejecucion.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Tabla Dependencia_Tarea del diagrama de BD. */
@Entity
@Table(name = "dependencia_tarea")
@IdClass(DependenciaTareaId.class)
public class DependenciaTarea {

    @Id
    @Column(name = "id_tarea_predecesora")
    private Long idTareaPredecesora;

    @Id
    @Column(name = "id_tarea_dependiente")
    private Long idTareaDependiente;

    public Long getIdTareaPredecesora() {
        return idTareaPredecesora;
    }

    public void setIdTareaPredecesora(Long idTareaPredecesora) {
        this.idTareaPredecesora = idTareaPredecesora;
    }

    public Long getIdTareaDependiente() {
        return idTareaDependiente;
    }

    public void setIdTareaDependiente(Long idTareaDependiente) {
        this.idTareaDependiente = idTareaDependiente;
    }
}
