package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.notificaciones.PersonasEnEventos;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derecho de supresión y revocatoria (Ley 1581 de 2012, art. 8 e) y 15; bloque 27). La fila de usuario no se borra:
 * se anonimiza para no romper las FK de los registros académicos, que se conservan ligados a un id sin datos
 * personales. Todo en una transacción; el log de auditoría solo recibe el id y la acción.
 */
@Service
public class ServicioSupresion {

    /** Mismo texto que usuario_anonimizado_ck (V4). */
    public static final String NOMBRE_ANONIMO = "Usuario anonimizado";
    private static final String HASH_INUTILIZABLE = "!";

    private final JdbcClient jdbc;
    private final PasswordEncoder codificador;
    private final RegistroAuditoria auditoria;
    private final PersonasEnEventos eventos;
    private final Clock reloj;

    private record Cuenta(String nombre, String email, String hash, RolPlataforma rol, boolean anonimizada) {}

    public ServicioSupresion(JdbcClient jdbc, PasswordEncoder codificador, RegistroAuditoria auditoria,
                             PersonasEnEventos eventos, Clock reloj) {
        this.jdbc = jdbc;
        this.codificador = codificador;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /** Dominio reservado (RFC 2606) que solo usa la anonimización; no se puede habilitar (Aclaración 1, A1-3). */
    public static final String SUFIJO_ANONIMO = "@anonimo.invalid";

    /** Correo que queda en la fila: único por id y con un dominio reservado que no existe. */
    public static String correoAnonimo(long idUsuario) {
        return "anonimo-" + idUsuario + SUFIJO_ANONIMO;
    }

    /** emailNormalizado: ya pasado por ServicioCuentas.normalizarEmail. */
    public static boolean esCorreoReservado(String emailNormalizado) {
        return emailNormalizado.endsWith(SUFIJO_ANONIMO);
    }

    public static SolicitudInvalidaException correoReservado() {
        return new SolicitudInvalidaException("CORREO_RESERVADO",
                "Los correos " + SUFIJO_ANONIMO + " están reservados y no se pueden habilitar");
    }

    /** Idempotente: sobre una cuenta ya anonimizada no hace nada. */
    @Transactional
    public void suprimir(long idUsuario, String contrasena) {
        Cuenta cuenta = jdbc.sql("SELECT nombre, email, password_hash, rol_plataforma, "
                        + "fecha_anonimizacion IS NOT NULL AS anonimizada FROM usuario WHERE id_usuario = :id FOR UPDATE")
                .param("id", idUsuario)
                .query((rs, fila) -> new Cuenta(rs.getString("nombre"), rs.getString("email"),
                        rs.getString("password_hash"), RolPlataforma.valueOf(rs.getString("rol_plataforma")),
                        rs.getBoolean("anonimizada")))
                .optional()
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta ya no existe"));
        if (cuenta.anonimizada()) {
            return;
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > ServicioCuentas.MAXIMO_BYTES_CONTRASENA
                || !codificador.matches(contrasena, cuenta.hash())) {
            throw new SolicitudInvalidaException("CONTRASENA_INCORRECTA", "La contraseña no es correcta");
        }
        Instant ahora = Instant.now(reloj);
        exigirQuePuedeSuprimirse(idUsuario, cuenta.rol(), FechasDominio.hoy(ahora));
        String anonimo = correoAnonimo(idUsuario);
        jdbc.sql("DELETE FROM codigo_vinculacion WHERE usuario_id = :id").param("id", idUsuario).update();
        jdbc.sql("DELETE FROM notificacion WHERE id_destinatario = :id").param("id", idUsuario).update();
        eventos.reemplazar(idUsuario, NOMBRE_ANONIMO, true);
        jdbc.sql("DELETE FROM correo_habilitado WHERE email = :email AND fecha_uso IS NULL")
                .param("email", cuenta.email())
                .update();
        jdbc.sql("UPDATE correo_habilitado SET email = :anonimo WHERE id_usuario_registrado = :id")
                .param("anonimo", anonimo)
                .param("id", idUsuario)
                .update();
        jdbc.sql("UPDATE evidencia SET nombre_archivo = 'evidencia-' || CAST(id_evidencia AS text) || '.' "
                        + "|| substring(url from '\\.(pdf|png|jpg)$') "
                        + "WHERE id_usuario = :id AND tipo_evidencia IN ('PDF', 'IMAGEN')")
                .param("id", idUsuario)
                .update();
        jdbc.sql("UPDATE usuario SET nombre = :nombre, email = :anonimo, password_hash = :hash, "
                        + "telegram_chat_id = NULL, autoriza_tratamiento_datos = false, fecha_autorizacion = NULL, "
                        + "fecha_anonimizacion = :ahora WHERE id_usuario = :id")
                .param("nombre", NOMBRE_ANONIMO)
                .param("anonimo", anonimo)
                .param("hash", HASH_INUTILIZABLE)
                .param("ahora", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .param("id", idUsuario)
                .update();
        auditoria.registrar(EntradaAuditoria.de(idUsuario, "usuario", idUsuario, AccionAuditoria.SUPRIMIR_CUENTA));
    }

    /** Decisión 27-11: 409 si la cuenta sostiene algo que sigue en curso. */
    private void exigirQuePuedeSuprimirse(long idUsuario, RolPlataforma rol, LocalDate hoy) {
        if (rol == RolPlataforma.ADMINISTRADOR) {
            throw new ReglaNegocioException("CUENTA_ADMINISTRADOR",
                    "La cuenta de administrador no se puede eliminar desde la aplicación");
        }
        if (existe("SELECT EXISTS (SELECT 1 FROM proyecto WHERE id_docente = :id AND fecha_fin >= :hoy)",
                idUsuario, hoy)) {
            throw new ReglaNegocioException("DOCENTE_CON_PROYECTOS_ACTIVOS",
                    "No puedes eliminar tu cuenta mientras tengas proyectos activos (hasta su fecha de fin)");
        }
        if (existe("SELECT EXISTS (SELECT 1 FROM miembro_equipo m JOIN equipo e ON e.id_equipo = m.id_equipo "
                        + "JOIN proyecto p ON p.id_proyecto = e.id_proyecto WHERE m.id_usuario = :id "
                        + "AND m.rol_equipo = 'LIDER' AND p.fecha_fin >= :hoy)",
                idUsuario, hoy)) {
            throw new ReglaNegocioException("LIDER_DE_EQUIPO_ACTIVO",
                    "Eres líder de un equipo en un proyecto activo. Pide a tu docente que asigne otro líder "
                            + "antes de eliminar tu cuenta");
        }
    }

    private boolean existe(String sql, long idUsuario, LocalDate hoy) {
        return jdbc.sql(sql).param("id", idUsuario).param("hoy", hoy).query(Boolean.class).single();
    }
}
