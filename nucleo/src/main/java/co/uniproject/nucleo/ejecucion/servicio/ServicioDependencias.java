package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.ejecucion.dependencias.MotorDependencias;
import co.uniproject.nucleo.ejecucion.dependencias.ResultadoDependencias;
import co.uniproject.nucleo.ejecucion.dependencias.RutaCritica;
import co.uniproject.nucleo.ejecucion.dependencias.TareaPlan;
import co.uniproject.nucleo.ejecucion.dependencias.Tiempos;
import co.uniproject.nucleo.ejecucion.dominio.Tarea;
import co.uniproject.nucleo.ejecucion.repositorio.TareaRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF2 (dependencias), RF3 (ruta crítica) y 4d (sin ciclos). Escribir: el líder. Leer: quien ve el equipo. */
@Service
public class ServicioDependencias {

    private final TareaRepositorio tareas;
    private final ConsultasEjecucion consultas;
    private final LectorTareas lector;
    private final RecalculoTareas recalculo;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioDependencias(TareaRepositorio tareas, ConsultasEjecucion consultas, LectorTareas lector,
                                RecalculoTareas recalculo, Permisos permisos, RegistroAuditoria auditoria,
                                JdbcClient jdbc, Clock reloj) {
        this.tareas = tareas;
        this.consultas = consultas;
        this.lector = lector;
        this.recalculo = recalculo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    /** Caso de uso "Establecer dependencias" (entrega1 sección 2.7) con su flujo alterno de dependencia circular. */
    @Transactional
    public ResultadoDependenciasVista agregar(UsuarioAutenticado usuario, long idTarea, SolicitudDependencias solicitud) {
        permisos.exigir(permisos.puedeGestionarTarea(usuario.idUsuario(), idTarea));
        BloqueoHito.bloquearPorTarea(jdbc, idTarea);
        Tarea tarea = tareas.findById(idTarea).orElseThrow();
        ContextoHito hito = consultas.hito(tarea.getIdHitoEquipo()).orElseThrow();
        EdicionHito.exigirAbierta(hito, Instant.now(reloj));
        List<Tarea> delHito = tareas.findByIdHitoEquipo(tarea.getIdHitoEquipo());
        Set<Long> ids = delHito.stream().map(Tarea::getIdTarea).collect(Collectors.toSet());
        Set<Long> candidatos = new HashSet<>(solicitud.prerrequisitos());
        if (!ids.containsAll(candidatos)) {
            throw new SolicitudInvalidaException("PRERREQUISITO_DE_OTRO_HITO",
                    "Los prerrequisitos deben ser tareas del mismo hito del equipo");
        }
        Map<Long, Set<Long>> previas = recalculo.prerrequisitos(delHito);
        List<TareaPlan> planes = delHito.stream().map(t -> RecalculoTareas.plan(t, previas)).toList();
        ResultadoDependencias resultado = MotorDependencias.agregarPrerrequisitos(planes, idTarea, candidatos);
        for (long p : resultado.aceptados()) {
            int filas = jdbc.sql("INSERT INTO dependencia_tarea (id_tarea_predecesora, id_tarea_dependiente) "
                            + "VALUES (:predecesora, :dependiente) ON CONFLICT DO NOTHING")
                    .param("predecesora", p)
                    .param("dependiente", idTarea)
                    .update();
            if (filas == 1) {
                auditoria.registrar(EntradaAuditoria.cambio(usuario.idUsuario(), "tarea", idTarea,
                        AccionAuditoria.ESTABLECER_DEPENDENCIA, "prerrequisito", null, String.valueOf(p)));
            }
        }
        recalculo.recalcular(tarea.getIdHitoEquipo());
        return new ResultadoDependenciasVista(resultado.aceptados(), resultado.ciclicos(), lector.una(idTarea));
    }

    @Transactional
    public TareaVista quitar(UsuarioAutenticado usuario, long idTarea, long idPrerrequisito) {
        permisos.exigir(permisos.puedeGestionarTarea(usuario.idUsuario(), idTarea));
        BloqueoHito.bloquearPorTarea(jdbc, idTarea);
        Tarea tarea = tareas.findById(idTarea).orElseThrow();
        ContextoHito hito = consultas.hito(tarea.getIdHitoEquipo()).orElseThrow();
        EdicionHito.exigirAbierta(hito, Instant.now(reloj));
        int filas = jdbc.sql("DELETE FROM dependencia_tarea WHERE id_tarea_predecesora = :predecesora "
                        + "AND id_tarea_dependiente = :dependiente")
                .param("predecesora", idPrerrequisito)
                .param("dependiente", idTarea)
                .update();
        if (filas == 0) {
            throw new RecursoNoEncontradoException("Esa dependencia no existe");
        }
        auditoria.registrar(EntradaAuditoria.cambio(usuario.idUsuario(), "tarea", idTarea,
                AccionAuditoria.ESTABLECER_DEPENDENCIA, "prerrequisito", String.valueOf(idPrerrequisito), null));
        recalculo.recalcular(tarea.getIdHitoEquipo());
        return lector.una(idTarea);
    }

    /** RF3: ruta crítica del hito de equipo para graficar (calculada al pedirla, no guarda nada). */
    @Transactional(readOnly = true)
    public RutaCriticaVista rutaCritica(UsuarioAutenticado usuario, long idHitoEquipo) {
        permisos.exigir(permisos.puedeVerHitoEquipo(usuario.idUsuario(), idHitoEquipo));
        List<Tarea> lista = tareas.findByIdHitoEquipo(idHitoEquipo);
        Map<Long, Set<Long>> previas = recalculo.prerrequisitos(lista);
        List<TareaPlan> planes = lista.stream().map(t -> RecalculoTareas.plan(t, previas)).toList();
        RutaCritica ruta = MotorDependencias.rutaCritica(planes);
        Map<Long, Tarea> porId = lista.stream().collect(Collectors.toMap(Tarea::getIdTarea, Function.identity()));
        Set<Long> criticas = new HashSet<>(ruta.criticas());
        List<RutaCriticaVista.Nodo> nodos = ruta.orden().stream().map(id -> {
            Tarea t = porId.get(id);
            Tiempos ti = ruta.tiempos().get(id);
            return new RutaCriticaVista.Nodo(id, t.getTitulo(), t.getHorasEstimadas(), t.getPorcentajeAvance(),
                    criticas.contains(id), RecalculoTareas.horas(ti.inicioTemprano()),
                    RecalculoTareas.horas(ti.finTemprano()), RecalculoTareas.horas(ti.inicioTardio()),
                    RecalculoTareas.horas(ti.finTardio()), RecalculoTareas.horas(ti.holgura()),
                    List.copyOf(previas.getOrDefault(id, Set.of())));
        }).toList();
        return new RutaCriticaVista(idHitoEquipo, RecalculoTareas.horas(ruta.duracionTotal()), ruta.criticas(), nodos);
    }
}
