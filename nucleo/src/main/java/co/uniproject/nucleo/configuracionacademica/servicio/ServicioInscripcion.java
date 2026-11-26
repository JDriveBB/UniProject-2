package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.cursos.ParticipacionCursos;
import co.uniproject.nucleo.configuracionacademica.dominio.Equipo;
import co.uniproject.nucleo.configuracionacademica.repositorio.EquipoRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autoinscripción de estudiantes a equipos con cupo, configuración del cierre de equipos y reparto automático.
 */
@Service
public class ServicioInscripcion {

    private final JdbcClient jdbc;
    private final EquipoRepositorio equipos;
    private final ApoyoConfiguracion apoyo;
    private final ParticipacionCursos participacion;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioInscripcion(
            JdbcClient jdbc,
            EquipoRepositorio equipos,
            ApoyoConfiguracion apoyo,
            ParticipacionCursos participacion,
            Permisos permisos,
            RegistroAuditoria auditoria,
            Clock reloj) {
        this.jdbc = jdbc;
        this.equipos = equipos;
        this.apoyo = apoyo;
        this.participacion = participacion;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /**
     * Vista de autoinscripción del proyecto. Expone solo los nombres de los integrantes, sin correos ni ids de usuario
     * (Ley 1581). En proyectos no mixtos, un participante ve solo los equipos de su curso más el suyo.
     */
    @Transactional(readOnly = true)
    public InscripcionVista vista(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.puedeVerProyecto(usuario.idUsuario(), idProyecto));
        record ConfigProyecto(Instant cierreEquipos, int horasRonda, boolean equiposMixtos) {}
        ConfigProyecto config = jdbc.sql(
                        "SELECT cierre_equipos, horas_ronda, equipos_mixtos FROM proyecto WHERE id_proyecto = :p")
                .param("p", idProyecto)
                .query((rs, fila) -> new ConfigProyecto(
                        instante(rs.getObject("cierre_equipos", OffsetDateTime.class)),
                        rs.getInt("horas_ronda"),
                        rs.getBoolean("equipos_mixtos")))
                .single();
        boolean abierta = estaAbierta(config.cierreEquipos());
        Long idCursoQueCuenta =
                apoyo.cursoDelParticipante(idProyecto, usuario.idUsuario()).orElse(null);
        boolean esParticipante = idCursoQueCuenta != null;
        Long idMiEquipo = esParticipante
                ? jdbc.sql("SELECT m.id_equipo FROM miembro_equipo m JOIN equipo e ON e.id_equipo = m.id_equipo "
                                + "WHERE e.id_proyecto = :p AND m.id_usuario = :u LIMIT 1")
                        .param("p", idProyecto)
                        .param("u", usuario.idUsuario())
                        .query(Long.class)
                        .optional()
                        .orElse(null)
                : null;
        record FilaEquipo(long idEquipo, String nombreEquipo, Long idCurso, String nombreCurso, Integer cupo) {}
        List<FilaEquipo> todos = jdbc.sql(
                        "SELECT e.id_equipo, e.nombre_equipo, e.id_curso, c.nombre AS nombre_curso, e.cupo "
                                + "FROM equipo e LEFT JOIN curso c ON c.id_curso = e.id_curso "
                                + "WHERE e.id_proyecto = :p ORDER BY e.nombre_equipo")
                .param("p", idProyecto)
                .query((rs, fila) -> new FilaEquipo(
                        rs.getLong("id_equipo"),
                        rs.getString("nombre_equipo"),
                        rs.getObject("id_curso", Long.class),
                        rs.getString("nombre_curso"),
                        rs.getObject("cupo", Integer.class)))
                .list();
        Map<Long, List<String>> nombresPorEquipo = new LinkedHashMap<>();
        jdbc.sql("SELECT m.id_equipo, u.nombre FROM miembro_equipo m "
                        + "JOIN equipo e ON e.id_equipo = m.id_equipo "
                        + "JOIN usuario u ON u.id_usuario = m.id_usuario "
                        + "WHERE e.id_proyecto = :p ORDER BY m.id_equipo, u.nombre, u.id_usuario")
                .param("p", idProyecto)
                .query((rs, fila) -> Map.entry(rs.getLong("id_equipo"), rs.getString("nombre")))
                .list()
                .forEach(par -> nombresPorEquipo
                        .computeIfAbsent(par.getKey(), k -> new ArrayList<>())
                        .add(par.getValue()));
        List<EquipoInscripcionVista> lista = new ArrayList<>();
        for (FilaEquipo e : todos) {
            if (esParticipante
                    && !config.equiposMixtos()
                    && e.idCurso() != null
                    && !Objects.equals(e.idCurso(), idCursoQueCuenta)
                    && !Objects.equals(e.idEquipo(), idMiEquipo)) {
                continue;
            }
            List<String> nombres = List.copyOf(nombresPorEquipo.getOrDefault(e.idEquipo(), List.of()));
            int integrantes = nombres.size();
            boolean cursoCoincide = e.idCurso() == null || Objects.equals(e.idCurso(), idCursoQueCuenta);
            boolean puedeUnirse = abierta
                    && esParticipante
                    && idMiEquipo == null
                    && e.cupo() != null
                    && integrantes < e.cupo()
                    && cursoCoincide;
            lista.add(new EquipoInscripcionVista(
                    e.idEquipo(),
                    e.nombreEquipo(),
                    e.idCurso(),
                    e.nombreCurso(),
                    e.cupo(),
                    integrantes,
                    nombres,
                    puedeUnirse));
        }
        return new InscripcionVista(
                config.cierreEquipos(), config.horasRonda(), abierta, idCursoQueCuenta, idMiEquipo, lista);
    }

