package co.uniproject.nucleo.evaluacion.repositorio;

import co.uniproject.nucleo.evaluacion.dominio.EvaluacionHito;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluacionHitoRepositorio extends JpaRepository<EvaluacionHito, Long> {

    Optional<EvaluacionHito> findByIdEnvio(Long idEnvio);
}
