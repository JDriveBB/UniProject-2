package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.configuracionacademica.servicio.Textos;
import co.uniproject.nucleo.ejecucion.dominio.EstadoTarea;
import co.uniproject.nucleo.ejecucion.dominio.RegistroAvance;
import co.uniproject.nucleo.ejecucion.dominio.Tarea;
import co.uniproject.nucleo.ejecucion.repositorio.RegistroAvanceRepositorio;
import co.uniproject.nucleo.ejecucion.repositorio.TareaRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF12 (avance), RF13 (encargado o líder) y entrega1 §2.5 (una tarea bloqueada no avanza). */
@Service
public class ServicioAvance {

    private static final String PREVIAS_INCOMPLETAS = "SELECT p.titulo FROM dependencia_tarea d "
            + "JOIN tarea p ON p.id_tarea = d.id_tarea_predecesora WHERE d.id_tarea_dependiente = :tarea "
            + "AND p.estado <> 'COMPLETADA' ORDER BY p.id_tarea";

    private final TareaRepositorio tareas;
    private final RegistroAvanceRepositorio registros;
    private final ConsultasEjecucion consultas;
    private final LectorTareas lector;
    private final RecalculoTareas recalculo;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioAvance(TareaRepositorio tareas, RegistroAvanceRepositorio registros, ConsultasEjecucion consultas,
                          LectorTareas lector, RecalculoTareas recalculo, Permisos permisos,
                          RegistroAuditoria auditoria, JdbcClient jdbc, Clock reloj) {
        this.tareas = tareas;
        this.registros = registros;
        this.consultas = consultas;
        this.lector = lector;
        this.recalculo = recalculo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional
    public TareaVista actualizar(UsuarioAutenticado usuario, long idTarea, SolicitudAvance solicitud) {
        permisos.exigir(permisos.puedeActualizarAvance(usuario.idUsuario(), idTarea));
        Tarea t = tareas.findById(idTarea).orElseThrow();
        ContextoHito hito = consultas.hito(t.getIdHitoEquipo()).orElseThrow();
        Instant ahora = Instant.now(reloj);
        EdicionHito.exigirAbierta(hito, ahora);
        List<String> previas = jdbc.sql(PREVIAS_INCOMPLETAS).param("tarea", idTarea).query(String.class).list();
        if (!previas.isEmpty()) {
            throw new ReglaNegocioException("TAREA_BLOQUEADA", "La tarea espera prerrequisitos sin terminar", previas);
        }
        int nuevo = solicitud.porcentajeAvance();
        if (nuevo == 100 && consultas.correccionPendiente(idTarea)) {
            throw new ReglaNegocioException("CORRECCION_PENDIENTE",
                    "Adjunta la nueva versión de la evidencia antes de completar la tarea");
        }
        long id = usuario.idUsuario();
        int anterior = t.getPorcentajeAvance();
        EstadoTarea estadoAnterior = t.getEstado();
        EstadoTarea estado = estadoPara(nuevo);
        if (anterior != nuevo) {
            auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idTarea, AccionAuditoria.ACTUALIZAR_AVANCE,
                    "porcentaje_avance", String.valueOf(anterior), String.valueOf(nuevo)));
        }
        if (estado != estadoAnterior) {
            t.setEstado(estado);
            t.setFechaCambioEstado(ahora);
            t.setFechaCompletada(estado == EstadoTarea.COMPLETADA ? ahora : null);
            auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idTarea, AccionAuditoria.CAMBIAR_ESTADO,
                    "estado", estadoAnterior.name(), estado.name()));
        }
        t.setPorcentajeAvance(nuevo);
        tareas.saveAndFlush(t);
        RegistroAvance r = new RegistroAvance();
        r.setIdTarea(idTarea);
        r.setIdUsuario(id);
        r.setFechaHora(ahora);
        r.setPorcentajeAvance(nuevo);
        r.setObservacion(Textos.opcional(solicitud.observacion()));
        registros.saveAndFlush(r);
        recalculo.recalcular(t.getIdHitoEquipo());
        return lector.una(idTarea);
    }

    /** Decisión 17a-6. */
    static EstadoTarea estadoPara(int porcentaje) {
        if (porcentaje == 100) {
            return EstadoTarea.COMPLETADA;
        }
        return porcentaje == 0 ? EstadoTarea.PENDIENTE : EstadoTarea.EN_PROGRESO;
    }
}
