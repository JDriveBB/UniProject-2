package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.cursos.CursoResumen;
import co.uniproject.nucleo.configuracionacademica.cursos.ParticipacionCursos;
import co.uniproject.nucleo.configuracionacademica.cursos.ServicioCursos;
import co.uniproject.nucleo.configuracionacademica.dominio.Proyecto;
import co.uniproject.nucleo.configuracionacademica.repositorio.CriterioRubricaRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.EquipoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.HitoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.ProyectoRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF1: proyectos y cursos asignados. Ver: Permisos.puedeVerProyecto. Crear: rol DOCENTE. Cambiar: solo su docente. */
@Service
public class ServicioProyectos {

    /** Misma regla que PermisosJdbc.puedeVerProyecto, para listar. */
    private static final String VISIBLES = "SELECT p.id_proyecto FROM proyecto p WHERE p.id_docente = :usuario "
            + "OR (p.usa_monitores AND EXISTS (SELECT 1 FROM monitor_proyecto mp WHERE mp.id_proyecto = p.id_proyecto "
            + "AND mp.id_monitor = :usuario)) "
            + "OR EXISTS (SELECT 1 FROM participante_proyecto pp WHERE pp.id_proyecto = p.id_proyecto "
            + "AND pp.id_usuario = :usuario) "
            + "OR EXISTS (SELECT 1 FROM equipo e JOIN miembro_equipo m "
            + "ON m.id_equipo = e.id_equipo WHERE e.id_proyecto = p.id_proyecto AND m.id_usuario = :usuario) "
            + "ORDER BY p.fecha_inicio DESC, p.id_proyecto";

    private final ProyectoRepositorio proyectos;
    private final HitoRepositorio hitos;
    private final CriterioRubricaRepositorio criterios;
    private final EquipoRepositorio equipos;
    private final ApoyoConfiguracion apoyo;
    private final ParticipacionCursos participacion;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;

    public ServicioProyectos(
            ProyectoRepositorio proyectos,
            HitoRepositorio hitos,
            CriterioRubricaRepositorio criterios,
            EquipoRepositorio equipos,
            ApoyoConfiguracion apoyo,
            ParticipacionCursos participacion,
            Permisos permisos,
            RegistroAuditoria auditoria,
            JdbcClient jdbc) {
        this.proyectos = proyectos;
        this.hitos = hitos;
        this.criterios = criterios;
        this.equipos = equipos;
        this.apoyo = apoyo;
        this.participacion = participacion;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
    }

    @Transactional
    public ProyectoVista crear(UsuarioAutenticado usuario, SolicitudProyecto solicitud) {
        permisos.exigir(usuario.rol() == RolPlataforma.DOCENTE);
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Proyecto p = new Proyecto();
        p.setIdDocente(usuario.idUsuario());
        p.setEquiposMixtos(Boolean.TRUE.equals(solicitud.equiposMixtos()));
        p.setHorasRonda(Proyecto.HORAS_RONDA_POR_DEFECTO);
        aplicar(p, solicitud);
        p = proyectos.saveAndFlush(p);
        auditoria.registrar(
                EntradaAuditoria.de(usuario.idUsuario(), "proyecto", p.getIdProyecto(), AccionAuditoria.CREAR));
        return ProyectoVista.de(p);
    }

    @Transactional(readOnly = true)
    public List<ProyectoVista> listar(UsuarioAutenticado usuario) {
        List<Long> ids = jdbc.sql(VISIBLES)
                .param("usuario", usuario.idUsuario())
                .query(Long.class)
                .list();
        Map<Long, Proyecto> porId = proyectos.findAllById(ids).stream()
                .collect(Collectors.toMap(Proyecto::getIdProyecto, Function.identity()));
        return ids.stream().map(porId::get).map(ProyectoVista::de).toList();
    }

