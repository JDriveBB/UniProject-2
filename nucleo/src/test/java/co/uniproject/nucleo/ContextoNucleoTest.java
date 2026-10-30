package co.uniproject.nucleo;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.nucleo.notificaciones.PublicadorOutbox;
import co.uniproject.nucleo.notificaciones.RegistroEventos;
import co.uniproject.nucleo.notificaciones.programador.DetectorInactividad;
import co.uniproject.nucleo.notificaciones.programador.EstadoAlertas;
import co.uniproject.nucleo.notificaciones.programador.EvaluadorRiesgo;
import co.uniproject.nucleo.notificaciones.programador.RevisionInactividad;
import co.uniproject.nucleo.notificaciones.programador.RevisionRiesgo;
import co.uniproject.nucleo.vinculaciontelegram.ServicioVinculacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ContextoNucleoTest {

    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6");
    private static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.3.7-management");

    static {
        postgres.start();
        rabbit.start();
    }

    @DynamicPropertySource
    static void propiedadesDinamicas(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("uniproject.token-interno", () -> "token-de-prueba");
        registry.add("uniproject.outbox.intervalo-ms", () -> 600000L);
    }

    @Autowired
    private ApplicationContext ctx;

    @Test
    void arrancaConTodasLasPiezas() {
        assertThat(ctx.getBeanNamesForType(RegistroEventos.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(PublicadorOutbox.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(EvaluadorRiesgo.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(DetectorInactividad.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(EstadoAlertas.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(RevisionRiesgo.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(RevisionInactividad.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(ServicioVinculacion.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(CodecEventos.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(Clock.class)).hasSize(1);

        assertThat(ctx.getBeanNamesForType(SecurityFilterChain.class)).hasSize(2);

        JdbcClient jdbc = ctx.getBean(JdbcClient.class);
        Integer cantidadTablas = jdbc.sql(
                "select count(*) from information_schema.tables " +
                "where table_schema = 'public' and table_name in ('estado_riesgo_tarea', 'alerta_inactividad')")
                .query(Integer.class)
                .single();
        assertThat(cantidadTablas).isEqualTo(2);
    }
}
