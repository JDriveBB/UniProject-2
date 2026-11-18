package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.configuracionacademica.servicio.AuditoriaCambios;
import co.uniproject.nucleo.configuracionacademica.servicio.Textos;
import co.uniproject.nucleo.ejecucion.dominio.Tarea;
import co.uniproject.nucleo.ejecucion.repositorio.TareaRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF2: el líder crea, edita y asigna tareas del hito de su equipo. Leer: quien puede ver el equipo. */
@Service
public class ServicioTareas {

    private final TareaRepositorio tareas;
    private final ConsultasEjecucion consultas;
    private final LectorTareas lector;
    private final RecalculoTareas recalculo;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioTareas(TareaRepositorio tareas, ConsultasEjecucion consultas, LectorTareas lector,
                          RecalculoTareas recalculo, Permisos permisos, RegistroAuditoria auditoria, JdbcClient jdbc, Clock reloj) {
        this.tareas = tareas;
        this.consultas = consultas;
        this.lector = lector;
        this.recalculo = recalculo;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public TableroHito tablero(UsuarioAutenticado usuario, long idHitoEquipo) {
        permisos.exigir(permisos.puedeVerHitoEquipo(usuario.idUsuario(), idHitoEquipo));
        ContextoHito hito = consultas.hito(idHitoEquipo).orElseThrow();
        return TableroHito.de(hito, EdicionHito.abierta(hito, Instant.now(reloj)), lector.delHito(idHitoEquipo));
    }

    @Transactional(readOnly = true)
    public TareaDetalle detalle(UsuarioAutenticado usuario, long idTarea) {
        permisos.exigir(permisos.puedeVerTarea(usuario.idUsuario(), idTarea));
        ContextoHito hito = consultas.hitoDeTarea(idTarea).orElseThrow();
        return new TareaDetalle(lector.una(idTarea), EdicionHito.abierta(hito, Instant.now(reloj)),
                lector.avances(idTarea));
    }

    @Transactional(readOnly = true)
    public List<MiTareaVista> mias(UsuarioAutenticado usuario) {
        return lector.mias(usuario.idUsuario());
    }

    @Transactional
    public TareaVista crear(UsuarioAutenticado usuario, long idHitoEquipo, SolicitudTarea solicitud) {
        permisos.exigir(permisos.puedeGestionarHitoEquipo(usuario.idUsuario(), idHitoEquipo));
        BloqueoHito.bloquear(jdbc, idHitoEquipo);
        ContextoHito hito = consultas.hito(idHitoEquipo).orElseThrow();
        Instant ahora = Instant.now(reloj);
        EdicionHito.exigirAbierta(hito, ahora);
        validarFecha(hito, solicitud.fechaLimite());
        if (solicitud.idEncargado() != null) {
            exigirMiembro(hito, solicitud.idEncargado());
        }
        Tarea t = new Tarea();
        t.setIdHitoEquipo(idHitoEquipo);
        t.setIdEncargado(solicitud.idEncargado());
        t.setFechaCambioEstado(ahora);
        aplicar(t, solicitud);
        t = tareas.saveAndFlush(t);
        long idTarea = t.getIdTarea();
        auditoria.registrar(EntradaAuditoria.de(usuario.idUsuario(), "tarea", idTarea, AccionAuditoria.CREAR));
        if (solicitud.idEncargado() != null) {
            auditoria.registrar(EntradaAuditoria.cambio(usuario.idUsuario(), "tarea", idTarea,
                    AccionAuditoria.ASIGNAR_TAREA, "id_encargado", null, String.valueOf(solicitud.idEncargado())));
        }
        recalculo.recalcular(idHitoEquipo);
        return lector.una(idTarea);
    }

    /** PUT: reemplaza los campos. El encargado solo se fija si no había (decisión 17a-8). */
    @Transactional
    public TareaVista editar(UsuarioAutenticado usuario, long idTarea, SolicitudTarea solicitud) {
        permisos.exigir(permisos.puedeGestionarTarea(usuario.idUsuario(), idTarea));
        BloqueoHito.bloquearPorTarea(jdbc, idTarea);
        Tarea t = tareas.findById(idTarea).orElseThrow();
        ContextoHito hito = consultas.hito(t.getIdHitoEquipo()).orElseThrow();
        EdicionHito.exigirAbierta(hito, Instant.now(reloj));
        validarFecha(hito, solicitud.fechaLimite());
        Long actual = t.getIdEncargado();
        Long nuevo = solicitud.idEncargado();
        if (actual != null && !actual.equals(nuevo)) {
            throw new ReglaNegocioException("USAR_REASIGNACION",
                    "Para cambiar el encargado usa la reasignación: queda en la auditoría y avisa a los dos");
        }
        long id = usuario.idUsuario();
        if (actual == null && nuevo != null) {
            exigirMiembro(hito, nuevo);
            t.setIdEncargado(nuevo);
            auditoria.registrar(EntradaAuditoria.cambio(id, "tarea", idTarea, AccionAuditoria.ASIGNAR_TAREA,
                    "id_encargado", null, String.valueOf(nuevo)));
        }
        AuditoriaCambios.siCambia(auditoria, id, "tarea", idTarea, "titulo", t.getTitulo(), solicitud.titulo().strip());
        AuditoriaCambios.siCambia(auditoria, id, "tarea", idTarea, "descripcion", t.getDescripcion(),
                Textos.opcional(solicitud.descripcion()));
        AuditoriaCambios.siCambia(auditoria, id, "tarea", idTarea, "horas_estimadas", t.getHorasEstimadas(),
                solicitud.horasEstimadas());
        AuditoriaCambios.siCambia(auditoria, id, "tarea", idTarea, "fecha_limite", t.getFechaLimite(),
                solicitud.fechaLimite());
        AuditoriaCambios.siCambia(auditoria, id, "tarea", idTarea, "es_prerrequisito_critico",
                t.getEsPrerrequisitoCritico(), Boolean.TRUE.equals(solicitud.esPrerrequisitoCritico()));
        aplicar(t, solicitud);
        tareas.saveAndFlush(t);
        recalculo.recalcular(t.getIdHitoEquipo());
        return lector.una(idTarea);
    }

    /** Decisión 17a-10: si la tarea trae fecha propia, debe caer dentro del hito. */
    private static void validarFecha(ContextoHito hito, LocalDate fecha) {
        if (fecha != null && (fecha.isBefore(hito.fechaInicio()) || fecha.isAfter(hito.fechaLimite()))) {
            throw new SolicitudInvalidaException("FECHA_FUERA_DEL_HITO",
                    "La fecha límite de la tarea debe estar dentro de las fechas del hito");
        }
    }

    private void exigirMiembro(ContextoHito hito, long idUsuario) {
        if (!permisos.esIntegranteDelEquipo(idUsuario, hito.idEquipo())) {
            throw new ReglaNegocioException("ENCARGADO_NO_ES_MIEMBRO", "El encargado debe ser miembro del equipo");
        }
    }

    private static void aplicar(Tarea t, SolicitudTarea s) {
        t.setTitulo(s.titulo().strip());
        t.setDescripcion(Textos.opcional(s.descripcion()));
        t.setHorasEstimadas(s.horasEstimadas());
        t.setFechaLimite(s.fechaLimite());
        t.setEsPrerrequisitoCritico(Boolean.TRUE.equals(s.esPrerrequisitoCritico()));
    }
}
