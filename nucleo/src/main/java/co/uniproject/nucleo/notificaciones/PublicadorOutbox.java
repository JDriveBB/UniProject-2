package co.uniproject.nucleo.notificaciones;

import co.uniproject.eventos.Topologia;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PublicadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(PublicadorOutbox.class);

    private final JdbcClient jdbc;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate tx;
    private final int tamanoLote;
    private final Duration esperaConfirmacion;

    private record Pendiente(UUID id, String tipo, String carga) {}

    public PublicadorOutbox(JdbcClient jdbc, RabbitTemplate rabbit, TransactionTemplate tx,
                            @Value("${uniproject.outbox.tamano-lote}") int tamanoLote,
                            @Value("${uniproject.outbox.espera-confirmacion}") Duration esperaConfirmacion) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.tx = tx;
        this.tamanoLote = tamanoLote;
        this.esperaConfirmacion = esperaConfirmacion;
    }

    /** Una pasada: publica hasta tamanoLote eventos pendientes, en una sola transacción. */
    public ResultadoLote publicarLote() {
        return tx.execute(estado -> {
            List<Pendiente> pendientes = jdbc.sql("""
                    SELECT id, tipo, carga::text AS carga
                      FROM evento_saliente
                     WHERE publicado_en IS NULL
                     ORDER BY creado_en
                     LIMIT :lote
                     FOR UPDATE SKIP LOCKED
                    """)
                    .param("lote", tamanoLote)
                    .query((rs, rowNum) -> new Pendiente(
                            rs.getObject("id", UUID.class),
                            rs.getString("tipo"),
                            rs.getString("carga")
                    ))
                    .list();

            int leidos = pendientes.size();
            int publicados = 0;
            boolean cortado = false;

            for (Pendiente p : pendientes) {
                Message mensaje = MessageBuilder.withBody(p.carga().getBytes(StandardCharsets.UTF_8))
                        .setContentType(Topologia.CONTENT_TYPE)
                        .setContentEncoding("utf-8")
                        .setMessageId(p.id().toString())
                        .setType(p.tipo())
                        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                        .build();

                CorrelationData cd = new CorrelationData(p.id().toString());
                try {
                    rabbit.send(Topologia.EXCHANGE_EVENTOS, p.tipo(), mensaje, cd);
                    CorrelationData.Confirm confirmacion = cd.getFuture().get(esperaConfirmacion.toMillis(), TimeUnit.MILLISECONDS);
                    if (confirmacion != null && confirmacion.isAck()) {
                        jdbc.sql("UPDATE evento_saliente SET publicado_en = now() WHERE id = :id")
                                .param("id", p.id())
                                .update();
                        publicados++;
                    } else {
                        jdbc.sql("UPDATE evento_saliente SET intentos = intentos + 1 WHERE id = :id")
                                .param("id", p.id())
                                .update();
                        log.warn("No se confirmó la publicación del evento {} ({}); se reintenta en la próxima pasada", p.id(), p.tipo());
                        cortado = true;
                        break;
                    }
                } catch (AmqpException | TimeoutException | ExecutionException e) {
                    jdbc.sql("UPDATE evento_saliente SET intentos = intentos + 1 WHERE id = :id")
                            .param("id", p.id())
                            .update();
                    log.warn("No se confirmó la publicación del evento {} ({}); se reintenta en la próxima pasada", p.id(), p.tipo());
                    cortado = true;
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    jdbc.sql("UPDATE evento_saliente SET intentos = intentos + 1 WHERE id = :id")
                            .param("id", p.id())
                            .update();
                    log.warn("No se confirmó la publicación del evento {} ({}); se reintenta en la próxima pasada", p.id(), p.tipo());
                    cortado = true;
                    break;
                }
            }

            if (publicados > 0) {
                log.info("Outbox: {} eventos publicados", publicados);
            }
            return new ResultadoLote(leidos, publicados, cortado);
        });
    }

    @Scheduled(fixedDelayString = "${uniproject.outbox.intervalo-ms}", initialDelayString = "${uniproject.outbox.intervalo-ms}")
    public void publicarProgramado() {
        try {
            publicarLote();
        } catch (Exception e) {
            log.error("Error al publicar lote de outbox", e);
        }
    }
}
