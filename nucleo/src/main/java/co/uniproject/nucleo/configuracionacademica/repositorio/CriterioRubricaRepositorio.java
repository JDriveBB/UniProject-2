package co.uniproject.nucleo.configuracionacademica.repositorio;

import co.uniproject.nucleo.configuracionacademica.dominio.CriterioRubrica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriterioRubricaRepositorio extends JpaRepository<CriterioRubrica, Long> {

    List<CriterioRubrica> findByIdHitoOrderByIdCriterio(Long idHito);
}
