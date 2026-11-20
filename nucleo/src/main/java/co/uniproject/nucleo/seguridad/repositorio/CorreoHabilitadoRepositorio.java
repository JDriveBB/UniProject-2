package co.uniproject.nucleo.seguridad.repositorio;

import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorreoHabilitadoRepositorio extends JpaRepository<CorreoHabilitado, Long> {

    List<CorreoHabilitado> findByEmailAndFechaUsoIsNull(String email);

    /**
     * Las habilitaciones sin usar del correo, bloqueadas hasta el fin de la transacción (registro): una invitación a
     * equipo que se guarda al mismo tiempo no se pierde ni queda en una habilitación ya usada (T-027).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CorreoHabilitado c WHERE c.email = :email AND c.fechaUso IS NULL "
            + "ORDER BY c.idCorreoHabilitado")
    List<CorreoHabilitado> bloquearPendientes(@Param("email") String email);

    /** Habilitación pendiente dueña del selector del enlace de invitación, bloqueada durante el registro (T-040). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CorreoHabilitado c WHERE c.enlaceSelector = :selector AND c.fechaUso IS NULL")
    Optional<CorreoHabilitado> bloquearPendientePorSelector(@Param("selector") byte[] selector);

    List<CorreoHabilitado> findByIdProyectoOrderByEmail(Long idProyecto);

    List<CorreoHabilitado> findByRolPlataformaOrderByEmail(RolPlataforma rolPlataforma);
}
