package co.uniproject.nucleo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** V10: la rama con enlace del CHECK exige selector y verificador no nulos. */
class MigracionEnlaceRegistroTest {

    private static final String INSERT = "INSERT INTO correo_habilitado (email, rol_plataforma, id_habilitado_por, "
            + "enlace_selector, enlace_verificador_hash, enlace_vence_en) VALUES (:e, 'DOCENTE', 1, %s, %s, %s)";
    private static final String SELECTOR = "decode('0102030405060708', 'hex')";
    private static final String HASH = "decode(repeat('ab', 32), 'hex')";
    private static final String VENCE = "now() + interval '1 day'";

    private static JdbcClient base() {
        DataSource ds = PostgresPrueba.baseLimpia("10");
        JdbcClient jdbc = JdbcClient.create(ds);
        jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma) "
                + "VALUES (1, 'Admin', 'admin@u.edu.co', '!', 'ADMINISTRADOR')").update();
        return jdbc;
    }

    private static void insertar(JdbcClient jdbc, String email, String selector, String hash, String vence) {
        jdbc.sql(INSERT.formatted(selector, hash, vence)).param("e", email).update();
    }

    @Test
    void selectorSinVerificadorOVerificadorSinSelectorYaNoPasan() {
        JdbcClient jdbc = base();
        assertThatThrownBy(() -> insertar(jdbc, "a@u.edu.co", SELECTOR, "NULL", VENCE))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, "b@u.edu.co", "NULL", HASH, VENCE))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, "c@u.edu.co", SELECTOR, HASH, "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, "d@u.edu.co", "decode('0102', 'hex')", HASH, VENCE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lasFilasValidasSiguenPasando() {
        JdbcClient jdbc = base();
        insertar(jdbc, "ok@u.edu.co", SELECTOR, HASH, VENCE);
        insertar(jdbc, "sin@u.edu.co", "NULL", "NULL", "NULL");
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado").query(Integer.class).single()).isEqualTo(2);
    }
}
