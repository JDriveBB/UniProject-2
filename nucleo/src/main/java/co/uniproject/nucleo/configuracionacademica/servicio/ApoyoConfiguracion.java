package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.configuracionacademica.dominio.Equipo;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipo;
import co.uniproject.nucleo.configuracionacademica.repositorio.MiembroEquipoRepositorio;
import co.uniproject.nucleo.ejecucion.repositorio.HitoEquipoRepositorio;
import co.uniproject.nucleo.seguridad.dominio.Usuario;
import co.uniproject.nucleo.seguridad.repositorio.UsuarioRepositorio;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Operaciones que comparten los servicios de configuración académica. */
@Component
public class ApoyoConfiguracion {

    private final MiembroEquipoRepositorio miembros;
    private final HitoEquipoRepositorio hitosEquipo;
    private final UsuarioRepositorio usuarios;
    private final JdbcClient jdbc;

    public ApoyoConfiguracion(MiembroEquipoRepositorio miembros, HitoEquipoRepositorio hitosEquipo,
                              UsuarioRepositorio usuarios, JdbcClient jdbc) {
        this.miembros = miembros;
        this.hitosEquipo = hitosEquipo;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    /** Decisión R-7: crea el hito_equipo de cada par (hito, equipo) del proyecto que falte. Llamar tras saveAndFlush. */
    public int completarHitosEquipo(long idProyecto) {
        return jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) SELECT h.id_hito, e.id_equipo FROM hito h "
                        + "JOIN equipo e ON e.id_proyecto = h.id_proyecto WHERE h.id_proyecto = :proyecto "
                        + "ON CONFLICT (id_hito, id_equipo) DO NOTHING")
                .param("proyecto", idProyecto)
                .update();
    }

    /** Equipo con sus miembros (primero el líder, luego por nombre) y sus hitos (por id de hito). */
    public EquipoVista vistaEquipo(Equipo equipo) {
        List<MiembroEquipo> lista = miembros.findByIdEquipo(equipo.getIdEquipo());
        Map<Long, String> nombres = usuarios.findAllById(lista.stream().map(MiembroEquipo::getIdUsuario).toList())
                .stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, Usuario::getNombre));
        List<MiembroVista> vistasMiembros = lista.stream()
                .map(m -> new MiembroVista(m.getIdUsuario(), nombres.get(m.getIdUsuario()), m.getRolEquipo()))
                .sorted(Comparator.comparing(MiembroVista::rolEquipo)
                        .thenComparing(MiembroVista::nombre)
                        .thenComparingLong(MiembroVista::idUsuario))
                .toList();
        List<HitoEquipoVista> vistasHitos = hitosEquipo.findByIdEquipo(equipo.getIdEquipo()).stream()
                .map(HitoEquipoVista::de)
                .sorted(Comparator.comparingLong(HitoEquipoVista::idHito))
                .toList();
        return new EquipoVista(equipo.getIdEquipo(), equipo.getIdProyecto(), equipo.getNombreEquipo(), vistasMiembros,
                vistasHitos);
    }
}
