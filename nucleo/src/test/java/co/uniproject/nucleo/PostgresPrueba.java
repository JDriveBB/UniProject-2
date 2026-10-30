package co.uniproject.nucleo;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL 18.6 real para las pruebas (un contenedor por JVM). */
public final class PostgresPrueba {

    private static final PostgreSQLContainer CONTENEDOR = new PostgreSQLContainer("postgres:18.6");

    static {
        CONTENEDOR.start();
    }

    private PostgresPrueba() {}

    /** Borra todo y aplica classpath:db/migration hasta la versión indicada ("1", "2", …). */
    public static DataSource baseLimpia(String versionObjetivo) {
        DataSource ds = new DriverManagerDataSource(CONTENEDOR.getJdbcUrl(), CONTENEDOR.getUsername(), CONTENEDOR.getPassword());
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .target(versionObjetivo)
                .load();
        flyway.clean();
        flyway.migrate();
        return ds;
    }
}
