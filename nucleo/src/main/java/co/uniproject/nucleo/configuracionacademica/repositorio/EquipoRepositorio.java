package co.uniproject.nucleo.configuracionacademica.repositorio;

import co.uniproject.nucleo.configuracionacademica.dominio.Equipo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoRepositorio extends JpaRepository<Equipo, Long> {

    List<Equipo> findByIdProyectoOrderByNombreEquipo(Long idProyecto);
}
