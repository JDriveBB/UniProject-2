package co.uniproject.nucleo.ejecucion.repositorio;

import co.uniproject.nucleo.ejecucion.dominio.Evidencia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenciaRepositorio extends JpaRepository<Evidencia, Long> {

    List<Evidencia> findByIdTareaOrderByFechaSubidaDesc(Long idTarea);
}
