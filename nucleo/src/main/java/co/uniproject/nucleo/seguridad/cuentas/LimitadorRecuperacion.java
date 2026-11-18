package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.comun.error.DemasiadasSolicitudesException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Límites de solicitudes (decisión 28-6): ventana fija por clave en PostgreSQL, en su propia transacción para que el
 * intento cuente aunque la petición falle después. La clave guarda el SHA-256 del valor, nunca el correo ni la IP.
 */
@Component
public class LimitadorRecuperacion {

    public enum Tipo {
        CORREO("correo", ""),
        IP("ip", ""),
        CANJE("canje", ""),
        CAMBIO("cambio", ""),
        LOGIN_CORREO("correo", "login:"),
        LOGIN_IP("ip", "login:"),
        REGISTRO_IP("ip", "registro:");

        private final String prefijo;
        private final String espacio;

        Tipo(String prefijo, String espacio) {
            this.prefijo = prefijo;
            this.espacio = espacio;
        }
    }

    private static final String CONTAR = """
            INSERT INTO limite_recuperacion AS l (clave, ventana_inicio, cuenta) VALUES (:clave, now(), 1)
            ON CONFLICT (clave) DO UPDATE SET
                ventana_inicio = CASE WHEN l.ventana_inicio <= now() - make_interval(secs => :segundos)
                                      THEN now() ELSE l.ventana_inicio END,
                cuenta = CASE WHEN l.ventana_inicio <= now() - make_interval(secs => :segundos)
                              THEN 1 ELSE LEAST(l.cuenta + 1, 1000000) END
            RETURNING cuenta
            """;

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Duration ventana;
    private final Map<Tipo, Integer> maximos = new EnumMap<>(Tipo.class);

    public LimitadorRecuperacion(JdbcClient jdbc, PlatformTransactionManager gestor,
                                 @Value("${uniproject.recuperacion.ventana}") Duration ventana,
                                 @Value("${uniproject.recuperacion.maximo-por-correo}") int porCorreo,
                                 @Value("${uniproject.recuperacion.maximo-por-ip}") int porIp,
                                 @Value("${uniproject.recuperacion.maximo-canjes-por-ip}") int canjesPorIp,
                                 @Value("${uniproject.recuperacion.maximo-cambios-por-cuenta}") int cambiosPorCuenta,
                                 @Value("${uniproject.recuperacion.maximo-login-por-correo}") int loginPorCorreo,
                                 @Value("${uniproject.recuperacion.maximo-login-por-ip}") int loginPorIp,
                                 @Value("${uniproject.recuperacion.maximo-registro-por-ip}") int registroPorIp) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(gestor);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.ventana = ventana;
        maximos.put(Tipo.CORREO, porCorreo);
        maximos.put(Tipo.IP, porIp);
        maximos.put(Tipo.CANJE, canjesPorIp);
        maximos.put(Tipo.CAMBIO, cambiosPorCuenta);
        maximos.put(Tipo.LOGIN_CORREO, loginPorCorreo);
        maximos.put(Tipo.LOGIN_IP, loginPorIp);
        maximos.put(Tipo.REGISTRO_IP, registroPorIp);
    }

    /** Cuenta el intento y lanza DemasiadasSolicitudesException (429) si la clave pasó su máximo en la ventana. */
    public void exigir(Tipo tipo, String valor) {
        String clave = clave(tipo, valor);
        Integer cuenta = tx.execute(estado -> jdbc.sql(CONTAR)
                .param("clave", clave)
                .param("segundos", ventana.toSeconds())
                .query(Integer.class)
                .single());
        if (cuenta == null || cuenta > maximos.get(tipo)) {
            throw new DemasiadasSolicitudesException();
        }
    }

    /** Resta 1 (sin bajar de 0) al contador de esa clave en su propia transacción. */
    public void devolver(Tipo tipo, String valor) {
        String clave = clave(tipo, valor);
        tx.executeWithoutResult(estado -> {
            int actualizados = jdbc.sql(
                    "UPDATE limite_recuperacion SET cuenta = cuenta - 1 WHERE clave = :clave AND cuenta > 1")
                    .param("clave", clave)
                    .update();
            if (actualizados == 0) {
                jdbc.sql("DELETE FROM limite_recuperacion WHERE clave = :clave AND cuenta <= 1")
                        .param("clave", clave)
                        .update();
            }
        });
    }

    /** Borra el contador de esa clave. */
    public void reiniciar(Tipo tipo, String valor) {
        String clave = clave(tipo, valor);
        tx.executeWithoutResult(estado -> jdbc.sql("DELETE FROM limite_recuperacion WHERE clave = :clave")
                .param("clave", clave)
                .update());
    }

    private String clave(Tipo tipo, String valor) {
        return tipo.prefijo + ":" + huella(tipo.espacio + valor);
    }

    static String huella(String valor) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
