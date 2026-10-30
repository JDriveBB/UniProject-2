package co.uniproject.nucleo.configuracionacademica.dominio;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de monitor_proyecto (@IdClass). */
public class MonitorProyectoId implements Serializable {

    private Long idProyecto;
    private Long idMonitor;

    public MonitorProyectoId() {
    }

    public MonitorProyectoId(Long idProyecto, Long idMonitor) {
        this.idProyecto = idProyecto;
        this.idMonitor = idMonitor;
    }

    public Long getIdProyecto() {
        return idProyecto;
    }

    public Long getIdMonitor() {
        return idMonitor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MonitorProyectoId otra
                && Objects.equals(idProyecto, otra.idProyecto)
                && Objects.equals(idMonitor, otra.idMonitor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProyecto, idMonitor);
    }
}
