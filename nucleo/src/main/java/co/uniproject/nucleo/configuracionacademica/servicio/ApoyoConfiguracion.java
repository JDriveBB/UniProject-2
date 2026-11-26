package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.configuracionacademica.dominio.Equipo;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipo;
import co.uniproject.nucleo.configuracionacademica.repositorio.MiembroEquipoRepositorio;
import co.uniproject.nucleo.ejecucion.repositorio.HitoEquipoRepositorio;
import co.uniproject.nucleo.seguridad.dominio.Usuario;
import co.uniproject.nucleo.seguridad.repositorio.UsuarioRepositorio;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ApoyoConfiguracion(
            MiembroEquipoRepositorio miembros,
            HitoEquipoRepositorio hitosEquipo,
            UsuarioRepositorio usuarios,
            JdbcClient jdbc,
            RegistroAuditoria auditoria,
            Clock reloj) {
        this.miembros = miembros;
        this.hitosEquipo = hitosEquipo;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /** Decisión R-7: crea el hito_equipo de cada par (hito, equipo) del proyecto que falte. Llamar tras saveAndFlush. */
    public int completarHitosEquipo(long idProyecto) {
        return jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) SELECT h.id_hito, e.id_equipo FROM hito h "
                        + "JOIN equipo e ON e.id_proyecto = h.id_proyecto WHERE h.id_proyecto = :proyecto "
                        + "ON CONFLICT (id_hito, id_equipo) DO NOTHING")
                .param("proyecto", idProyecto)
                .update();
    }

    /** Serializa los cambios de integrantes, cupo y cierre del proyecto (un estudiante, un equipo). */
    public void bloquearProyecto(long idProyecto) {
        jdbc.sql("SELECT id_proyecto FROM proyecto WHERE id_proyecto = :proyecto FOR NO KEY UPDATE")
                .param("proyecto", idProyecto)
                .query(Long.class)
                .single();
    }

    /** Toma el candado del equipo en el orden único de candados (proyecto antes que equipo). */
    public void bloquearEquipo(long idEquipo) {
        jdbc.sql("SELECT id_equipo FROM equipo WHERE id_equipo = :e FOR NO KEY UPDATE")
                .param("e", idEquipo)
                .query(Long.class)
                .single();
    }

    public boolean tieneEquipoEnProyecto(long idProyecto, long idUsuario) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM miembro_equipo m JOIN equipo e "
                        + "ON e.id_equipo = m.id_equipo WHERE e.id_proyecto = :proyecto AND m.id_usuario = :usuario)")
                .param("proyecto", idProyecto)
                .param("usuario", idUsuario)
                .query(Boolean.class)
                .single();
    }

    public void agregarIntegrante(long idQuienAgrega, long idEquipo, long idEstudiante) {
        MiembroEquipo m = new MiembroEquipo();
        m.setIdEquipo(idEquipo);
        m.setIdUsuario(idEstudiante);
        m.setFechaIngreso(Instant.now(reloj));
        miembros.saveAndFlush(m);
        auditoria.registrar(EntradaAuditoria.cambio(
                idQuienAgrega,
                "miembro_equipo",
                idEquipo,
                AccionAuditoria.CREAR,
                "id_usuario",
                null,
                String.valueOf(idEstudiante)));
    }

    public int contarIntegrantes(long idEquipo) {
        return jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :equipo")
                .param("equipo", idEquipo)
                .query(Integer.class)
                .single();
    }

    public Optional<Long> cursoDelParticipante(long idProyecto, long idUsuario) {
        return jdbc.sql(
                        "SELECT id_curso FROM participante_proyecto WHERE id_proyecto = :proyecto AND id_usuario = :usuario")
                .param("proyecto", idProyecto)
                .param("usuario", idUsuario)
                .query(Long.class)
                .optional();
    }

    public Optional<Long> liderDe(long idEquipo) {
        return jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :equipo AND rol_equipo = 'LIDER'")
                .param("equipo", idEquipo)
                .query(Long.class)
                .optional();
    }

    /** Equipo con sus miembros (primero el líder, luego por nombre) y sus hitos (por id de hito, con su último envío). */
    public EquipoVista vistaEquipo(Equipo equipo) {
        List<MiembroEquipo> lista = miembros.findByIdEquipo(equipo.getIdEquipo());
        Map<Long, String> nombres =
                usuarios
                        .findAllById(
                                lista.stream().map(MiembroEquipo::getIdUsuario).toList())
                        .stream()
                        .collect(Collectors.toMap(Usuario::getIdUsuario, Usuario::getNombre));
        List<MiembroVista> vistasMiembros = lista.stream()
                .map(m -> new MiembroVista(m.getIdUsuario(), nombres.get(m.getIdUsuario()), m.getRolEquipo()))
                .sorted(Comparator.comparing(MiembroVista::rolEquipo)
                        .thenComparing(MiembroVista::nombre)
                        .thenComparingLong(MiembroVista::idUsuario))
                .toList();
        Map<Long, Instant> envios = new HashMap<>();
        jdbc.sql("SELECT he.id_hito_equipo, max(en.fecha_envio) AS fecha FROM envio_hito en "
                        + "JOIN hito_equipo he ON he.id_hito_equipo = en.id_hito_equipo WHERE he.id_equipo = :equipo "
                        + "GROUP BY he.id_hito_equipo")
                .param("equipo", equipo.getIdEquipo())
                .query((rs, fila) -> Map.entry(
                        rs.getLong("id_hito_equipo"),
                        rs.getObject("fecha", OffsetDateTime.class).toInstant()))
                .list()
                .forEach(par -> envios.put(par.getKey(), par.getValue()));
        List<HitoEquipoVista> vistasHitos = hitosEquipo.findByIdEquipo(equipo.getIdEquipo()).stream()
                .map(he -> HitoEquipoVista.de(he, envios.get(he.getIdHitoEquipo())))
                .sorted(Comparator.comparingLong(HitoEquipoVista::idHito))
                .toList();
        return new EquipoVista(
                equipo.getIdEquipo(),
                equipo.getIdProyecto(),
                equipo.getNombreEquipo(),
                vistasMiembros,
                vistasHitos,
                equipo.getIdCurso(),
                equipo.getCupo());
    }
}
