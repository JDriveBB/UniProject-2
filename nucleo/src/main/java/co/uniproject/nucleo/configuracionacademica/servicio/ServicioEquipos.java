package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.configuracionacademica.cursos.ParticipacionCursos;
import co.uniproject.nucleo.configuracionacademica.dominio.Equipo;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipo;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipoId;
import co.uniproject.nucleo.configuracionacademica.repositorio.EquipoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.MiembroEquipoRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Equipos del proyecto, sus miembros (participantes por curso) y su líder (supuesto 2 de entrega1: uno por equipo).
 * Solo el docente dueño.
 */
@Service
public class ServicioEquipos {

    private final EquipoRepositorio equipos;
    private final MiembroEquipoRepositorio miembros;
    private final ApoyoConfiguracion apoyo;
    private final ParticipacionCursos participacion;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioEquipos(
            EquipoRepositorio equipos,
            MiembroEquipoRepositorio miembros,
            ApoyoConfiguracion apoyo,
            ParticipacionCursos participacion,
            Permisos permisos,
            RegistroAuditoria auditoria,
            JdbcClient jdbc,
            Clock reloj) {
        this.equipos = equipos;
        this.miembros = miembros;
        this.apoyo = apoyo;
        this.participacion = participacion;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional
    public EquipoVista crear(UsuarioAutenticado usuario, long idProyecto, SolicitudEquipo solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        String nombre = solicitud.nombreEquipo().strip();
        boolean repetido = equipos.findByIdProyectoOrderByNombreEquipo(idProyecto).stream()
                .anyMatch(x -> x.getNombreEquipo().equalsIgnoreCase(nombre));
        if (repetido) {
            throw equipoRepetido();
        }
        Equipo equipo = new Equipo();
        equipo.setIdProyecto(idProyecto);
        equipo.setNombreEquipo(nombre);
        try {
            equipo = equipos.saveAndFlush(equipo);
        } catch (DataIntegrityViolationException e) {
            throw equipoRepetido();
        }
        apoyo.completarHitosEquipo(idProyecto);
        auditoria.registrar(
                EntradaAuditoria.de(usuario.idUsuario(), "equipo", equipo.getIdEquipo(), AccionAuditoria.CREAR));
        return apoyo.vistaEquipo(equipo);
    }

    /** Primero se revisa la participación en el proyecto: así no se revela si un correo cualquiera tiene cuenta. */
    @Transactional
    public EquipoVista agregarMiembro(UsuarioAutenticado usuario, long idEquipo, SolicitudMiembro solicitud) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        bloquearProyecto(equipo.getIdProyecto());
        String email = ServicioCuentas.normalizarEmail(solicitud.email());
        long idEstudiante = jdbc.sql("SELECT pp.id_usuario FROM participante_proyecto pp "
                        + "JOIN usuario u ON u.id_usuario = pp.id_usuario "
                        + "WHERE pp.id_proyecto = :p AND u.email = :email AND u.fecha_anonimizacion IS NULL")
                .param("p", equipo.getIdProyecto())
                .param("email", email)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new ReglaNegocioException(
                        "ESTUDIANTE_SIN_CURSO", "Ese correo no es de un estudiante de los cursos del proyecto"));
        if (tieneEquipoEnProyecto(equipo.getIdProyecto(), idEstudiante)) {
            throw new ReglaNegocioException(
                    "YA_TIENE_EQUIPO", "Ese estudiante ya pertenece a un equipo de este proyecto");
        }
        agregar(usuario.idUsuario(), idEquipo, idEstudiante);
        return apoyo.vistaEquipo(equipo);
    }

    /** Sus tareas no cambian: el líder las reasigna (RF11, bloque 18). */
    @Transactional
    public EquipoVista quitarMiembro(UsuarioAutenticado usuario, long idEquipo, long idUsuario) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        bloquearProyecto(equipo.getIdProyecto());
        jdbc.sql("SELECT id_equipo FROM equipo WHERE id_equipo = :equipo FOR NO KEY UPDATE")
                .param("equipo", idEquipo)
                .query(Long.class)
                .single();
        MiembroEquipoId id = new MiembroEquipoId(idEquipo, idUsuario);
        if (!miembros.existsById(id)) {
            throw new RecursoNoEncontradoException("Esa persona no es miembro del equipo");
        }
        miembros.deleteById(id);
        miembros.flush();
        auditoria.registrar(EntradaAuditoria.cambio(
                usuario.idUsuario(),
                "miembro_equipo",
                idEquipo,
                AccionAuditoria.ELIMINAR,
                "id_usuario",
                String.valueOf(idUsuario),
                null));
        participacion.sincronizar(equipo.getIdProyecto(), idUsuario);
        return apoyo.vistaEquipo(equipo);
    }

    /** Decisión 15-8: dos UPDATE en orden por el índice único parcial (un líder por equipo). */
    @Transactional
    public EquipoVista cambiarLider(UsuarioAutenticado usuario, long idEquipo, SolicitudLider solicitud) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        jdbc.sql("SELECT id_equipo FROM equipo WHERE id_equipo = :equipo FOR NO KEY UPDATE")
                .param("equipo", idEquipo)
                .query(Long.class)
                .single();
        long nuevo = solicitud.idUsuario();
        if (!permisos.esIntegranteDelEquipo(nuevo, idEquipo)) {
            throw new ReglaNegocioException("NO_ES_MIEMBRO", "El líder debe ser miembro del equipo");
        }
        Optional<Long> anterior = jdbc.sql(
                        "SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :equipo AND rol_equipo = 'LIDER'")
                .param("equipo", idEquipo)
                .query(Long.class)
                .optional();
        if (anterior.isEmpty() || anterior.get() != nuevo) {
            jdbc.sql("UPDATE miembro_equipo SET rol_equipo = 'INTEGRANTE' WHERE id_equipo = :equipo "
                            + "AND rol_equipo = 'LIDER'")
                    .param("equipo", idEquipo)
                    .update();
            int promovidos = jdbc.sql(
                            "UPDATE miembro_equipo SET rol_equipo = 'LIDER' WHERE id_equipo = :equipo AND id_usuario = :usuario")
                    .param("equipo", idEquipo)
                    .param("usuario", nuevo)
                    .update();
            if (promovidos != 1) {
                throw new ReglaNegocioException("NO_ES_MIEMBRO", "El líder debe ser miembro del equipo");
            }
            auditoria.registrar(EntradaAuditoria.cambio(
                    usuario.idUsuario(),
                    "equipo",
                    idEquipo,
                    AccionAuditoria.ACTUALIZAR,
                    "lider",
                    anterior.map(String::valueOf).orElse(null),
                    String.valueOf(nuevo)));
        }
        return apoyo.vistaEquipo(equipo);
    }

    /** Serializa los cambios de integrantes del proyecto (un estudiante, un equipo). */
    private void bloquearProyecto(long idProyecto) {
        jdbc.sql("SELECT id_proyecto FROM proyecto WHERE id_proyecto = :proyecto FOR NO KEY UPDATE")
                .param("proyecto", idProyecto)
                .query(Long.class)
                .single();
    }

    private boolean tieneEquipoEnProyecto(long idProyecto, long idUsuario) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM miembro_equipo m JOIN equipo e "
                        + "ON e.id_equipo = m.id_equipo WHERE e.id_proyecto = :proyecto AND m.id_usuario = :usuario)")
                .param("proyecto", idProyecto)
                .param("usuario", idUsuario)
                .query(Boolean.class)
                .single();
    }

    private void agregar(long idQuienAgrega, long idEquipo, long idEstudiante) {
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

    /** 403 si el equipo no existe o no es de un proyecto del docente (decisión 15-2). */
    private Equipo equipoDelDocente(UsuarioAutenticado usuario, long idEquipo) {
        Optional<Equipo> equipo = equipos.findById(idEquipo);
        permisos.exigir(equipo.isPresent()
                && permisos.esDocenteDelProyecto(
                        usuario.idUsuario(), equipo.get().getIdProyecto()));
        return equipo.get();
    }

    private static ReglaNegocioException equipoRepetido() {
        return new ReglaNegocioException("EQUIPO_REPETIDO", "Ya hay un equipo con ese nombre en el proyecto");
    }
}
