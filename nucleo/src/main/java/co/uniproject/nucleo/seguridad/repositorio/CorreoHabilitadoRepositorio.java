package co.uniproject.nucleo.seguridad.repositorio;

import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorreoHabilitadoRepositorio extends JpaRepository<CorreoHabilitado, Long> {

    List<CorreoHabilitado> findByEmailAndFechaUsoIsNull(String email);

    List<CorreoHabilitado> findByIdProyectoOrderByEmail(Long idProyecto);

    List<CorreoHabilitado> findByRolPlataformaOrderByEmail(RolPlataforma rolPlataforma);
}
