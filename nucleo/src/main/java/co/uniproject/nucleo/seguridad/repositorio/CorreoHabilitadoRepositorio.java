package co.uniproject.nucleo.seguridad.repositorio;

import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorreoHabilitadoRepositorio extends JpaRepository<CorreoHabilitado, Long> {

    List<CorreoHabilitado> findByEmailAndFechaUsoIsNull(String email);

    /**
     * Las habilitaciones sin usar del correo, bloqueadas hasta el fin de la transacción en orden ascendente de id para
     * evitar interbloqueos entre registros simultáneos del mismo correo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CorreoHabilitado c WHERE c.email = :email AND c.fechaUso IS NULL "
            + "ORDER BY c.idCorreoHabilitado")
    List<CorreoHabilitado> bloquearPendientes(@Param("email") String email);

    List<CorreoHabilitado> findByIdProyectoOrderByEmail(Long idProyecto);

    List<CorreoHabilitado> findByIdCursoAndFechaUsoIsNullOrderByEmail(Long idCurso);

    List<CorreoHabilitado> findByRolPlataformaOrderByEmail(RolPlataforma rolPlataforma);
}
