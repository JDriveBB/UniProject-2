package co.uniproject.nucleo.ejecucion.repositorio;

import co.uniproject.nucleo.ejecucion.dominio.SolicitudCorreccion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudCorreccionRepositorio extends JpaRepository<SolicitudCorreccion, Long> {

    List<SolicitudCorreccion> findByIdTareaOrderByFechaSolicitudDesc(Long idTarea);
}
