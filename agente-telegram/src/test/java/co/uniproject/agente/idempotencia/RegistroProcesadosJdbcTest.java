package co.uniproject.agente.idempotencia;

import co.uniproject.agente.PostgresPrueba;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RegistroProcesadosJdbcTest {

    private static final UUID E = UUID.fromString("7f3c2a90-1b4e-4c1a-9d2f-0e6b5a8c3d11");

    private DataSource ds;
    private RegistroProcesadosJdbc registro;

    @BeforeEach
    void setUp() {
        ds = PostgresPrueba.baseLimpia("1");
        registro = new RegistroProcesadosJdbc(JdbcClient.create(ds));
    }

    @Test
    void nuevoNoEstaProcesado() {
        assertThat(registro.yaProcesado(E, 101L)).isFalse();
    }

    @Test
    void registrarMarcaSoloEsaPareja() {
        registro.registrar(E, 101L);

        assertThat(registro.yaProcesado(E, 101L)).isTrue();
        assertThat(registro.yaProcesado(E, 103L)).isFalse();
    }

    @Test
    void registrarDosVecesNoFalla() {
        assertDoesNotThrow(() -> {
            registro.registrar(E, 101L);
            registro.registrar(E, 101L);
        });

        long count = JdbcClient.create(ds)
                .sql("SELECT count(*) FROM evento_procesado")
                .query(Long.class)
                .single();
        assertThat(count).isEqualTo(1);
    }
}
