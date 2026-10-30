package co.uniproject.agente;

import co.uniproject.agente.consumo.ConsumidorNotificaciones;
import co.uniproject.agente.destinatarios.ReglasDestinatarios;
import co.uniproject.agente.idempotencia.RegistroProcesados;
import co.uniproject.agente.plantillas.RenderizadorAvisos;
import co.uniproject.agente.telegram.ArranqueLongPolling;
import co.uniproject.agente.telegram.EnviadorTelegram;
import co.uniproject.agente.telegram.ReceptorTelegram;
import co.uniproject.agente.vinculacion.ClienteNucleo;
import co.uniproject.eventos.CodecEventos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContextoAgenteTest {

    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6");
    private static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.3.7-management");

    static {
        postgres.start();
        rabbit.start();
    }

    @DynamicPropertySource
    static void propiedadesDinamicas(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=agente");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
        registry.add("uniproject.telegram.token", () -> "");
    }

    @Autowired
    private ApplicationContext ctx;

    @Test
    void arrancaConTodasLasPiezas() {
        assertThat(ctx.getBeanNamesForType(ConsumidorNotificaciones.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(ReglasDestinatarios.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(RenderizadorAvisos.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(EnviadorTelegram.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(RegistroProcesados.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(ClienteNucleo.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(ReceptorTelegram.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(ArranqueLongPolling.class)).hasSize(1);
        assertThat(ctx.getBeanNamesForType(CodecEventos.class)).hasSize(1);

        assertThat(ctx.getBean(ArranqueLongPolling.class).activo()).isFalse();
    }
}
