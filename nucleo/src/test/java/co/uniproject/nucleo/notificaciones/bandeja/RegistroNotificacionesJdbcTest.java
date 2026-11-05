package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import co.uniproject.nucleo.PostgresPrueba;
import co.uniproject.nucleo.notificaciones.RegistroEventosJdbc;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroNotificacionesJdbcTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-24T19:05:00Z"), ZoneId.of("America/Bogota"));

    private JdbcClient jdbc;
    private TransactionTemplate tx;
    private RegistroNotificacionesJdbc bandeja;

    @BeforeEach
    void setUp() {
        DataSource ds = PostgresPrueba.baseLimpia("4");
        jdbc = JdbcClient.create(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        bandeja = new RegistroNotificacionesJdbc(jdbc);
        jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) VALUES "
                + "(100, 'Estudiante C', 'c@prueba.invalid', '!', 'ESTUDIANTE'), "
                + "(101, 'Estudiante A', 'a@prueba.invalid', '!', 'ESTUDIANTE'), "
                + "(102, 'Estudiante B', 'b@prueba.invalid', '!', 'ESTUDIANTE'), "
                + "(103, 'Estudiante E', 'e@prueba.invalid', '!', 'ESTUDIANTE'), "
                + "(200, 'Docente D', 'd@prueba.invalid', '!', 'DOCENTE')").update();
    }

    private List<String> filas() {
        return jdbc.sql("SELECT id_destinatario || ':' || estado_envio || ':' || tipo || ':' || entidad_referencia "
                        + "|| ':' || id_referencia FROM notificacion ORDER BY id_destinatario")
                .query(String.class)
                .list();
    }

    private int contar(String tabla) {
        return jdbc.sql("SELECT count(*) FROM " + tabla).query(Integer.class).single();
    }

    @Test
    void unaFilaPorDestinatarioConSuEstadoDeEnvio() {
        Integer insertadas = tx.execute(s -> bandeja.registrar(
                EventoCloud.nuevo(EjemplosEventos.tareaRutaCriticaRiesgo(), RELOJ)));
        assertThat(insertadas).isEqualTo(4);
        assertThat(filas()).containsExactly(
                "100:EN_COLA:tarea.ruta_critica.riesgo:tarea:45",
                "101:EN_COLA:tarea.ruta_critica.riesgo:tarea:45",
                "102:SIN_TELEGRAM:tarea.ruta_critica.riesgo:tarea:45",
                "200:EN_COLA:tarea.ruta_critica.riesgo:tarea:45");
        assertThat(jdbc.sql("SELECT DISTINCT fecha_creacion FROM notificacion").query(OffsetDateTime.class).single()
                .toInstant()).isEqualTo(Instant.parse("2026-10-24T19:05:00Z"));
        assertThat(jdbc.sql("SELECT count(*) FROM notificacion WHERE leida").query(Integer.class).single()).isZero();
    }

    @Test
    void laReasignacionDaUnMensajeACadaLado() {
        tx.executeWithoutResult(s -> bandeja.registrar(EventoCloud.nuevo(EjemplosEventos.tareaReasignada(), RELOJ)));
        assertThat(jdbc.sql("SELECT id_destinatario || ' ' || mensaje FROM notificacion ORDER BY id_destinatario")
                .query(String.class).list())
                .containsExactly(
                        "101 La tarea \"Diagramas de secuencia\" se reasignó a Estudiante E.",
                        "103 Se te asignó la tarea \"Diagramas de secuencia\" en Equipo 4.");
    }

    @Test
    void vaEnLaMismaTransaccionQueLaOutbox() {
        RegistroEventosJdbc registro = new RegistroEventosJdbc(jdbc, new CodecEventos(), RELOJ, bandeja);
        tx.executeWithoutResult(s -> {
            registro.registrar(EjemplosEventos.hitoEnviado());
            s.setRollbackOnly();
        });
        assertThat(contar("evento_saliente")).isZero();
        assertThat(contar("notificacion")).isZero();
        tx.executeWithoutResult(s -> registro.registrar(EjemplosEventos.hitoEnviado()));
        assertThat(contar("evento_saliente")).isEqualTo(1);
        assertThat(filas()).containsExactly("200:EN_COLA:hito.enviado:hito_equipo:7");
    }
}
