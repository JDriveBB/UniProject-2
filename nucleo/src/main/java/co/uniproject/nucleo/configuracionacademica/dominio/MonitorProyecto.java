package co.uniproject.nucleo.configuracionacademica.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Tabla Monitor_Proyecto del diagrama de BD. */
@Entity
@Table(name = "monitor_proyecto")
@IdClass(MonitorProyectoId.class)
public class MonitorProyecto {

    @Id
    @Column(name = "id_proyecto")
    private Long idProyecto;

    @Id
    @Column(name = "id_monitor")
    private Long idMonitor;

    public Long getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(Long idProyecto) {
        this.idProyecto = idProyecto;
    }

    public Long getIdMonitor() {
        return idMonitor;
    }

    public void setIdMonitor(Long idMonitor) {
        this.idMonitor = idMonitor;
    }
}
