package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.eventos.datos.ReferenciaTarea;
import co.uniproject.eventos.datos.TareaReasignada;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.servicio.AuditoriaCambios;
import co.uniproject.nucleo.configuracionacademica.servicio.Textos;
import co.uniproject.nucleo.ejecucion.dominio.EstadoTarea;
import co.uniproject.nucleo.ejecucion.dominio.Tarea;
import co.uniproject.nucleo.ejecucion.repositorio.TareaRepositorio;
import co.uniproject.nucleo.notificaciones.RegistroEventos;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF11: reasignación total o parcial por el líder (decisiones 18-1 a 18-5). */
@Service
public class ServicioReasignacion {

    private final TareaRepositorio tareas;
    private final ConsultasEjecucion consultas;
    private final LectorTareas lector;
    private final RecalculoTareas recalculo;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final RegistroEventos eventos;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioReasignacion(TareaRepositorio tareas, ConsultasEjecucion consultas, LectorTareas lector,
                                RecalculoTareas recalculo, Permisos permisos, RegistroAuditoria auditoria,
                                RegistroEventos eventos, JdbcClient jdbc, Clock reloj) {
        this.tareas = tareas;
        this.consultas = consultas;
        this.lector = lector;
        this.recalculo = recalculo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional
    public ResultadoReasignacion reasignar(UsuarioAutenticado usuario, long idTarea, SolicitudReasignacion s) {
        permisos.exigir(permisos.puedeGestionarTarea(usuario.idUsuario(), idTarea));
        Tarea t = tareas.findById(idTarea).orElseThrow();
        ContextoHito hito = consultas.hito(t.getIdHitoEquipo()).orElseThrow();
        Instant ahora = Instant.now(reloj);
        EdicionHito.exigirAbierta(hito, ahora);
        if (t.getEstado() == EstadoTarea.COMPLETADA) {
            throw new ReglaNegocioException("TAREA_COMPLETADA", "Una tarea completada no se reasigna");
        }
        Long anterior = t.getIdEncargado();
        if (anterior == null) {
            throw new ReglaNegocioException("SIN_ENCARGADO",
                    "La tarea no tiene encargado: asígnala editando la tarea");
        }
        long nuevo = s.idNuevoEncargado();
        if (anterior == nuevo) {
            throw new ReglaNegocioException("MISMO_ENCARGADO", "La tarea ya está a cargo de esa persona");
        }
        if (!permisos.esIntegranteDelEquipo(nuevo, hito.idEquipo())) {
            throw new ReglaNegocioException("ENCARGADO_NO_ES_MIEMBRO", "El encargado debe ser miembro del equipo");
        }
        long id = usuario.idUsuario();
        Tarea movida;
        if (s.tipo() == TipoReasignacion.TOTAL) {
            t.setIdEncargado(nuevo);
            tareas.saveAndFlush(t);
            auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idTarea, AccionAuditoria.REASIGNAR_TAREA,
                    "id_encargado", String.valueOf(anterior), String.valueOf(nuevo)));
            jdbc.sql("UPDATE solicitud_correccion SET id_estudiante = :nuevo "
                            + "WHERE id_tarea = :tarea AND estado = 'PENDIENTE'")
                    .param("nuevo", nuevo)
                    .param("tarea", idTarea)
                    .update();
            movida = t;
        } else {
            movida = dividir(t, s, anterior, nuevo, id, ahora);
        }
        String motivo = Textos.opcional(s.motivo());
        if (motivo != null) {
            auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idTarea, AccionAuditoria.REASIGNAR_TAREA,
                    "motivo", null, motivo));
        }
        Map<Long, Persona> personas = consultas.personas(List.of(anterior, nuevo, id));
        eventos.registrar(new TareaReasignada(new Referencia(hito.idProyecto(), hito.nombreProyecto()),
                new Referencia(hito.idEquipo(), hito.nombreEquipo()),
                new ReferenciaTarea(movida.getIdTarea(), movida.getTitulo()), personas.get(anterior),
                personas.get(nuevo), new Referencia(id, personas.get(id).nombre()), ahora));
        recalculo.recalcular(t.getIdHitoEquipo());
        return new ResultadoReasignacion(lector.una(idTarea),
                s.tipo() == TipoReasignacion.TOTAL ? null : lector.una(movida.getIdTarea()));
    }

    /** Decisión 18-3: crea la parte, le resta las horas a la original y le copia las dependencias. */
    private Tarea dividir(Tarea t, SolicitudReasignacion s, long anterior, long nuevo, long id, Instant ahora) {
        String titulo = Textos.opcional(s.tituloParcial());
        BigDecimal horas = s.horasParcial();
        if (titulo == null || horas == null) {
            throw new SolicitudInvalidaException("DATOS_PARCIAL_INCOMPLETOS",
                    "Para una reasignación parcial indica el título y las horas de la parte que pasa");
        }
        if (horas.compareTo(t.getHorasEstimadas()) >= 0) {
            throw new SolicitudInvalidaException("HORAS_PARCIAL_INVALIDAS",
                    "La parte debe tener menos horas que la tarea; si pasa toda, usa la reasignación total");
        }
        long idOrigen = t.getIdTarea();
        BigDecimal restantes = t.getHorasEstimadas().subtract(horas);
        AuditoriaCambios.siCambia(auditoria, id, "tarea", idOrigen, "horas_estimadas", t.getHorasEstimadas(),
                restantes);
        t.setHorasEstimadas(restantes);
        tareas.saveAndFlush(t);
        Tarea parte = new Tarea();
        parte.setIdHitoEquipo(t.getIdHitoEquipo());
        parte.setIdEncargado(nuevo);
        parte.setIdTareaOrigen(idOrigen);
        parte.setTitulo(titulo);
        parte.setHorasEstimadas(horas);
        parte.setFechaLimite(t.getFechaLimite());
        parte.setEsPrerrequisitoCritico(t.getEsPrerrequisitoCritico());
        parte.setFechaCambioEstado(ahora);
        parte = tareas.saveAndFlush(parte);
        long idParte = parte.getIdTarea();
        jdbc.sql("INSERT INTO dependencia_tarea (id_tarea_predecesora, id_tarea_dependiente) "
                        + "SELECT id_tarea_predecesora, :parte FROM dependencia_tarea WHERE id_tarea_dependiente = :origen")
                .param("parte", idParte)
                .param("origen", idOrigen)
                .update();
        jdbc.sql("INSERT INTO dependencia_tarea (id_tarea_predecesora, id_tarea_dependiente) "
                        + "SELECT :parte, id_tarea_dependiente FROM dependencia_tarea WHERE id_tarea_predecesora = :origen")
                .param("parte", idParte)
                .param("origen", idOrigen)
                .update();
        auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idOrigen, AccionAuditoria.REASIGNAR_TAREA,
                "tarea_parcial", null, String.valueOf(idParte)));
        auditoria.registrar(EntradaAuditoria.de(id, "tarea", idParte, AccionAuditoria.CREAR));
        auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idParte, AccionAuditoria.REASIGNAR_TAREA,
                "id_encargado", String.valueOf(anterior), String.valueOf(nuevo)));
        return parte;
    }
}