    /**
     * Configura la fecha de cierre de autoinscripción y las horas de ronda de líder. Solo el docente dueño. Cambios de
     * cierre reinician recordatorio_cierre_en y no se permiten si ya se abrieron rondas.
     */
    @Transactional
    public InscripcionVista configurar(UsuarioAutenticado usuario, long idProyecto, SolicitudInscripcion solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        apoyo.bloquearProyecto(idProyecto);
        record EstadoConfig(Instant cierreEquipos, int horasRonda, Instant rondasProgramadasEn) {}
        EstadoConfig actual = jdbc.sql(
                        "SELECT cierre_equipos, horas_ronda, rondas_programadas_en FROM proyecto WHERE id_proyecto = :p")
                .param("p", idProyecto)
                .query((rs, fila) -> new EstadoConfig(
                        instante(rs.getObject("cierre_equipos", OffsetDateTime.class)),
                        rs.getInt("horas_ronda"),
                        instante(rs.getObject("rondas_programadas_en", OffsetDateTime.class))))
                .single();
        Instant cierreNuevo = solicitud.cierreEquipos();
        if (!Objects.equals(actual.cierreEquipos(), cierreNuevo)) {
            if (actual.rondasProgramadasEn() != null) {
                throw new ReglaNegocioException(
                        "INSCRIPCION_YA_CERRADA", "La inscripción ya cerró y se abrieron las rondas de líder");
            }
            if (cierreNuevo != null && !cierreNuevo.isAfter(Instant.now(reloj))) {
                throw new SolicitudInvalidaException("CIERRE_EN_EL_PASADO", "La fecha de cierre debe ser futura");
            }
            OffsetDateTime valor = cierreNuevo == null ? null : OffsetDateTime.ofInstant(cierreNuevo, ZoneOffset.UTC);
            jdbc.sql("UPDATE proyecto SET cierre_equipos = :c, recordatorio_cierre_en = NULL WHERE id_proyecto = :p")
                    .param("c", valor, Types.TIMESTAMP_WITH_TIMEZONE)
                    .param("p", idProyecto)
                    .update();
            AuditoriaCambios.siCambia(
                    auditoria,
                    usuario.idUsuario(),
                    "proyecto",
                    idProyecto,
                    "cierre_equipos",
                    actual.cierreEquipos(),
                    cierreNuevo);
        }
        int horasNuevas = solicitud.horasRonda();
        if (actual.horasRonda() != horasNuevas) {
            jdbc.sql("UPDATE proyecto SET horas_ronda = :h WHERE id_proyecto = :p")
                    .param("h", horasNuevas)
                    .param("p", idProyecto)
                    .update();
            AuditoriaCambios.siCambia(
                    auditoria,
                    usuario.idUsuario(),
                    "proyecto",
                    idProyecto,
                    "horas_ronda",
                    actual.horasRonda(),
                    horasNuevas);
        }
        return vista(usuario, idProyecto);
    }

