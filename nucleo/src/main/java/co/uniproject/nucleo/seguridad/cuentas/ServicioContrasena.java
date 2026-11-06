package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.eventos.datos.ContrasenaCambiada;
import co.uniproject.eventos.datos.OrigenCambioContrasena;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.RecuperacionSolicitada;
import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.notificaciones.RegistroEventos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambio de contraseña con sesión y recuperación por Telegram (bloque 28; D6 sin correos). El código de un solo uso
 * no se guarda ni viaja por la outbox: el agente lo pide con emitirCodigo justo antes de enviarlo (decisión 28-2).
 */
@Service
public class ServicioContrasena {

    public enum ResultadoEmision { EMITIDO, INEXISTENTE, NO_VIGENTE }

    /** Respuesta para el endpoint interno. toString no muestra el código (es secreto). */
    public record EmisionCodigo(ResultadoEmision resultado, String codigo) {
        @Override
        public String toString() {
            return "EmisionCodigo[resultado=" + resultado + "]";
        }
    }

    private record Titular(long id, String nombre, Long chatId) {}

    private record Solicitud(UUID id, long usuarioId, byte[] verificadorHash, boolean activa, boolean cuentaActiva,
                             String nombre, Long chatId) {}

    private record Cuenta(String hash, String nombre, Long chatId, boolean anonimizada) {}

    private static final String ANULAR_ACTIVAS = "UPDATE recuperacion_contrasena SET anulada_en = now() "
            + "WHERE usuario_id = :usuario AND usada_en IS NULL AND anulada_en IS NULL";

    private final JdbcClient jdbc;
    private final PasswordEncoder codificador;
    private final RegistroAuditoria auditoria;
    private final RegistroEventos eventos;
    private final Clock reloj;
    private final Duration vigencia;
    private final int entregasMaximas;
    private final int intentosMaximos;

    public ServicioContrasena(JdbcClient jdbc, PasswordEncoder codificador, RegistroAuditoria auditoria,
                              RegistroEventos eventos, Clock reloj,
                              @Value("${uniproject.recuperacion.vigencia}") Duration vigencia,
                              @Value("${uniproject.recuperacion.entregas-maximas}") int entregasMaximas,
                              @Value("${uniproject.recuperacion.intentos-maximos}") int intentosMaximos) {
        this.jdbc = jdbc;
        this.codificador = codificador;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.reloj = reloj;
        this.vigencia = vigencia;
        this.entregasMaximas = entregasMaximas;
        this.intentosMaximos = intentosMaximos;
    }

    /** Decisión 28-5: no devuelve nada ni lanza por la cuenta; solo emite si existe, no está anonimizada y tiene chat. */
    @Transactional
    public void solicitarRecuperacion(String emailSolicitado) {
        Optional<Titular> titular = jdbc.sql("SELECT id_usuario, nombre, telegram_chat_id FROM usuario "
                        + "WHERE email = :email AND fecha_anonimizacion IS NULL AND telegram_chat_id IS NOT NULL "
                        + "FOR UPDATE")
                .param("email", ServicioCuentas.normalizarEmail(emailSolicitado))
                .query((rs, fila) -> new Titular(rs.getLong("id_usuario"), rs.getString("nombre"),
                        rs.getLong("telegram_chat_id")))
                .optional();
        if (titular.isEmpty()) {
            return;
        }
        Titular t = titular.get();
        jdbc.sql(ANULAR_ACTIVAS).param("usuario", t.id()).update();
        UUID id = UUID.randomUUID();
        OffsetDateTime vence = jdbc.sql("INSERT INTO recuperacion_contrasena (id, usuario_id, vence_en) "
                        + "VALUES (:id, :usuario, now() + make_interval(secs => :segundos)) RETURNING vence_en")
                .param("id", id)
                .param("usuario", t.id())
                .param("segundos", vigencia.toSeconds())
                .query((rs, fila) -> rs.getObject(1, OffsetDateTime.class))
                .single();
        auditoria.registrar(EntradaAuditoria.de(null, "usuario", t.id(), AccionAuditoria.SOLICITAR_RECUPERACION));
        eventos.registrar(new RecuperacionSolicitada(id, new Persona(t.id(), t.nombre(), t.chatId()),
                vence.toInstant().truncatedTo(ChronoUnit.SECONDS)));
    }