    @Transactional(readOnly = true)
    public ProyectoDetalle detalle(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.puedeVerProyecto(usuario.idUsuario(), idProyecto));
        Proyecto p = buscar(idProyecto);
        List<HitoVista> vistasHitos = hitos.findByIdProyectoOrderByOrden(idProyecto).stream()
                .map(h -> HitoVista.de(h, criterios.findByIdHitoOrderByIdCriterio(h.getIdHito())))
                .toList();
        List<EquipoVista> vistasEquipos = equipos.findByIdProyectoOrderByNombreEquipo(idProyecto).stream()
                .map(apoyo::vistaEquipo)
                .toList();
        return new ProyectoDetalle(ProyectoVista.de(p), vistasHitos, vistasEquipos);
    }

    @Transactional
    public ProyectoVista actualizar(UsuarioAutenticado usuario, long idProyecto, SolicitudProyecto solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Proyecto p = buscar(idProyecto);
        if (solicitud.equiposMixtos() != null
                && !solicitud.equiposMixtos().equals(Boolean.TRUE.equals(p.getEquiposMixtos()))) {
            throw new ReglaNegocioException(
                    "EQUIPOS_MIXTOS_INMUTABLE", "Los equipos mixtos solo se eligen al crear el proyecto");
        }
        boolean hitosFuera = jdbc.sql("SELECT EXISTS (SELECT 1 FROM hito WHERE id_proyecto = :proyecto "
                        + "AND (fecha_inicio < :inicio OR fecha_limite > :fin))")
                .param("proyecto", idProyecto)
                .param("inicio", solicitud.fechaInicio())
                .param("fin", solicitud.fechaFin())
                .query(Boolean.class)
                .single();
        if (hitosFuera) {
            throw new ReglaNegocioException(
                    "HITOS_FUERA_DE_FECHAS",
                    "Hay hitos fuera de las nuevas fechas del proyecto. Ajusta primero sus fechas");
        }
        long id = usuario.idUsuario();
        AuditoriaCambios.siCambia(
                auditoria,
                id,
                "proyecto",
                idProyecto,
                "nombre",
                p.getNombre(),
                solicitud.nombre().strip());
        AuditoriaCambios.siCambia(
                auditoria,
                id,
                "proyecto",
                idProyecto,
                "descripcion",
                p.getDescripcion(),
                Textos.opcional(solicitud.descripcion()));
        AuditoriaCambios.siCambia(
                auditoria, id, "proyecto", idProyecto, "fecha_inicio", p.getFechaInicio(), solicitud.fechaInicio());
        AuditoriaCambios.siCambia(
                auditoria, id, "proyecto", idProyecto, "fecha_fin", p.getFechaFin(), solicitud.fechaFin());
        AuditoriaCambios.siCambia(
                auditoria, id, "proyecto", idProyecto, "usa_monitores", p.getUsaMonitores(), solicitud.usaMonitores());
        aplicar(p, solicitud);
        return ProyectoVista.de(proyectos.save(p));
    }

    /** Cursos asignados al proyecto en orden ascendente de id_curso. Solo el docente dueño. */
    @Transactional(readOnly = true)
    public List<CursoResumen> cursos(UsuarioAutenticado usuario, long idProyecto) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        return jdbc.sql("SELECT c.id_curso, c.nombre, c.periodo, c.archivado_en IS NOT NULL AS archivado "
                        + "FROM curso_proyecto cp JOIN curso c ON c.id_curso = cp.id_curso "
                        + "WHERE cp.id_proyecto = :p ORDER BY c.id_curso")
                .param("p", idProyecto)
                .query((rs, fila) -> new CursoResumen(
                        rs.getLong("id_curso"),
                        rs.getString("nombre"),
                        rs.getString("periodo"),
                        rs.getBoolean("archivado")))
                .list();
    }

    /**
     * Asigna el conjunto de cursos al proyecto. Con 2 o más cursos distintos de los actuales y confirmar=false devuelve
     * vista previa con los estudiantes repetidos sin escribir ni auditar.
     */
    @Transactional
    public ResultadoCursosProyecto asignarCursos(
            UsuarioAutenticado usuario, long idProyecto, SolicitudCursosProyecto solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        Set<Long> pedidos = new TreeSet<>(solicitud.idsCursos());
        Set<Long> actuales = new TreeSet<>(cursosAsignadosIds(idProyecto));
        for (long idCurso : pedidos) {
            permisos.exigir(permisos.esDocenteDelCurso(usuario.idUsuario(), idCurso));
            if (!actuales.contains(idCurso) && estaArchivado(idCurso)) {
                throw ServicioCursos.cursoArchivado();
            }
        }
        if (pedidos.size() >= 2 && !pedidos.equals(actuales) && !solicitud.confirmar()) {
            return new ResultadoCursosProyecto(false, resumenesCursos(pedidos), repetidos(idProyecto, pedidos));
        }
        Set<Long> todos = new TreeSet<>(actuales);
        todos.addAll(pedidos);
        for (long idCurso : todos) {
            participacion.bloquearCurso(idCurso);
        }
        jdbc.sql("SELECT id_proyecto FROM proyecto WHERE id_proyecto = :p FOR NO KEY UPDATE")
                .param("p", idProyecto)
                .query(Long.class)
                .single();
        Set<Long> actualesBloqueados = new TreeSet<>(cursosAsignadosIds(idProyecto));
        for (long idCurso : actualesBloqueados) {
            if (!pedidos.contains(idCurso)) {
                jdbc.sql("DELETE FROM curso_proyecto WHERE id_curso = :c AND id_proyecto = :p")
                        .param("c", idCurso)
                        .param("p", idProyecto)
                        .update();
            }
        }
        for (long idCurso : pedidos) {
            if (!actualesBloqueados.contains(idCurso)) {
                jdbc.sql("INSERT INTO curso_proyecto (id_curso, id_proyecto) VALUES (:c, :p) ON CONFLICT DO NOTHING")
                        .param("c", idCurso)
                        .param("p", idProyecto)
                        .update();
            }
        }
        participacion.sincronizarProyecto(idProyecto);
        if (!actualesBloqueados.equals(pedidos)) {
            auditoria.registrar(EntradaAuditoria.cambio(
                    usuario.idUsuario(),
                    "proyecto",
                    idProyecto,
                    AccionAuditoria.ACTUALIZAR,
                    "cursos",
                    csvOpcional(actualesBloqueados),
                    csvOpcional(pedidos)));
        }
        return new ResultadoCursosProyecto(true, resumenesCursos(pedidos), repetidos(idProyecto, pedidos));
    }

    /** Elige con qué curso cuenta un estudiante participante en el proyecto. Solo el docente dueño. */
    @Transactional
    public EstudianteProyectoVista elegirCurso(
            UsuarioAutenticado usuario, long idProyecto, long idUsuario, SolicitudCursoEstudiante solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        jdbc.sql("SELECT id_proyecto FROM proyecto WHERE id_proyecto = :p FOR NO KEY UPDATE")
                .param("p", idProyecto)
                .query(Long.class)
                .single();
        long anterior = jdbc.sql(
                        "SELECT id_curso FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u FOR UPDATE")
                .param("p", idProyecto)
                .param("u", idUsuario)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new RecursoNoEncontradoException("Ese estudiante no participa en el proyecto"));
        long nuevo = solicitud.idCurso();
        if (!participacion.cursosValidos(idProyecto, idUsuario).contains(nuevo)) {
            throw new ReglaNegocioException("CURSO_NO_VALIDO", "Ese estudiante no está en ese curso del proyecto");
        }
        Optional<Long> cursoEquipo = jdbc.sql(
                        "SELECT e.id_curso FROM miembro_equipo m JOIN equipo e ON e.id_equipo = m.id_equipo "
                                + "WHERE e.id_proyecto = :p AND m.id_usuario = :u AND e.id_curso IS NOT NULL LIMIT 1")
                .param("p", idProyecto)
                .param("u", idUsuario)
                .query(Long.class)
                .optional();
        if (cursoEquipo.isPresent() && cursoEquipo.get() != nuevo) {
            throw new ReglaNegocioException("EQUIPO_DE_OTRO_CURSO", "Ese estudiante está en un equipo de otro curso");
        }
        if (anterior != nuevo) {
            jdbc.sql("UPDATE participante_proyecto SET id_curso = :c WHERE id_proyecto = :p AND id_usuario = :u")
                    .param("c", nuevo)
                    .param("p", idProyecto)
                    .param("u", idUsuario)
                    .update();
            auditoria.registrar(EntradaAuditoria.cambio(
                    usuario.idUsuario(),
                    "proyecto",
                    idProyecto,
                    AccionAuditoria.ACTUALIZAR,
                    "curso_estudiante",
                    idUsuario + ":" + anterior,
                    idUsuario + ":" + nuevo));
        }
        return jdbc.sql("SELECT pp.id_usuario, u.email, u.nombre, pp.id_curso, c.nombre AS nombre_curso, "
                        + "(SELECT m.id_equipo FROM miembro_equipo m JOIN equipo e ON e.id_equipo = m.id_equipo "
                        + "WHERE e.id_proyecto = pp.id_proyecto AND m.id_usuario = pp.id_usuario LIMIT 1) AS id_equipo "
                        + "FROM participante_proyecto pp JOIN usuario u ON u.id_usuario = pp.id_usuario "
                        + "JOIN curso c ON c.id_curso = pp.id_curso "
                        + "WHERE pp.id_proyecto = :p AND pp.id_usuario = :u")
                .param("p", idProyecto)
                .param("u", idUsuario)
                .query((rs, fila) -> new EstudianteProyectoVista(
                        rs.getLong("id_usuario"),
                        rs.getString("email"),
                        rs.getString("nombre"),
                        rs.getLong("id_curso"),
                        rs.getString("nombre_curso"),
                        rs.getObject("id_equipo", Long.class)))
                .single();
    }

    /** Fin no anterior al inicio (proyecto y hito). */
    static void validarFechas(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new SolicitudInvalidaException(
                    "FECHAS_INVALIDAS", "La fecha final no puede ser anterior a la inicial");
        }
    }

    private Proyecto buscar(long idProyecto) {
        return proyectos
                .findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("El proyecto no existe"));
    }

    private List<Long> cursosAsignadosIds(long idProyecto) {
        return jdbc.sql("SELECT id_curso FROM curso_proyecto WHERE id_proyecto = :p ORDER BY id_curso")
                .param("p", idProyecto)
                .query(Long.class)
                .list();
    }

    private boolean estaArchivado(long idCurso) {
        return jdbc.sql("SELECT archivado_en IS NOT NULL FROM curso WHERE id_curso = :c")
                .param("c", idCurso)
                .query(Boolean.class)
                .single();
    }

    private List<CursoResumen> resumenesCursos(Set<Long> idsCursos) {
        if (idsCursos.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT id_curso, nombre, periodo, archivado_en IS NOT NULL AS archivado "
                        + "FROM curso WHERE id_curso IN (:ids) ORDER BY id_curso")
                .param("ids", idsCursos)
                .query((rs, fila) -> new CursoResumen(
                        rs.getLong("id_curso"),
                        rs.getString("nombre"),
                        rs.getString("periodo"),
                        rs.getBoolean("archivado")))
                .list();
    }

    private List<EstudianteRepetidoVista> repetidos(long idProyecto, Set<Long> idsCursos) {
        if (idsCursos.size() < 2) {
            return List.of();
        }
        record FilaRepetido(long idUsuario, String nombre, String email, CursoResumen curso, Long cursoActual) {}
        List<FilaRepetido> filas = jdbc.sql(
                        "SELECT u.id_usuario, u.nombre, u.email, c.id_curso, c.nombre AS nombre_curso, c.periodo, "
                                + "c.archivado_en IS NOT NULL AS archivado, pp.id_curso AS curso_actual "
                                + "FROM miembro_curso mc "
                                + "JOIN usuario u ON u.id_usuario = mc.id_usuario "
                                + "JOIN curso c ON c.id_curso = mc.id_curso "
                                + "LEFT JOIN participante_proyecto pp ON pp.id_proyecto = :p AND pp.id_usuario = u.id_usuario "
                                + "WHERE mc.id_curso IN (:ids) AND u.fecha_anonimizacion IS NULL "
                                + "ORDER BY u.nombre, u.email, u.id_usuario, c.id_curso")
                .param("p", idProyecto)
                .param("ids", idsCursos)
                .query((rs, fila) -> new FilaRepetido(
                        rs.getLong("id_usuario"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        new CursoResumen(
                                rs.getLong("id_curso"),
                                rs.getString("nombre_curso"),
                                rs.getString("periodo"),
                                rs.getBoolean("archivado")),
                        rs.getObject("curso_actual", Long.class)))
                .list();
        Map<Long, List<FilaRepetido>> porUsuario = new LinkedHashMap<>();
        for (FilaRepetido f : filas) {
            porUsuario.computeIfAbsent(f.idUsuario(), k -> new ArrayList<>()).add(f);
        }
        List<EstudianteRepetidoVista> resultado = new ArrayList<>();
        for (List<FilaRepetido> grupo : porUsuario.values()) {
            if (grupo.size() < 2) {
                continue;
            }
            FilaRepetido primero = grupo.getFirst();
            List<CursoResumen> cursos = grupo.stream().map(FilaRepetido::curso).toList();
            Long actual = primero.cursoActual();
            long cuenta = actual != null && idsCursos.contains(actual)
                    ? actual
                    : cursos.getFirst().idCurso();
            resultado.add(new EstudianteRepetidoVista(
                    primero.idUsuario(), primero.nombre(), primero.email(), cursos, cuenta));
        }
        return resultado;
    }

    private static String csvOpcional(Set<Long> ids) {
        if (ids.isEmpty()) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static void aplicar(Proyecto p, SolicitudProyecto s) {
        p.setNombre(s.nombre().strip());
        p.setDescripcion(Textos.opcional(s.descripcion()));
        p.setFechaInicio(s.fechaInicio());
        p.setFechaFin(s.fechaFin());
        p.setUsaMonitores(s.usaMonitores());
    }
}
