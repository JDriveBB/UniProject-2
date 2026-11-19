package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.configuracionacademica.dominio.Equipo;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipo;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipoId;
import co.uniproject.nucleo.configuracionacademica.repositorio.EquipoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.MiembroEquipoRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.dominio.Usuario;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.seguridad.repositorio.UsuarioRepositorio;
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
 * Equipos del proyecto, sus miembros, sus invitados sin cuenta (D-12 20A) y su líder (supuesto 2 de entrega1: uno por
 * equipo). Solo el docente dueño; al invitado lo incorpora el registro de su cuenta.
 */
@Service
public class ServicioEquipos {

    /** Columna de correo_habilitado con la invitación; así la nombra la auditoría (Ley 1581: solo ids). */
    private static final String CAMPO_INVITACION = "id_equipo_invitado";

    private final EquipoRepositorio equipos;
    private final MiembroEquipoRepositorio miembros;
    private final UsuarioRepositorio usuarios;
    private final ApoyoConfiguracion apoyo;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioEquipos(
            EquipoRepositorio equipos,
            MiembroEquipoRepositorio miembros,
            UsuarioRepositorio usuarios,
            ApoyoConfiguracion apoyo,
            Permisos permisos,
            RegistroAuditoria auditoria,
            JdbcClient jdbc,
            Clock reloj) {
        this.equipos = equipos;
        this.miembros = miembros;
        this.usuarios = usuarios;
        this.apoyo = apoyo;
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

    /** Primero se revisa la habilitación en el proyecto: así no se revela si un correo cualquiera tiene cuenta. */
    @Transactional
    public EquipoVista agregarMiembro(UsuarioAutenticado usuario, long idEquipo, SolicitudMiembro solicitud) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        bloquearProyecto(equipo.getIdProyecto());
        String email = ServicioCuentas.normalizarEmail(solicitud.email());
        habilitacionDeEstudiante(equipo.getIdProyecto(), email);
        Usuario estudiante = usuarios.findByEmail(email)
                .filter(u -> u.getRolPlataforma() == RolPlataforma.ESTUDIANTE)
                .orElseThrow(() -> new ReglaNegocioException(
                        "ESTUDIANTE_NO_REGISTRADO",
                        "Ese estudiante aún no crea su cuenta. Pídele que se registre con ese correo"));
        if (tieneEquipoEnProyecto(equipo.getIdProyecto(), estudiante.getIdUsuario())) {
            throw new ReglaNegocioException(
                    "YA_TIENE_EQUIPO", "Ese estudiante ya pertenece a un equipo de este proyecto");
        }
        agregar(usuario.idUsuario(), idEquipo, estudiante.getIdUsuario());
        return apoyo.vistaEquipo(equipo);
    }

