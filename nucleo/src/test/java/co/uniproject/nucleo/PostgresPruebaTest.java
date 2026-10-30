package co.uniproject.nucleo;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresPruebaTest {

    @Test
    void aplicaLaMigracionV1() {
        DataSource ds = PostgresPrueba.baseLimpia("1");
        long count = JdbcClient.create(ds)
                .sql("select count(*) from information_schema.tables where table_schema = 'public' and table_name in ('usuario','evento_saliente','codigo_vinculacion')")
                .query(Long.class)
                .single();
        assertThat(count).isEqualTo(3);
    }
}
