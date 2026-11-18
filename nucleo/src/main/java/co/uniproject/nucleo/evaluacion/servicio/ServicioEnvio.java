package co.uniproject.nucleo.evaluacion.servicio;

import co.uniproject.eventos.datos.HitoEnviado;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.ejecucion.servicio.ConsultasEjecucion;
import co.uniproject.nucleo.ejecucion.servicio.ContextoHito;
import co.uniproject.nucleo.ejecucion.servicio.EdicionHito;
import co.uniproject.nucleo.notificaciones.RegistroEventos;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF7: envío del hito a revisión (decisiones 19-1 y 19-2). */
@Service
public class ServicioEnvio {

    private final ConsultasEjecucion consultas;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final RegistroEventos eventos;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioEnvio(ConsultasEjecucion consultas, Permisos permisos, RegistroAuditoria auditoria,
                         RegistroEventos eventos, JdbcClient jdbc, Clock reloj) {
        this.consultas = consultas;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    /** Decisión 19-2: serializa envíos y evaluaciones del mismo hito de equipo. */
    static void bloquear(JdbcClient jdbc, long idHitoEquipo) {
        co.uniproject.nucleo.ejecucion.servicio.BloqueoHito.bloquear(jdbc, idHitoEquipo);
    }

    @Transactional
    public EnvioVista enviar(UsuarioAutenticado usuario, long idHitoEquipo) {
        long id = usuario.idUsuario();
        permisos.exigir(permisos.puedeEnviarHito(id, idHitoEquipo));
        bloquear(jdbc, idHitoEquipo);
        ContextoHito hito = consultas.hito(idHitoEquipo).orElseThrow();
        Instant ahora = Instant.now(reloj);
        if (!EdicionHito.abierta(hito, ahora)) {
            throw new ReglaNegocioException("HITO_NO_ENVIABLE", "El hito ya está enviado o cerrado");
        }
        List<String> pendientes = jdbc.sql("SELECT titulo FROM tarea WHERE id_hito_equipo = :he "
                        + "AND es_prerrequisito_critico AND estado <> 'COMPLETADA' ORDER BY id_tarea")
                .param("he", idHitoEquipo)
                .query(String.class)
                .list();
        if (!pendientes.isEmpty()) {
            throw new ReglaNegocioException("PRERREQUISITOS_INCOMPLETOS",
                    "Completa las tareas marcadas como prerrequisito crítico antes de enviar el hito", pendientes);
        }
        long idEnvio = jdbc.sql("INSERT INTO envio_hito (id_hito_equipo, id_usuario_envia, fecha_envio) "
                        + "VALUES (:he, :usuario, :fecha) RETURNING id_envio")
                .param("he", idHitoEquipo)
                .param("usuario", id)
                .param("fecha", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .query(Long.class)
                .single();
        jdbc.sql("UPDATE hito_equipo SET estado = 'ENVIADO' WHERE id_hito_equipo = :he")
                .param("he", idHitoEquipo)
                .update();
        jdbc.sql("UPDATE tarea SET edicion_habilitada = false WHERE id_hito_equipo = :he")
                .param("he", idHitoEquipo)
                .update();
        auditoria.registrar(EntradaAuditoria.cambio(id, "hito_equipo", idHitoEquipo, AccionAuditoria.ENVIAR_HITO,
                "estado", hito.estado().name(), "ENVIADO"));
        Map<Long, Persona> personas = consultas.personas(List.of(id, hito.idDocente()));
        String nombre = personas.get(id).nombre();
        eventos.registrar(new HitoEnviado(new Referencia(hito.idProyecto(), hito.nombreProyecto()),
                new Referencia(hito.idEquipo(), hito.nombreEquipo()), new Referencia(idHitoEquipo, hito.nombreHito()),
                ahora, new Referencia(id, nombre), personas.get(hito.idDocente())));
        return new EnvioVista(idEnvio, idHitoEquipo, id, nombre, ahora);
    }
}
