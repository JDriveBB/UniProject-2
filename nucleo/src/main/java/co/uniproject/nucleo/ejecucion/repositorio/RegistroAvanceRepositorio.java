package co.uniproject.nucleo.ejecucion.repositorio;

import co.uniproject.nucleo.ejecucion.dominio.RegistroAvance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAvanceRepositorio extends JpaRepository<RegistroAvance, Long> {

    List<RegistroAvance> findByIdTareaOrderByFechaHoraDesc(Long idTarea);
}