    /** El estudiante participante se une a un equipo con cupo mientras la inscripción esté abierta. */
    @Transactional
    public EquipoVista unirse(UsuarioAutenticado usuario, long idEquipo) {
        Optional<Equipo> opt = equipos.findById(idEquipo);
        permisos.exigir(opt.isPresent()
                && permisos.esParticipanteDelProyecto(
                        usuario.idUsuario(), opt.get().getIdProyecto()));
        Equipo equipo = opt.get();
        long idProyecto = equipo.getIdProyecto();
        apoyo.bloquearProyecto(idProyecto);
        apoyo.bloquearEquipo(idEquipo);
        Long idCursoParticipante =
                apoyo.cursoDelParticipante(idProyecto, usuario.idUsuario()).orElse(null);
        permisos.exigir(idCursoParticipante != null);
        record InfoEquipo(Long idCurso, Integer cupo) {}
        InfoEquipo info = jdbc.sql("SELECT id_curso, cupo FROM equipo WHERE id_equipo = :e")
                .param("e", idEquipo)
                .query((rs, fila) ->
                        new InfoEquipo(rs.getObject("id_curso", Long.class), rs.getObject("cupo", Integer.class)))
                .single();
        if (info.cupo() == null) {
            throw new ReglaNegocioException("EQUIPO_SIN_INSCRIPCION", "Ese equipo lo arma el docente");
        }
        if (!inscripcionAbierta(idProyecto)) {
            throw new ReglaNegocioException("INSCRIPCION_CERRADA", "La inscripción a equipos está cerrada");
        }
        if (apoyo.tieneEquipoEnProyecto(idProyecto, usuario.idUsuario())) {
            throw new ReglaNegocioException(
                    "YA_TIENE_EQUIPO", "Ya estás en un equipo de este proyecto. Sal primero de ese equipo");
        }
        if (info.idCurso() != null && !Objects.equals(info.idCurso(), idCursoParticipante)) {
            throw new ReglaNegocioException("CURSO_DEL_EQUIPO", "Ese equipo es de otro curso");
        }
        if (apoyo.contarIntegrantes(idEquipo) >= info.cupo()) {
            throw new ReglaNegocioException("EQUIPO_LLENO", "El equipo ya está completo");
        }
        apoyo.agregarIntegrante(usuario.idUsuario(), idEquipo, usuario.idUsuario());
        return apoyo.vistaEquipo(equipo);
    }

    /**
     * El estudiante sale de un equipo con cupo mientras la inscripción esté abierta y no tenga tareas asignadas en ese
     * equipo.
     */
    @Transactional
    public void salirse(UsuarioAutenticado usuario, long idEquipo) {
        Optional<Equipo> opt = equipos.findById(idEquipo);
        permisos.exigir(opt.isPresent()
                && permisos.esParticipanteDelProyecto(
                        usuario.idUsuario(), opt.get().getIdProyecto()));
        long idProyecto = opt.get().getIdProyecto();
        apoyo.bloquearProyecto(idProyecto);
        apoyo.bloquearEquipo(idEquipo);
        boolean esMiembro = jdbc.sql(
                        "SELECT EXISTS (SELECT 1 FROM miembro_equipo WHERE id_equipo = :e AND id_usuario = :u)")
                .param("e", idEquipo)
                .param("u", usuario.idUsuario())
                .query(Boolean.class)
                .single();
        if (!esMiembro) {
            throw new RecursoNoEncontradoException("No estás en ese equipo");
        }
        Integer cupo = jdbc.sql("SELECT cupo FROM equipo WHERE id_equipo = :e")
                .param("e", idEquipo)
                .query((rs, fila) -> Optional.ofNullable(rs.getObject("cupo", Integer.class)))
                .single()
                .orElse(null);
        if (cupo == null) {
            throw new ReglaNegocioException(
                    "EQUIPO_SIN_INSCRIPCION", "Ese equipo lo arma el docente. Pídele que te saque");
        }
        if (!inscripcionAbierta(idProyecto)) {
            throw new ReglaNegocioException("INSCRIPCION_CERRADA", "La inscripción a equipos está cerrada");
        }
        boolean tieneTareas = jdbc.sql("SELECT EXISTS (SELECT 1 FROM tarea t JOIN hito_equipo he "
                        + "ON he.id_hito_equipo = t.id_hito_equipo WHERE he.id_equipo = :e AND t.id_encargado = :u)")
                .param("e", idEquipo)
                .param("u", usuario.idUsuario())
                .query(Boolean.class)
                .single();
        if (tieneTareas) {
            throw new ReglaNegocioException(
                    "TIENE_TAREAS",
                    "Tienes tareas asignadas en este equipo. Pide al líder que las reasigne antes de salir");
        }
        boolean eraLider =
                apoyo.liderDe(idEquipo).map(l -> l == usuario.idUsuario()).orElse(false);
        jdbc.sql("DELETE FROM miembro_equipo WHERE id_equipo = :e AND id_usuario = :u")
                .param("e", idEquipo)
                .param("u", usuario.idUsuario())
                .update();
        if (eraLider) {
            auditoria.registrar(EntradaAuditoria.cambio(
                    usuario.idUsuario(),
                    "equipo",
                    idEquipo,
                    AccionAuditoria.ACTUALIZAR,
                    "lider",
                    String.valueOf(usuario.idUsuario()),
                    null));
        }
        auditoria.registrar(EntradaAuditoria.cambio(
                usuario.idUsuario(),
                "miembro_equipo",
                idEquipo,
                AccionAuditoria.ELIMINAR,
                "id_usuario",
                String.valueOf(usuario.idUsuario()),
                null));
        participacion.sincronizar(idProyecto, usuario.idUsuario());
    }