    /** Endpoint interno (decisiones 28-2 y 28-4): genera un código nuevo y reemplaza el anterior de esa solicitud. */
    @Transactional
    public EmisionCodigo emitirCodigo(UUID idSolicitud, long chatId) {
        Optional<Boolean> vigente = jdbc.sql("SELECT (r.usada_en IS NULL AND r.anulada_en IS NULL "
                        + "AND r.vence_en > now() AND r.entregas < :maximo AND u.fecha_anonimizacion IS NULL "
                        + "AND coalesce(u.telegram_chat_id = :chat, false)) AS vigente "
                        + "FROM recuperacion_contrasena r JOIN usuario u ON u.id_usuario = r.usuario_id "
                        + "WHERE r.id = :id FOR UPDATE OF r")
                .param("maximo", entregasMaximas)
                .param("chat", chatId)
                .param("id", idSolicitud)
                .query(Boolean.class)
                .optional();
        if (vigente.isEmpty()) {
            return new EmisionCodigo(ResultadoEmision.INEXISTENTE, null);
        }
        if (!vigente.get()) {
            return new EmisionCodigo(ResultadoEmision.NO_VIGENTE, null);
        }
        CodigosRecuperacion.Nuevo nuevo = CodigosRecuperacion.nuevo();
        jdbc.sql("UPDATE recuperacion_contrasena SET selector = :selector, verificador_hash = :hash, "
                        + "entregas = entregas + 1 WHERE id = :id")
                .param("selector", nuevo.selector())
                .param("hash", nuevo.hashVerificador())
                .param("id", idSolicitud)
                .update();
        return new EmisionCodigo(ResultadoEmision.EMITIDO, nuevo.texto());
    }

    /**
     * false = código inválido, vencido, usado, anulado o reemplazado (el controlador responde 400 CODIGO_INVALIDO).
     * Devuelve false en vez de lanzar para que el contador de intentos fallidos se confirme.
     */
    @Transactional
    public boolean restablecer(String codigo, String contrasenaNueva) {
        ServicioCuentas.exigirContrasenaValida(contrasenaNueva);
        Optional<CodigosRecuperacion.Partes> partes = CodigosRecuperacion.leer(codigo);
        if (partes.isEmpty()) {
            return false;
        }
        Optional<Solicitud> fila = jdbc.sql("SELECT r.id, r.usuario_id, r.verificador_hash, "
                        + "(r.usada_en IS NULL AND r.anulada_en IS NULL AND r.vence_en > now()) AS activa, "
                        + "u.fecha_anonimizacion IS NULL AS cuenta_activa, u.nombre, u.telegram_chat_id "
                        + "FROM recuperacion_contrasena r JOIN usuario u ON u.id_usuario = r.usuario_id "
                        + "WHERE r.selector = :selector FOR UPDATE OF r, u")
                .param("selector", partes.get().selector())
                .query((rs, n) -> new Solicitud(rs.getObject("id", UUID.class), rs.getLong("usuario_id"),
                        rs.getBytes("verificador_hash"), rs.getBoolean("activa"), rs.getBoolean("cuenta_activa"),
                        rs.getString("nombre"), rs.getObject("telegram_chat_id", Long.class)))
                .optional();
        if (fila.isEmpty()) {
            return false;
        }
        Solicitud s = fila.get();
        if (!CodigosRecuperacion.coincide(partes.get().verificador(), s.verificadorHash())) {
            jdbc.sql("UPDATE recuperacion_contrasena SET intentos_fallidos = intentos_fallidos + 1, "
                            + "anulada_en = CASE WHEN intentos_fallidos + 1 >= :maximo "
                            + "THEN coalesce(anulada_en, now()) ELSE anulada_en END WHERE id = :id")
                    .param("maximo", intentosMaximos)
                    .param("id", s.id())
                    .update();
            return false;
        }
        if (!s.activa() || !s.cuentaActiva()) {
            return false;
        }
        jdbc.sql("UPDATE recuperacion_contrasena SET usada_en = now() WHERE id = :id").param("id", s.id()).update();
        jdbc.sql(ANULAR_ACTIVAS).param("usuario", s.usuarioId()).update();
        jdbc.sql("UPDATE usuario SET password_hash = :hash, version_credenciales = version_credenciales + 1 "
                        + "WHERE id_usuario = :id")
                .param("hash", codificador.encode(contrasenaNueva))
                .param("id", s.usuarioId())
                .update();
        auditoria.registrar(EntradaAuditoria.de(s.usuarioId(), "usuario", s.usuarioId(),
                AccionAuditoria.RESTABLECER_CONTRASENA));
        avisarCambio(s.usuarioId(), s.nombre(), s.chatId(), OrigenCambioContrasena.RECUPERACION);
        return true;
    }

