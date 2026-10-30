package co.uniproject.nucleo.ejecucion.repositorio;

import co.uniproject.nucleo.ejecucion.dominio.Tarea;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TareaRepositorio extends JpaRepository<Tarea, Long> {

    List<Tarea> findByIdHitoEquipo(Long idHitoEquipo);

    List<Tarea> findByIdEncargado(Long idEncargado);
}
