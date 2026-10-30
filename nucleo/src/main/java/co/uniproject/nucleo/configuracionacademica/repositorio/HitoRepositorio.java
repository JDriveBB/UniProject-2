package co.uniproject.nucleo.configuracionacademica.repositorio;

import co.uniproject.nucleo.configuracionacademica.dominio.Hito;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HitoRepositorio extends JpaRepository<Hito, Long> {

    List<Hito> findByIdProyectoOrderByOrden(Long idProyecto);
}