    /** Cambio con sesión (28-1 A). Devuelve la versión nueva de credenciales para dejarla en la sesión actual. */
    @Transactional
    public int cambiar(long idUsuario, String actual, String nueva) {
        Cuenta c = jdbc.sql("SELECT password_hash, nombre, telegram_chat_id, "
                        + "fecha_anonimizacion IS NOT NULL AS anonimizada FROM usuario WHERE id_usuario = :id FOR UPDATE")
                .param("id", idUsuario)
                .query((rs, n) -> new Cuenta(rs.getString("password_hash"), rs.getString("nombre"),
                        rs.getObject("telegram_chat_id", Long.class), rs.getBoolean("anonimizada")))
                .optional()
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta ya no existe"));
        if (c.anonimizada()) {
            throw new RecursoNoEncontradoException("La cuenta ya no existe");
        }
        if (actual.getBytes(StandardCharsets.UTF_8).length > ServicioCuentas.MAXIMO_BYTES_CONTRASENA
                || !codificador.matches(actual, c.hash())) {
            throw new SolicitudInvalidaException("CONTRASENA_INCORRECTA", "La contraseña actual no es correcta");
        }
        ServicioCuentas.exigirContrasenaValida(nueva);
        if (codificador.matches(nueva, c.hash())) {
            throw new SolicitudInvalidaException("CONTRASENA_REPETIDA",
                    "La contraseña nueva debe ser distinta de la actual");
        }
        int version = jdbc.sql("UPDATE usuario SET password_hash = :hash, "
                        + "version_credenciales = version_credenciales + 1 WHERE id_usuario = :id "
                        + "RETURNING version_credenciales")
                .param("hash", codificador.encode(nueva))
                .param("id", idUsuario)
                .query(Integer.class)
                .single();
        jdbc.sql(ANULAR_ACTIVAS).param("usuario", idUsuario).update();
        auditoria.registrar(EntradaAuditoria.de(idUsuario, "usuario", idUsuario, AccionAuditoria.CAMBIAR_CONTRASENA));
        avisarCambio(idUsuario, c.nombre(), c.chatId(), OrigenCambioContrasena.PERFIL);
        return version;
    }

    /** Decisión 28-11: solo si tiene Telegram (los eventos de cuenta no van a la bandeja, 28-13). */
    private void avisarCambio(long idUsuario, String nombre, Long chatId, OrigenCambioContrasena origen) {
        if (chatId != null) {
            eventos.registrar(new ContrasenaCambiada(new Persona(idUsuario, nombre, chatId),
                    Instant.now(reloj).truncatedTo(ChronoUnit.SECONDS), origen));
        }
    }
}
