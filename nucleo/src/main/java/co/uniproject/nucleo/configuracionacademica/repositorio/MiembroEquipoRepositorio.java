package co.uniproject.nucleo.configuracionacademica.repositorio;

import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipo;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipoId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiembroEquipoRepositorio extends JpaRepository<MiembroEquipo, MiembroEquipoId> {

    List<MiembroEquipo> findByIdEquipo(Long idEquipo);

    List<MiembroEquipo> findByIdUsuario(Long idUsuario);
}
