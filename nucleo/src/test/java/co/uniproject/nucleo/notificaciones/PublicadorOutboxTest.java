package co.uniproject.nucleo.notificaciones;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.Topologia;
import co.uniproject.eventos.datos.DatosEvento;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import co.uniproject.nucleo.PostgresPrueba;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PublicadorOutboxTest {

    private DataSource ds;
    private JdbcClient jdbc;
    private TransactionTemplate tx;
    private CodecEventos codec;
    private RegistroEventosJdbc registro;
    private RabbitTemplate rabbit;
    private PublicadorOutbox publicador;

    @BeforeEach
    void setUp() {
        ds = PostgresPrueba.baseLimpia("1");
        jdbc = JdbcClient.create(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        Clock reloj = Clock.fixed(Instant.parse("2026-10-24T19:05:00Z"), ZoneId.of("America/Bogota"));
        codec = new CodecEventos();
        registro = new RegistroEventosJdbc(jdbc, codec, reloj);
        rabbit = Mockito.mock(RabbitTemplate.class);
        publicador = new PublicadorOutbox(jdbc, rabbit, tx, 50, Duration.ofMillis(300));
    }

    private UUID sembrar(DatosEvento datos) {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return tx.execute(s -> registro.registrar(datos));
    }

    @Test
    void publicaTodoConAck() {
        sembrar(EjemplosEventos.tareaReasignada());
        sembrar(EjemplosEventos.hitoEnviado());
        sembrar(EjemplosEventos.tareaCorreccion());

        doAnswer(inv -> {
            CorrelationData cd = inv.getArgument(3);
            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(eq(Topologia.EXCHANGE_EVENTOS), anyString(), any(Message.class), any(CorrelationData.class));

        ResultadoLote res = publicador.publicarLote();

        assertThat(res).isEqualTo(new ResultadoLote(3, 3, false));

        Integer noNulos = jdbc.sql("SELECT count(*) FROM evento_saliente WHERE publicado_en IS NOT NULL")
                .query(Integer.class)
                .single();
        assertThat(noNulos).isEqualTo(3);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);

        verify(rabbit, times(3)).send(eq(Topologia.EXCHANGE_EVENTOS), routingKeyCaptor.capture(), messageCaptor.capture(), any(CorrelationData.class));

        List<Message> mensajes = messageCaptor.getAllValues();
        List<String> routingKeys = routingKeyCaptor.getAllValues();

        for (int i = 0; i < 3; i++) {
            Message msg = mensajes.get(i);
            String routingKey = routingKeys.get(i);
            MessageProperties props = msg.getMessageProperties();

            assertThat(props.getContentType()).isEqualTo("application/cloudevents+json");
            assertThat(props.getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
            assertThat(props.getType()).isEqualTo(routingKey);

            UUID msgId = UUID.fromString(props.getMessageId());
            String tipoEnBd = jdbc.sql("SELECT tipo FROM evento_saliente WHERE id = :id")
                    .param("id", msgId)
                    .query(String.class)
                    .single();
            assertThat(tipoEnBd).isEqualTo(props.getType());
            assertThat(routingKey).isEqualTo(tipoEnBd);

            EventoCloud ec = codec.leer(msg.getBody());
            assertThat(ec).isNotNull();
            assertThat(ec.id()).isEqualTo(msgId);
        }
    }

    @Test
    void nackCortaElLote() {
        UUID id1 = sembrar(EjemplosEventos.tareaReasignada());
        UUID id2 = sembrar(EjemplosEventos.hitoEnviado());
        UUID id3 = sembrar(EjemplosEventos.tareaCorreccion());

        AtomicInteger llamadas = new AtomicInteger();
        doAnswer(inv -> {
            CorrelationData cd = inv.getArgument(3);
            int n = llamadas.incrementAndGet();
            if (n == 1) {
                cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            } else {
                cd.getFuture().complete(new CorrelationData.Confirm(false, "prueba"));
            }
            return null;
        }).when(rabbit).send(eq(Topologia.EXCHANGE_EVENTOS), anyString(), any(Message.class), any(CorrelationData.class));

        ResultadoLote res = publicador.publicarLote();

        assertThat(res).isEqualTo(new ResultadoLote(3, 1, true));

        var fila1Pub = jdbc.sql("SELECT publicado_en FROM evento_saliente WHERE id = :id")
                .param("id", id1)
                .query((rs, r) -> rs.getObject("publicado_en"))
                .optional();
        assertThat(fila1Pub.get()).isNotNull();

        var fila2 = jdbc.sql("SELECT publicado_en, intentos FROM evento_saliente WHERE id = :id")
                .param("id", id2)
                .query((rs, r) -> Map.of("pubNull", rs.getObject("publicado_en") == null, "intentos", rs.getInt("intentos")))
                .single();
        assertThat(fila2.get("pubNull")).isEqualTo(true);
        assertThat(fila2.get("intentos")).isEqualTo(1);

        var fila3 = jdbc.sql("SELECT publicado_en, intentos FROM evento_saliente WHERE id = :id")
                .param("id", id3)
                .query((rs, r) -> Map.of("pubNull", rs.getObject("publicado_en") == null, "intentos", rs.getInt("intentos")))
                .single();
        assertThat(fila3.get("pubNull")).isEqualTo(true);
        assertThat(fila3.get("intentos")).isEqualTo(0);

        verify(rabbit, times(2)).send(eq(Topologia.EXCHANGE_EVENTOS), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void tiempoAgotadoCuentaIntento() {
        UUID id = sembrar(EjemplosEventos.tareaReasignada());

        doAnswer(inv -> null).when(rabbit).send(eq(Topologia.EXCHANGE_EVENTOS), anyString(), any(Message.class), any(CorrelationData.class));

        ResultadoLote res = publicador.publicarLote();

        assertThat(res).isEqualTo(new ResultadoLote(1, 0, true));

        var fila = jdbc.sql("SELECT publicado_en, intentos FROM evento_saliente WHERE id = :id")
                .param("id", id)
                .query((rs, r) -> Map.of("pubNull", rs.getObject("publicado_en") == null, "intentos", rs.getInt("intentos")))
                .single();
        assertThat(fila.get("pubNull")).isEqualTo(true);
        assertThat(fila.get("intentos")).isEqualTo(1);
    }

    @Test
    void excepcionDeRabbitCuentaIntento() {
        UUID id1 = sembrar(EjemplosEventos.tareaReasignada());
        UUID id2 = sembrar(EjemplosEventos.hitoEnviado());

        doThrow(new AmqpConnectException(new java.net.ConnectException("caído")))
                .when(rabbit).send(eq(Topologia.EXCHANGE_EVENTOS), anyString(), any(Message.class), any(CorrelationData.class));

        ResultadoLote res = publicador.publicarLote();

        assertThat(res).isEqualTo(new ResultadoLote(2, 0, true));

        Integer intentos1 = jdbc.sql("SELECT intentos FROM evento_saliente WHERE id = :id")
                .param("id", id1)
                .query(Integer.class)
                .single();
        Integer intentos2 = jdbc.sql("SELECT intentos FROM evento_saliente WHERE id = :id")
                .param("id", id2)
                .query(Integer.class)
                .single();

        assertThat(intentos1).isEqualTo(1);
        assertThat(intentos2).isEqualTo(0);
    }

    @Test
    void noRepublicaLoYaPublicado() {
        sembrar(EjemplosEventos.tareaReasignada());
        sembrar(EjemplosEventos.hitoEnviado());

        doAnswer(inv -> {
            CorrelationData cd = inv.getArgument(3);
            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(eq(Topologia.EXCHANGE_EVENTOS), anyString(), any(Message.class), any(CorrelationData.class));

        ResultadoLote r1 = publicador.publicarLote();
        assertThat(r1).isEqualTo(new ResultadoLote(2, 2, false));

        ResultadoLote r2 = publicador.publicarLote();
        assertThat(r2).isEqualTo(new ResultadoLote(0, 0, false));

        verify(rabbit, times(2)).send(eq(Topologia.EXCHANGE_EVENTOS), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void saltaFilasBloqueadas() throws Exception {
        UUID id1 = sembrar(EjemplosEventos.tareaReasignada());
        UUID id2 = sembrar(EjemplosEventos.hitoEnviado());
        UUID id3 = sembrar(EjemplosEventos.tareaCorreccion());

        doAnswer(inv -> {
            CorrelationData cd = inv.getArgument(3);
            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(eq(Topologia.EXCHANGE_EVENTOS), anyString(), any(Message.class), any(CorrelationData.class));

        ResultadoLote res;
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM evento_saliente ORDER BY creado_en LIMIT 1 FOR UPDATE")) {
                assertThat(rs.next()).isTrue();
                UUID bloqueadoId = rs.getObject("id", UUID.class);
                assertThat(bloqueadoId).isEqualTo(id1);

                res = publicador.publicarLote();
            } finally {
                conn.rollback();
            }
        }

        assertThat(res).isEqualTo(new ResultadoLote(2, 2, false));

        var pub1 = jdbc.sql("SELECT publicado_en FROM evento_saliente WHERE id = :id")
                .param("id", id1)
                .query((rs, r) -> rs.getObject("publicado_en"))
                .optional();
        assertThat(pub1).isEmpty();

        var pub2 = jdbc.sql("SELECT publicado_en FROM evento_saliente WHERE id = :id")
                .param("id", id2)
                .query((rs, r) -> rs.getObject("publicado_en"))
                .optional();
        assertThat(pub2).isPresent();

        var pub3 = jdbc.sql("SELECT publicado_en FROM evento_saliente WHERE id = :id")
                .param("id", id3)
                .query((rs, r) -> rs.getObject("publicado_en"))
                .optional();
        assertThat(pub3).isPresent();
    }
}
