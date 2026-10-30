package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.nucleo.PostgresPrueba;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoAlertasJdbcTest {

    private static final Instant T1 = Instant.parse("2026-10-24T19:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-24T20:00:00Z");

    private JdbcClient jdbc;
    private EstadoAlertasJdbc estado;

    @BeforeEach
    void setUp() {
        DataSource ds = PostgresPrueba.baseLimpia("2");
        jdbc = JdbcClient.create(ds);
        estado = new EstadoAlertasJdbc(jdbc);
    }

    @Test
    void riesgoSinFilaEsVacio() {
        assertThat(estado.riesgoAnterior(45)).isEmpty();
    }

    @Test
    void guardarRiesgoInsertaYActualiza() {
        estado.guardarRiesgo(45, true, T1);
        estado.guardarRiesgo(45, false, T2);

        assertThat(estado.riesgoAnterior(45)).contains(false);
        long count = jdbc.sql("SELECT count(*) FROM estado_riesgo_tarea WHERE tarea_id = :id")
                .param("id", 45)
                .query(Long.class)
                .single();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void alertaInactividadInsertaYActualiza() {
        assertThat(estado.ultimaAlertaInactividad(12, 102)).isEmpty();

        estado.guardarAlertaInactividad(12, 102, T1);
        estado.guardarAlertaInactividad(12, 102, T2);

        assertThat(estado.ultimaAlertaInactividad(12, 102)).contains(T2);
        long count = jdbc.sql("SELECT count(*) FROM alerta_inactividad WHERE proyecto_id = :proyectoId AND estudiante_id = :estudianteId")
                .param("proyectoId", 12)
                .param("estudianteId", 102)
                .query(Long.class)
                .single();
        assertThat(count).isEqualTo(1);
    }
}
