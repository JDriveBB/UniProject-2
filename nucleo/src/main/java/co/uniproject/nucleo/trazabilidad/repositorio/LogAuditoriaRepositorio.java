package co.uniproject.nucleo.trazabilidad.repositorio;

import co.uniproject.nucleo.trazabilidad.dominio.LogAuditoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface LogAuditoriaRepositorio extends Repository<LogAuditoria, Long> {

    Optional<LogAuditoria> findById(Long id);

    List<LogAuditoria> findByEntidadAfectadaAndIdRegistroAfectadoOrderByFechaHora(String entidadAfectada, Long idRegistroAfectado);

    List<LogAuditoria> findByIdUsuarioAccionOrderByFechaHora(Long idUsuarioAccion);
}
