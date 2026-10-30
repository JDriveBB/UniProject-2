package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.eventos.datos.ReferenciaTarea;
import co.uniproject.eventos.datos.TareaCorreccion;
import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.ejecucion.dominio.EstadoSolicitudCorreccion;
import co.uniproject.nucleo.ejecucion.dominio.EstadoTarea;
import co.uniproject.nucleo.ejecucion.dominio.SolicitudCorreccion;
import co.uniproject.nucleo.ejecucion.dominio.Tarea;
import co.uniproject.nucleo.ejecucion.repositorio.EvidenciaRepositorio;
import co.uniproject.nucleo.ejecucion.repositorio.SolicitudCorreccionRepositorio;
import co.uniproject.nucleo.ejecucion.repositorio.TareaRepositorio;
import co.uniproject.nucleo.notificaciones.RegistroEventos;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF5: el líder pide rehacer una entrega (decisiones 17b-9 a 17b-11). */
@Service
public class ServicioCorrecciones {

    private static final int AVANCE_TRAS_CORRECCION = 99;

    private static final String VISTA = "SELECT sc.id_solicitud, sc.id_tarea, sc.id_evidencia, sc.id_lider, "
            + "l.nombre AS lider, sc.id_estudiante, s.nombre AS estudiante, sc.observaciones, sc.estado, "
            + "sc.fecha_solicitud, sc.fecha_atencion FROM solicitud_correccion sc "
            + "JOIN usuario l ON l.id_usuario = sc.id_lider JOIN usuario s ON s.id_usuario = sc.id_estudiante ";

    private final TareaRepositorio tareas;
    private final EvidenciaRepositorio evidencias;
    private final SolicitudCorreccionRepositorio solicitudes;
    private final ConsultasEjecucion consultas;
    private final RecalculoTareas recalculo;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final RegistroEventos eventos;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioCorrecciones(TareaRepositorio tareas, EvidenciaRepositorio evidencias,
                                SolicitudCorreccionRepositorio solicitudes, ConsultasEjecucion consultas,
                                RecalculoTareas recalculo, Permisos permisos, RegistroAuditoria auditoria,
                                RegistroEventos eventos, JdbcClient jdbc, Clock reloj) {
        this.tareas = tareas;
        this.evidencias = evidencias;
        this.solicitudes = solicitudes;
        this.consultas = consultas;
        this.recalculo = recalculo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<CorreccionVista> listar(UsuarioAutenticado usuario, long idTarea) {
        permisos.exigir(permisos.puedeVerTarea(usuario.idUsuario(), idTarea));
        return jdbc.sql(VISTA + "WHERE sc.id_tarea = :tarea ORDER BY sc.fecha_solicitud DESC, sc.id_solicitud DESC")
                .param("tarea", idTarea)
                .query(ServicioCorrecciones::fila)
                .list();
    }

    @Transactional
    public CorreccionVista solicitar(UsuarioAutenticado usuario, long idTarea, PeticionCorreccion peticion) {
        permisos.exigir(permisos.puedeGestionarTarea(usuario.idUsuario(), idTarea));
        Tarea t = tareas.findById(idTarea).orElseThrow();
        ContextoHito hito = consultas.hito(t.getIdHitoEquipo()).orElseThrow();
        Instant ahora = Instant.now(reloj);
        EdicionHito.exigirAbierta(hito, ahora);
        evidencias.findById(peticion.idEvidencia())
                .filter(e -> e.getIdTarea() == idTarea)
                .orElseThrow(() -> new RecursoNoEncontradoException("Esa evidencia no es de esta tarea"));
        Long encargado = t.getIdEncargado();
        if (encargado == null) {
            throw new ReglaNegocioException("SIN_ENCARGADO", "La tarea no tiene encargado a quien pedirle la corrección");
        }
        if (consultas.correccionPendiente(idTarea)) {
            throw new ReglaNegocioException("CORRECCION_PENDIENTE", "Ya hay una corrección pendiente en esta tarea");
        }
        long id = usuario.idUsuario();
        SolicitudCorreccion sc = new SolicitudCorreccion();
        sc.setIdTarea(idTarea);
        sc.setIdEvidencia(peticion.idEvidencia());
        sc.setIdLider(id);
        sc.setIdEstudiante(encargado);
        sc.setObservaciones(peticion.observaciones().strip());
        sc.setEstado(EstadoSolicitudCorreccion.PENDIENTE);
        sc.setFechaSolicitud(ahora);
        sc = solicitudes.saveAndFlush(sc);
        auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idTarea, AccionAuditoria.SOLICITAR_CORRECCION,
                "solicitud_correccion", null, String.valueOf(sc.getIdSolicitud())));
        if (t.getEstado() == EstadoTarea.COMPLETADA) {
            auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idTarea, AccionAuditoria.ACTUALIZAR_AVANCE,
                    "porcentaje_avance", String.valueOf(t.getPorcentajeAvance()),
                    String.valueOf(AVANCE_TRAS_CORRECCION)));
            auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idTarea, AccionAuditoria.CAMBIAR_ESTADO,
                    "estado", EstadoTarea.COMPLETADA.name(), EstadoTarea.EN_PROGRESO.name()));
            t.setPorcentajeAvance(AVANCE_TRAS_CORRECCION);
            t.setEstado(EstadoTarea.EN_PROGRESO);
            t.setFechaCompletada(null);
            t.setFechaCambioEstado(ahora);
            tareas.saveAndFlush(t);
        }
        Map<Long, Persona> personas = consultas.personas(List.of(encargado, id));
        eventos.registrar(new TareaCorreccion(new Referencia(hito.idProyecto(), hito.nombreProyecto()),
                new Referencia(hito.idEquipo(), hito.nombreEquipo()), new ReferenciaTarea(idTarea, t.getTitulo()),
                personas.get(encargado), new Referencia(id, personas.get(id).nombre()), sc.getObservaciones()));
        recalculo.recalcular(t.getIdHitoEquipo());
        return jdbc.sql(VISTA + "WHERE sc.id_solicitud = :id")
                .param("id", sc.getIdSolicitud())
                .query(ServicioCorrecciones::fila)
                .single();
    }

    private static CorreccionVista fila(java.sql.ResultSet rs, int fila) throws java.sql.SQLException {
        return new CorreccionVista(rs.getLong("id_solicitud"), rs.getLong("id_tarea"), rs.getLong("id_evidencia"),
                rs.getLong("id_lider"), rs.getString("lider"), rs.getLong("id_estudiante"), rs.getString("estudiante"),
                rs.getString("observaciones"), EstadoSolicitudCorreccion.valueOf(rs.getString("estado")),
                LectorTareas.instante(rs, "fecha_solicitud"), LectorTareas.instante(rs, "fecha_atencion"));
    }
}
