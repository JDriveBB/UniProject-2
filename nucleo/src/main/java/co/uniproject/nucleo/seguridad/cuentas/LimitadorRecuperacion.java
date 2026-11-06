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
        CORREO("correo"), IP("ip"), CANJE("canje"), CAMBIO("cambio");

        private final String prefijo;

        Tipo(String prefijo) {
            this.prefijo = prefijo;
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
                                 @Value("${uniproject.recuperacion.maximo-cambios-por-cuenta}") int cambiosPorCuenta) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(gestor);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.ventana = ventana;
        maximos.put(Tipo.CORREO, porCorreo);
        maximos.put(Tipo.IP, porIp);
        maximos.put(Tipo.CANJE, canjesPorIp);
        maximos.put(Tipo.CAMBIO, cambiosPorCuenta);
    }

    /** Cuenta el intento y lanza DemasiadasSolicitudesException (429) si la clave pasó su máximo en la ventana. */
    public void exigir(Tipo tipo, String valor) {
        String clave = tipo.prefijo + ":" + huella(valor);
        Integer cuenta = tx.execute(estado -> jdbc.sql(CONTAR)
                .param("clave", clave)
                .param("segundos", ventana.toSeconds())
                .query(Integer.class)
                .single());
        if (cuenta == null || cuenta > maximos.get(tipo)) {
            throw new DemasiadasSolicitudesException();
        }
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