    /**
     * Reparte automáticamente a los participantes sin equipo entre los equipos con cupo disponible respetando el curso.
     * Solo el docente dueño.
     */
    @Transactional
    public ResultadoReparto repartir(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        apoyo.bloquearProyecto(idProyecto);
        record Candidato(long idUsuario, String email, String nombre, long idCurso, String nombreCurso) {}
        List<Candidato> candidatos = jdbc.sql(
                        "SELECT pp.id_usuario, u.email, u.nombre, pp.id_curso, c.nombre AS nombre_curso "
                                + "FROM participante_proyecto pp "
                                + "JOIN usuario u ON u.id_usuario = pp.id_usuario "
                                + "JOIN curso c ON c.id_curso = pp.id_curso "
                                + "WHERE pp.id_proyecto = :p AND u.fecha_anonimizacion IS NULL "
                                + "AND NOT EXISTS (SELECT 1 FROM miembro_equipo m JOIN equipo e "
                                + "ON e.id_equipo = m.id_equipo "
                                + "WHERE e.id_proyecto = pp.id_proyecto AND m.id_usuario = pp.id_usuario) "
                                + "ORDER BY u.nombre, u.id_usuario")
                .param("p", idProyecto)
                .query((rs, fila) -> new Candidato(
                        rs.getLong("id_usuario"),
                        rs.getString("email"),
                        rs.getString("nombre"),
                        rs.getLong("id_curso"),
                        rs.getString("nombre_curso")))
                .list();
        List<EquipoReparto> equiposConCupo = jdbc.sql("SELECT e.id_equipo, e.nombre_equipo, e.id_curso, e.cupo, "
                        + "(SELECT count(*) FROM miembro_equipo m WHERE m.id_equipo = e.id_equipo) AS integrantes "
                        + "FROM equipo e WHERE e.id_proyecto = :p AND e.cupo IS NOT NULL ORDER BY e.id_equipo")
                .param("p", idProyecto)
                .query((rs, fila) -> new EquipoReparto(
                        rs.getLong("id_equipo"),
                        rs.getString("nombre_equipo"),
                        rs.getObject("id_curso", Long.class),
                        rs.getInt("cupo"),
                        rs.getInt("integrantes")))
                .list();
        List<AsignacionRepartoVista> asignados = new ArrayList<>();
        List<EstudianteDisponibleVista> sinCupo = new ArrayList<>();
        for (Candidato c : candidatos) {
            EquipoReparto elegido = null;
            for (EquipoReparto eq : equiposConCupo) {
                if (eq.integrantes >= eq.cupo) {
                    continue;
                }
                if (eq.idCurso != null && eq.idCurso != c.idCurso()) {
                    continue;
                }
                if (elegido == null || eq.integrantes < elegido.integrantes) {
                    elegido = eq;
                }
            }
            if (elegido == null) {
                sinCupo.add(new EstudianteDisponibleVista(c.email(), c.nombre(), c.idCurso(), c.nombreCurso()));
            } else {
                apoyo.agregarIntegrante(usuario.idUsuario(), elegido.idEquipo, c.idUsuario());
                elegido.integrantes++;
                asignados.add(
                        new AsignacionRepartoVista(c.idUsuario(), c.nombre(), elegido.idEquipo, elegido.nombreEquipo));
            }
        }
        return new ResultadoReparto(asignados, sinCupo);
    }

    private boolean inscripcionAbierta(long idProyecto) {
        Instant cierre = jdbc.sql("SELECT cierre_equipos FROM proyecto WHERE id_proyecto = :p")
                .param("p", idProyecto)
                .query((rs, fila) ->
                        Optional.ofNullable(instante(rs.getObject("cierre_equipos", OffsetDateTime.class))))
                .single()
                .orElse(null);
        return estaAbierta(cierre);
    }

    private boolean estaAbierta(Instant cierre) {
        return cierre != null && Instant.now(reloj).isBefore(cierre);
    }

    private static Instant instante(OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }

    private static final class EquipoReparto {
        final long idEquipo;
        final String nombreEquipo;
        final Long idCurso;
        final int cupo;
        int integrantes;

        EquipoReparto(long idEquipo, String nombreEquipo, Long idCurso, int cupo, int integrantes) {
            this.idEquipo = idEquipo;
            this.nombreEquipo = nombreEquipo;
            this.idCurso = idCurso;
            this.cupo = cupo;
            this.integrantes = integrantes;
        }
    }
}
