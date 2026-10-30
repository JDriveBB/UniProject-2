package co.uniproject.nucleo.notificaciones.repositorio;

import co.uniproject.nucleo.notificaciones.dominio.Notificacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepositorio extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findTop50ByIdDestinatarioOrderByFechaCreacionDesc(Long idDestinatario);

    long countByIdDestinatarioAndLeidaFalse(Long idDestinatario);
}
