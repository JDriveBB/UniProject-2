package co.uniproject.nucleo.vinculaciontelegram;

import co.uniproject.nucleo.PostgresPrueba;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoriaJdbc;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioPerfilTelegramTest {

    private JdbcClient jdbc;
    private TransactionTemplate tx;
    private RegistroAuditoriaJdbc auditoria;

    @BeforeEach
    void setUp() {
        DataSource ds = PostgresPrueba.baseLimpia("3");
        jdbc = JdbcClient.create(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        auditoria = new RegistroAuditoriaJdbc(jdbc, Clock.systemUTC());
        jdbc.sql("INSERT INTO usuario (id_usuario, nombre, email, password_hash, rol_plataforma, telegram_chat_id) "
                + "VALUES (1, 'Uno', 'uno@prueba.invalid', '!', 'ESTUDIANTE', 555)").update();
    }

    private ServicioPerfilTelegram servicio(String usuarioBot) {
        return new ServicioPerfilTelegram(new ServicioVinculacion(jdbc, tx, Duration.ofMinutes(15), usuarioBot),
                auditoria);
    }

    @Test
    void generarCodigoQuedaAuditadoSinElCodigo() {
        CodigoGenerado codigo = servicio("UniProjectBot").generarCodigo(1);
        assertThat(codigo.enlace()).isEqualTo("https://t.me/UniProjectBot?start=" + codigo.codigo());
        assertThat(jdbc.sql("SELECT accion || ':' || campo_modificado || ':' || valor_nuevo FROM log_auditoria "
                + "WHERE id_usuario_accion = 1").query(String.class).list())
                .containsExactly("VINCULAR_TELEGRAM:codigo_vinculacion:generado");
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE strpos(coalesce(valor_nuevo, ''), :codigo) > 0")
                .param("codigo", codigo.codigo()).query(Integer.class).single()).isZero();
    }

    @Test
    void sinBotConfiguradoResponde409YNoAudita() {
        assertThatThrownBy(() -> servicio("").generarCodigo(1))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ReglaNegocioException) e).getCodigo())
                .isEqualTo("TELEGRAM_NO_CONFIGURADO");
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria").query(Integer.class).single()).isZero();
    }

    @Test
    void desvincularBorraElChatYQuedaAuditado() {
        servicio("UniProjectBot").desvincular(1);
        assertThat(jdbc.sql("SELECT telegram_chat_id IS NULL FROM usuario WHERE id_usuario = 1")
                .query(Boolean.class).single()).isTrue();
        assertThat(jdbc.sql("SELECT accion FROM log_auditoria WHERE id_usuario_accion = 1").query(String.class).list())
                .containsExactly("DESVINCULAR_TELEGRAM");
    }
}
