package co.uniproject.nucleo.evaluacion.repositorio;

import co.uniproject.nucleo.evaluacion.dominio.NotaIndividual;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaIndividualRepositorio extends JpaRepository<NotaIndividual, Long> {

    List<NotaIndividual> findByIdEvaluacionOrderByIdEstudiante(Long idEvaluacion);
}
