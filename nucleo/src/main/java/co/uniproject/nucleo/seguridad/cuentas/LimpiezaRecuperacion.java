package co.uniproject.nucleo.seguridad.cuentas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Minimización (Ley 1581): borra solicitudes vencidas hace más de un día y contadores de límite viejos. */
@Component
public class LimpiezaRecuperacion {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaRecuperacion.class);

    private final JdbcClient jdbc;

    public LimpiezaRecuperacion(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "${uniproject.recuperacion.cron-limpieza}", zone = "America/Bogota")
    public void limpiar() {
        try {
            jdbc.sql("DELETE FROM recuperacion_contrasena WHERE vence_en < now() - interval '1 day'").update();
            jdbc.sql("DELETE FROM limite_recuperacion WHERE ventana_inicio < now() - interval '1 day'").update();
        } catch (RuntimeException e) {
            log.warn("No se pudo limpiar la recuperación de contraseñas ({})", e.getClass().getName());
        }
    }
}
