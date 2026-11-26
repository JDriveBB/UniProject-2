package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.eventos.datos.MovimientoEquipo;
import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.avisos.AvisosConfiguracion;
import co.uniproject.nucleo.configuracionacademica.cursos.ParticipacionCursos;
import co.uniproject.nucleo.configuracionacademica.dominio.Equipo;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipoId;
import co.uniproject.nucleo.configuracionacademica.repositorio.EquipoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.MiembroEquipoRepositorio;
import co.uniproject.nucleo.configuracionacademica.votacion.OrigenRonda;
import co.uniproject.nucleo.configuracionacademica.votacion.RondaLiderFila;
import co.uniproject.nucleo.configuracionacademica.votacion.RondasLider;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Equipos del proyecto, sus cupos, sus miembros (participantes por curso) y su líder (supuesto 2 de entrega1: uno por
 * equipo). Solo el docente dueño.
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
    private final RondasLider rondas;
    private final AvisosConfiguracion avisos;
    private final Clock reloj;

    public ServicioEquipos(
            EquipoRepositorio equipos,
            MiembroEquipoRepositorio miembros,
            ApoyoConfiguracion apoyo,
            ParticipacionCursos participacion,
            Permisos permisos,
            RegistroAuditoria auditoria,
            JdbcClient jdbc,
            RondasLider rondas,
            AvisosConfiguracion avisos,
            Clock reloj) {
        this.equipos = equipos;
        this.miembros = miembros;
        this.apoyo = apoyo;
        this.participacion = participacion;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.rondas = rondas;
        this.avisos = avisos;
        this.reloj = reloj;
    }

    @Transactional
    public EquipoVista crear(UsuarioAutenticado usuario, long idProyecto, SolicitudEquipo solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        apoyo.bloquearProyecto(idProyecto);
        Long idCurso = resolverCurso(idProyecto, solicitud.idCurso());
        String nombre = solicitud.nombreEquipo().strip();
        boolean repetido = equipos.findByIdProyectoOrderByNombreEquipo(idProyecto).stream()
                .anyMatch(x -> x.getNombreEquipo().equalsIgnoreCase(nombre));
        if (repetido) {
            throw equipoRepetido();
        }
        Equipo equipo = new Equipo();
        equipo.setIdProyecto(idProyecto);
        equipo.setNombreEquipo(nombre);
        equipo.setIdCurso(idCurso);
        equipo.setCupo(solicitud.cupo());
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

    /** Crea varios equipos con el mismo curso y cupo en orden de numeración. */
    @Transactional
    public List<EquipoVista> crearEnLote(UsuarioAutenticado usuario, long idProyecto, SolicitudEquiposLote solicitud) {
        permisos.exigir(permisos.esDocenteDelProyecto(usuario.idUsuario(), idProyecto));
        apoyo.bloquearProyecto(idProyecto);
        Long idCurso = resolverCurso(idProyecto, solicitud.idCurso());
        int cupo = solicitud.cupo();
        int cantidad;
        if (solicitud.cantidad() != null) {
            cantidad = solicitud.cantidad();
        } else {
            int sinEquipo = contarSinEquipo(idProyecto, idCurso);
            if (sinEquipo == 0) {
                throw new ReglaNegocioException(
                        "SIN_ESTUDIANTES_SIN_EQUIPO",
                        "No hay estudiantes sin equipo para calcular cuántos equipos crear");
            }
            cantidad = (sinEquipo + cupo - 1) / cupo;
            if (cantidad > 50) {
                throw new ReglaNegocioException(
                        "DEMASIADOS_EQUIPOS", "Serían más de 50 equipos. Sube el cupo o indica la cantidad");
            }
        }
        String prefijo = solicitud.prefijo().strip();
        List<String> nombres = new ArrayList<>(cantidad);
        for (int k = 1; k <= cantidad; k++) {
            nombres.add(prefijo + " " + k);
        }
        Set<String> existentes = equipos.findByIdProyectoOrderByNombreEquipo(idProyecto).stream()
                .map(x -> x.getNombreEquipo().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (String nombre : nombres) {
            if (existentes.contains(nombre.toLowerCase(Locale.ROOT))) {
                throw equipoRepetido();
            }
        }
        List<Equipo> creados = new ArrayList<>(cantidad);
        for (String nombre : nombres) {
            Equipo equipo = new Equipo();
            equipo.setIdProyecto(idProyecto);
            equipo.setNombreEquipo(nombre);
            equipo.setIdCurso(idCurso);
            equipo.setCupo(cupo);
            try {
                equipo = equipos.saveAndFlush(equipo);
            } catch (DataIntegrityViolationException e) {
                throw equipoRepetido();
            }
            auditoria.registrar(
                    EntradaAuditoria.de(usuario.idUsuario(), "equipo", equipo.getIdEquipo(), AccionAuditoria.CREAR));
            creados.add(equipo);
        }
        apoyo.completarHitosEquipo(idProyecto);
        return creados.stream().map(apoyo::vistaEquipo).toList();
    }

    /** Cambia el cupo del equipo (null lo convierte en manual). Se permite un cupo menor que los integrantes actuales. */
    @Transactional
    public EquipoVista cambiarCupo(UsuarioAutenticado usuario, long idEquipo, SolicitudCupo solicitud) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        apoyo.bloquearProyecto(equipo.getIdProyecto());
        apoyo.bloquearEquipo(idEquipo);
        Integer anterior = equipo.getCupo();
        Integer nuevo = solicitud.cupo();
        if (!Objects.equals(anterior, nuevo)) {
            equipo.setCupo(nuevo);
            equipo = equipos.saveAndFlush(equipo);
            AuditoriaCambios.siCambia(auditoria, usuario.idUsuario(), "equipo", idEquipo, "cupo", anterior, nuevo);
        }
        return apoyo.vistaEquipo(equipo);
    }

    /** Primero se revisa la participación en el proyecto: así no se revela si un correo cualquiera tiene cuenta. */
    @Transactional
    public EquipoVista agregarMiembro(UsuarioAutenticado usuario, long idEquipo, SolicitudMiembro solicitud) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        apoyo.bloquearProyecto(equipo.getIdProyecto());
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
        if (apoyo.tieneEquipoEnProyecto(equipo.getIdProyecto(), idEstudiante)) {
            throw new ReglaNegocioException(
                    "YA_TIENE_EQUIPO", "Ese estudiante ya pertenece a un equipo de este proyecto");
        }
        if (equipo.getIdCurso() != null) {
            Long cursoParticipante = apoyo.cursoDelParticipante(equipo.getIdProyecto(), idEstudiante)
                    .orElse(null);
            if (!Objects.equals(equipo.getIdCurso(), cursoParticipante)) {
                throw new ReglaNegocioException(
                        "CURSO_DEL_EQUIPO", "Ese estudiante cuenta con otro curso en este proyecto");
            }
        }
        if (equipo.getCupo() != null
                && apoyo.contarIntegrantes(idEquipo) >= equipo.getCupo()
                && !Boolean.TRUE.equals(solicitud.pasarCupo())) {
            throw new ReglaNegocioException(
                    "CUPO_LLENO", "El equipo ya tiene su cupo completo. Confírmalo para pasar el cupo");
        }
        apoyo.agregarIntegrante(usuario.idUsuario(), idEquipo, idEstudiante);
        avisos.miembroCambio(idEquipo, MovimientoEquipo.ENTRO, true, List.of(idEstudiante));
        return apoyo.vistaEquipo(equipo);
    }

    /** Sus tareas no cambian: el líder las reasigna (RF11, bloque 18). */
    @Transactional
    public EquipoVista quitarMiembro(UsuarioAutenticado usuario, long idEquipo, long idUsuario) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        apoyo.bloquearProyecto(equipo.getIdProyecto());
        apoyo.bloquearEquipo(idEquipo);
        MiembroEquipoId id = new MiembroEquipoId(idEquipo, idUsuario);
        if (!miembros.existsById(id)) {
            throw new RecursoNoEncontradoException("Esa persona no es miembro del equipo");
        }
        boolean eraLider = apoyo.liderDe(idEquipo).map(l -> l == idUsuario).orElse(false);
        miembros.deleteById(id);
        miembros.flush();
        if (eraLider) {
            auditoria.registrar(EntradaAuditoria.cambio(
                    usuario.idUsuario(),
                    "equipo",
                    idEquipo,
                    AccionAuditoria.ACTUALIZAR,
                    "lider",
                    String.valueOf(idUsuario),
                    null));
        }
        auditoria.registrar(EntradaAuditoria.cambio(
                usuario.idUsuario(),
                "miembro_equipo",
                idEquipo,
                AccionAuditoria.ELIMINAR,
                "id_usuario",
                String.valueOf(idUsuario),
                null));
        participacion.sincronizar(equipo.getIdProyecto(), idUsuario);
        avisos.miembroCambio(idEquipo, MovimientoEquipo.SALIO, true, List.of(idUsuario));
        return apoyo.vistaEquipo(equipo);
    }

    /**
     * Nombra líder a un integrante del equipo; el anterior deja de ser líder. Decisión 15-8: dos UPDATE en orden por
     * el índice único parcial (un líder por equipo). Si hay una votación sin cerrar, se cancela.
     */
    @Transactional
    public EquipoVista cambiarLider(UsuarioAutenticado usuario, long idEquipo, SolicitudLider solicitud) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        apoyo.bloquearEquipo(idEquipo);
        long nuevo = solicitud.idUsuario();
        if (!permisos.esIntegranteDelEquipo(nuevo, idEquipo)) {
            throw new ReglaNegocioException("NO_ES_MIEMBRO", "El líder debe ser miembro del equipo");
        }
        rondas.cancelarSinCerrar(usuario.idUsuario(), idEquipo, Instant.now(reloj));
        apoyo.nombrarLider(usuario.idUsuario(), idEquipo, nuevo);
        return apoyo.vistaEquipo(equipo);
    }

    /**
     * Destituye al líder del equipo; el equipo queda sin líder, con sus acciones de líder bloqueadas, y con una
     * votación abierta hasta que se elija o el docente nombre a otro.
     */
    @Transactional
    public EquipoVista destituir(UsuarioAutenticado usuario, long idEquipo) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        apoyo.bloquearEquipo(idEquipo);
        long anterior = apoyo.liderDe(idEquipo)
                .orElseThrow(() -> new ReglaNegocioException("SIN_LIDER", "El equipo no tiene líder"));
        jdbc.sql("UPDATE miembro_equipo SET rol_equipo = 'INTEGRANTE' WHERE id_equipo = :equipo "
                        + "AND rol_equipo = 'LIDER'")
                .param("equipo", idEquipo)
                .update();
        auditoria.registrar(EntradaAuditoria.cambio(
                usuario.idUsuario(),
                "equipo",
                idEquipo,
                AccionAuditoria.ACTUALIZAR,
                "lider",
                String.valueOf(anterior),
                null));
        Instant ahora = Instant.now(reloj);
        Optional<RondaLiderFila> fila = rondas.sinCerrar(idEquipo);
        if (fila.isPresent() && fila.get().vencida(ahora)) {
            rondas.cancelarSinCerrar(usuario.idUsuario(), idEquipo, ahora);
        }
        if (rondas.sinCerrar(idEquipo).isEmpty()) {
            rondas.abrir(usuario.idUsuario(), idEquipo, OrigenRonda.DEMANDA, ahora);
        }
        return apoyo.vistaEquipo(equipo);
    }

    private Long resolverCurso(long idProyecto, Long idCurso) {
        boolean mixto = jdbc.sql("SELECT equipos_mixtos FROM proyecto WHERE id_proyecto = :p")
                .param("p", idProyecto)
                .query(Boolean.class)
                .single();
        if (mixto) {
            if (idCurso != null) {
                throw new SolicitudInvalidaException(
                        "CURSO_EN_PROYECTO_MIXTO", "En un proyecto con equipos mixtos los equipos no tienen curso");
            }
            return null;
        }
        if (idCurso != null) {
            boolean asignado = jdbc.sql(
                            "SELECT EXISTS (SELECT 1 FROM curso_proyecto WHERE id_proyecto = :p AND id_curso = :c)")
                    .param("p", idProyecto)
                    .param("c", idCurso)
                    .query(Boolean.class)
                    .single();
            if (!asignado) {
                throw new ReglaNegocioException("CURSO_NO_ASIGNADO", "Ese curso no está asignado al proyecto");
            }
            return idCurso;
        }
        List<Long> cursos = jdbc.sql("SELECT id_curso FROM curso_proyecto WHERE id_proyecto = :p ORDER BY id_curso")
                .param("p", idProyecto)
                .query(Long.class)
                .list();
        if (cursos.isEmpty()) {
            throw new ReglaNegocioException("PROYECTO_SIN_CURSOS", "Asigna primero un curso al proyecto");
        }
        if (cursos.size() == 1) {
            return cursos.getFirst();
        }
        throw new SolicitudInvalidaException("CURSO_REQUERIDO", "Elige el curso del equipo");
    }

    private int contarSinEquipo(long idProyecto, Long idCurso) {
        if (idCurso == null) {
            return jdbc.sql("SELECT count(*) FROM participante_proyecto pp "
                            + "JOIN usuario u ON u.id_usuario = pp.id_usuario "
                            + "WHERE pp.id_proyecto = :p AND u.fecha_anonimizacion IS NULL "
                            + "AND NOT EXISTS (SELECT 1 FROM miembro_equipo m JOIN equipo e "
                            + "ON e.id_equipo = m.id_equipo "
                            + "WHERE e.id_proyecto = pp.id_proyecto AND m.id_usuario = pp.id_usuario)")
                    .param("p", idProyecto)
                    .query(Integer.class)
                    .single();
        }
        return jdbc.sql("SELECT count(*) FROM participante_proyecto pp "
                        + "JOIN usuario u ON u.id_usuario = pp.id_usuario "
                        + "WHERE pp.id_proyecto = :p AND pp.id_curso = :c AND u.fecha_anonimizacion IS NULL "
                        + "AND NOT EXISTS (SELECT 1 FROM miembro_equipo m JOIN equipo e "
                        + "ON e.id_equipo = m.id_equipo "
                        + "WHERE e.id_proyecto = pp.id_proyecto AND m.id_usuario = pp.id_usuario)")
                .param("p", idProyecto)
                .param("c", idCurso)
                .query(Integer.class)
                .single();
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
