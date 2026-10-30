package co.uniproject.agente;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresPruebaTest {

    @Test
    void aplicaLaMigracionV1DelAgente() {
        DataSource ds = PostgresPrueba.baseLimpia("1");
        long count = JdbcClient.create(ds)
                .sql("select count(*) from information_schema.tables where table_schema = 'agente' and table_name = 'evento_procesado'")
                .query(Long.class)
                .single();
        assertThat(count).isEqualTo(1);
    }
}
