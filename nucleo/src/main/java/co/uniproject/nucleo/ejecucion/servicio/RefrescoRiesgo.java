package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.FechasDominio;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * T-019: vuelve a guardar ruta crítica, holgura, riesgo y bloqueo de las tareas de los hitos EN_CURSO ya iniciados,
 * para que el riesgo guardado avance con el reloj aunque nadie toque las tareas. Una transacción por hito, con el
 * candado del hito (decisión 19-2) como primera sentencia; un hito que falla no detiene a los demás.
 */
@Component
public class RefrescoRiesgo {

    private static final Logger log = LoggerFactory.getLogger(RefrescoRiesgo.class);

    private static final String HITOS_EN_CURSO = "SELECT he.id_hito_equipo FROM hito_equipo he "
            + "JOIN hito h ON h.id_hito = he.id_hito WHERE he.estado = 'EN_CURSO' AND h.fecha_inicio <= :hoy "
            + "AND EXISTS (SELECT 1 FROM tarea t WHERE t.id_hito_equipo = he.id_hito_equipo) "
            + "ORDER BY he.id_hito_equipo";

    private final JdbcClient jdbc;
    private final RecalculoTareas recalculo;
    private final TransactionTemplate tx;
    private final Clock reloj;

    public RefrescoRiesgo(JdbcClient jdbc, RecalculoTareas recalculo, TransactionTemplate tx, Clock reloj) {
        this.jdbc = jdbc;
        this.recalculo = recalculo;
        this.tx = tx;
        this.reloj = reloj;
    }

    /** Recalcula cada hito en curso ya iniciado y con tareas; devuelve cuántos se recalcularon. */
    public int refrescarEnCurso() {
        List<Long> hitos = jdbc.sql(HITOS_EN_CURSO)
                .param("hoy", FechasDominio.hoy(Instant.now(reloj)))
                .query(Long.class)
                .list();
        int recalculados = 0;
        for (long id : hitos) {
            try {
                boolean recalculado = tx.execute(s -> {
                    BloqueoHito.bloquear(jdbc, id);
                    // El hito pudo entregarse entre la consulta y el candado: con el candado tomado se vuelve a leer.
                    String estado = jdbc.sql("SELECT estado FROM hito_equipo WHERE id_hito_equipo = :he")
                            .param("he", id)
                            .query(String.class)
                            .single();
                    if (!"EN_CURSO".equals(estado)) {
                        return false;
                    }
                    recalculo.recalcular(id);
                    return true;
                });
                if (recalculado) {
                    recalculados++;
                }
            } catch (RuntimeException e) {
                log.warn(
                        "Falló el recálculo de riesgo de un hito: {}",
                        e.getClass().getSimpleName());
            }
        }
        return recalculados;
    }
}
