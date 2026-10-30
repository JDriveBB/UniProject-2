package co.uniproject.nucleo.ejecucion.repositorio;

import co.uniproject.nucleo.ejecucion.dominio.DependenciaTarea;
import co.uniproject.nucleo.ejecucion.dominio.DependenciaTareaId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependenciaTareaRepositorio extends JpaRepository<DependenciaTarea, DependenciaTareaId> {

    List<DependenciaTarea> findByIdTareaDependiente(Long idTareaDependiente);

    List<DependenciaTarea> findByIdTareaPredecesora(Long idTareaPredecesora);

    List<DependenciaTarea> findByIdTareaDependienteIn(Collection<Long> idsTareaDependiente);
}
