package co.uniproject.nucleo.evaluacion.repositorio;

import co.uniproject.nucleo.evaluacion.dominio.CalificacionCriterio;
import co.uniproject.nucleo.evaluacion.dominio.CalificacionCriterioId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalificacionCriterioRepositorio extends JpaRepository<CalificacionCriterio, CalificacionCriterioId> {

    List<CalificacionCriterio> findByIdEvaluacion(Long idEvaluacion);
}
