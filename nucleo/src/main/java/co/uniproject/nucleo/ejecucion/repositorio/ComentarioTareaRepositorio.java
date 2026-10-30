package co.uniproject.nucleo.ejecucion.repositorio;

import co.uniproject.nucleo.ejecucion.dominio.ComentarioTarea;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComentarioTareaRepositorio extends JpaRepository<ComentarioTarea, Long> {

    List<ComentarioTarea> findByIdTareaOrderByFechaHora(Long idTarea);
}