    /**
     * D-12 20A: pone en el equipo a un estudiante habilitado en el proyecto que aún no tiene cuenta. Queda invitado y
     * entra al equipo cuando se registra (incorporarInvitado). Mismo orden de revisión que agregarMiembro; invitarlo
     * otra vez al mismo equipo no cambia nada. Con cuenta se usa agregarMiembro. La auditoría guarda solo ids.
     */
    @Transactional
    public EstudianteProyectoVista invitar(UsuarioAutenticado usuario, long idEquipo, SolicitudInvitacion solicitud) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        bloquearProyecto(equipo.getIdProyecto());
        String email = ServicioCuentas.normalizarEmail(solicitud.email());
        Habilitacion habilitacion = habilitacionDeEstudiante(equipo.getIdProyecto(), email);
        if (habilitacion.usada() || usuarios.existsByEmail(email)) {
            throw new ReglaNegocioException(
                    "YA_TIENE_CUENTA", "Ese estudiante ya tiene cuenta: agrégalo como integrante del equipo");
        }
        Long invitado = habilitacion.idEquipoInvitado();
        if (invitado != null && invitado != idEquipo) {
            throw new ReglaNegocioException(
                    "YA_INVITADO", "Ese estudiante ya está invitado a otro equipo de este proyecto");
        }
        if (invitado == null) {
            jdbc.sql("UPDATE correo_habilitado SET id_equipo_invitado = :equipo WHERE id_correo_habilitado = :id")
                    .param("equipo", idEquipo)
                    .param("id", habilitacion.idCorreoHabilitado())
                    .update();
            auditoria.registrar(EntradaAuditoria.cambio(
                    usuario.idUsuario(),
                    "correo_habilitado",
                    habilitacion.idCorreoHabilitado(),
                    AccionAuditoria.ACTUALIZAR,
                    CAMPO_INVITACION,
                    null,
                    String.valueOf(idEquipo)));
        }
        return new EstudianteProyectoVista(email, null, false, null, idEquipo);
    }

    /**
     * D-12 20A: el invitado entra al equipo al crear su cuenta, en la transacción del registro y con las reglas de
     * agregarMiembro (equipo del proyecto de la habilitación, cuenta de estudiante, sin otro equipo en el proyecto).
     * Nunca hace fallar el registro: si la regla ya no se cumple, no lo agrega, devuelve false y la auditoría guarda la
     * invitación retirada (id_equipo_invitado: equipo → null). El registro ya borró la invitación de la habilitación.
     * No bloquea el proyecto: nadie más ve la cuenta nueva hasta que el registro termina.
     */
    @Transactional
    public boolean incorporarInvitado(long idUsuario, long idCorreoHabilitado, long idProyecto, long idEquipo) {
        boolean cumple = equipos.findById(idEquipo)
                        .filter(e -> e.getIdProyecto() == idProyecto)
                        .isPresent()
                && usuarios.findById(idUsuario)
                        .filter(u -> u.getRolPlataforma() == RolPlataforma.ESTUDIANTE)
                        .isPresent()
                && !tieneEquipoEnProyecto(idProyecto, idUsuario);
        if (!cumple) {
            auditoria.registrar(EntradaAuditoria.cambio(
                    idUsuario,
                    "correo_habilitado",
                    idCorreoHabilitado,
                    AccionAuditoria.ACTUALIZAR,
                    CAMPO_INVITACION,
                    String.valueOf(idEquipo),
                    null));
            return false;
        }
        agregar(idUsuario, idEquipo, idUsuario);
        return true;
    }

    /** Sus tareas no cambian: el líder las reasigna (RF11, bloque 18). */
    @Transactional
    public EquipoVista quitarMiembro(UsuarioAutenticado usuario, long idEquipo, long idUsuario) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
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
                        "SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :equipo " + "AND rol_equipo = 'LIDER'")
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

    /** Serializa los cambios de integrantes e invitaciones del proyecto (un estudiante, un equipo). */
    private void bloquearProyecto(long idProyecto) {
        jdbc.sql("SELECT id_proyecto FROM proyecto WHERE id_proyecto = :proyecto FOR NO KEY UPDATE")
                .param("proyecto", idProyecto)
                .query(Long.class)
                .single();
    }

    /** Habilitación de ESTUDIANTE del correo en el proyecto, bloqueada hasta el fin de la transacción. */
    private Habilitacion habilitacionDeEstudiante(long idProyecto, String email) {
        return jdbc.sql("SELECT id_correo_habilitado, fecha_uso IS NOT NULL AS usada, id_equipo_invitado "
                        + "FROM correo_habilitado WHERE id_proyecto = :proyecto AND email = :email "
                        + "AND rol_plataforma = 'ESTUDIANTE' FOR UPDATE")
                .param("proyecto", idProyecto)
                .param("email", email)
                .query((rs, fila) -> new Habilitacion(
                        rs.getLong("id_correo_habilitado"),
                        rs.getBoolean("usada"),
                        rs.getObject("id_equipo_invitado", Long.class)))
                .optional()
                .orElseThrow(() -> new ReglaNegocioException(
                        "CORREO_NO_HABILITADO", "Ese correo no está habilitado como estudiante en este proyecto"));
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

    private record Habilitacion(long idCorreoHabilitado, boolean usada, Long idEquipoInvitado) {}

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
