package co.uniproject.nucleo.evaluacion.repositorio;

import co.uniproject.nucleo.evaluacion.dominio.EnvioHito;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvioHitoRepositorio extends JpaRepository<EnvioHito, Long> {

    List<EnvioHito> findByIdHitoEquipoOrderByFechaEnvioDesc(Long idHitoEquipo);
}
