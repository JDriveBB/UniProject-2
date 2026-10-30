package co.uniproject.nucleo.configuracionacademica.repositorio;

import co.uniproject.nucleo.configuracionacademica.dominio.MonitorProyecto;
import co.uniproject.nucleo.configuracionacademica.dominio.MonitorProyectoId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorProyectoRepositorio extends JpaRepository<MonitorProyecto, MonitorProyectoId> {

    List<MonitorProyecto> findByIdProyecto(Long idProyecto);

    List<MonitorProyecto> findByIdMonitor(Long idMonitor);
}
