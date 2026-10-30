package co.uniproject.nucleo.ejecucion.repositorio;

import co.uniproject.nucleo.ejecucion.dominio.HitoEquipo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HitoEquipoRepositorio extends JpaRepository<HitoEquipo, Long> {

    List<HitoEquipo> findByIdEquipo(Long idEquipo);

    List<HitoEquipo> findByIdHito(Long idHito);

    Optional<HitoEquipo> findByIdHitoAndIdEquipo(Long idHito, Long idEquipo);
}
