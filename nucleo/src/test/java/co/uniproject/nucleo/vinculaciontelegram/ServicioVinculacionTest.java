package co.uniproject.nucleo.vinculaciontelegram;

import co.uniproject.eventos.interno.ContratoInterno;
import co.uniproject.nucleo.PostgresPrueba;
import java.time.Duration;
import java.time.OffsetDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ServicioVinculacionTest {

    private DataSource ds;
    private JdbcClient jdbc;
    private TransactionTemplate tx;
    private ServicioVinculacion servicio;

    @BeforeEach
    void setUp() {
        ds = PostgresPrueba.baseLimpia("1");
        jdbc = JdbcClient.create(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        servicio = new ServicioVinculacion(jdbc, tx, Duration.ofMinutes(15), "UniProjectBot");
        jdbc.sql("INSERT INTO usuario (id_usuario) VALUES (1), (2)").update();
    }

    @Test
    void generaCodigoDeUnSoloUsoYEnlace() {
        CodigoGenerado cg = servicio.generarCodigo(1);
        assertThat(cg.codigo()).matches(ContratoInterno.PATRON_CODIGO);
        assertThat(cg.enlace()).isEqualTo("https://t.me/UniProjectBot?start=" + cg.codigo());

        Number diff = jdbc.sql("SELECT extract(epoch from vence_en - creado_en) FROM codigo_vinculacion WHERE codigo = :codigo")
                .param("codigo", cg.codigo())
                .query(Double.class)
                .single();
        assertThat(diff.doubleValue()).isEqualTo(900.0);
    }

    @Test
    void generarOtroInvalidaElAnterior() {
        CodigoGenerado cg1 = servicio.generarCodigo(1);
        CodigoGenerado cg2 = servicio.generarCodigo(1);

        OffsetDateTime usado1 = jdbc.sql("SELECT usado_en FROM codigo_vinculacion WHERE codigo = :codigo")
                .param("codigo", cg1.codigo())
                .query((rs, rowNum) -> rs.getObject(1, OffsetDateTime.class))
                .single();
        OffsetDateTime usado2 = jdbc.sql("SELECT usado_en FROM codigo_vinculacion WHERE codigo = :codigo")
                .param("codigo", cg2.codigo())
                .query(OffsetDateTime.class)
                .optional()
                .orElse(null);

        assertThat(usado1).isNotNull();
        assertThat(usado2).isNull();
    }

    @Test
    void confirmarVinculaYGastaElCodigo() {
        String codigo = "V".repeat(32);
        jdbc.sql("INSERT INTO codigo_vinculacion (codigo, usuario_id, creado_en, vence_en, usado_en) VALUES (:codigo, 1, now(), now() + interval '15 minutes', NULL)")
                .param("codigo", codigo)
                .update();

        ResultadoConfirmacion res = servicio.confirmar(codigo, 777);
        assertThat(res).isEqualTo(ResultadoConfirmacion.VINCULADO);

        Long chatId = jdbc.sql("SELECT telegram_chat_id FROM usuario WHERE id_usuario = 1")
                .query(Long.class)
                .optional()
                .orElse(null);
        assertThat(chatId).isEqualTo(777L);

        OffsetDateTime usado = jdbc.sql("SELECT usado_en FROM codigo_vinculacion WHERE codigo = :codigo")
                .param("codigo", codigo)
                .query((rs, rowNum) -> rs.getObject(1, OffsetDateTime.class))
                .single();
        assertThat(usado).isNotNull();
    }

    @Test
    void segundoUsoDevuelveVencidoOUsado() {
        String codigo = "V".repeat(32);
        jdbc.sql("INSERT INTO codigo_vinculacion (codigo, usuario_id, creado_en, vence_en, usado_en) VALUES (:codigo, 1, now(), now() + interval '15 minutes', NULL)")
                .param("codigo", codigo)
                .update();

        ResultadoConfirmacion r1 = servicio.confirmar(codigo, 777);
        assertThat(r1).isEqualTo(ResultadoConfirmacion.VINCULADO);

        ResultadoConfirmacion r2 = servicio.confirmar(codigo, 777);
        assertThat(r2).isEqualTo(ResultadoConfirmacion.CODIGO_VENCIDO_O_USADO);
    }

    @Test
    void codigoVencidoNoVincula() {
        String codigo = "X".repeat(32);
        jdbc.sql("INSERT INTO codigo_vinculacion (codigo, usuario_id, creado_en, vence_en, usado_en) VALUES (:codigo, 1, now() - interval '1 hour', now() - interval '30 minutes', NULL)")
                .param("codigo", codigo)
                .update();

        ResultadoConfirmacion res = servicio.confirmar(codigo, 777);
        assertThat(res).isEqualTo(ResultadoConfirmacion.CODIGO_VENCIDO_O_USADO);

        Long chatId = jdbc.sql("SELECT telegram_chat_id FROM usuario WHERE id_usuario = 1")
                .query(Long.class)
                .optional()
                .orElse(null);
        assertThat(chatId).isNull();
    }

    @Test
    void codigoInexistente() {
        ResultadoConfirmacion res = servicio.confirmar("Z".repeat(32), 777);
        assertThat(res).isEqualTo(ResultadoConfirmacion.CODIGO_INEXISTENTE);
    }

    @Test
    void elUltimoVinculoGana() {
        jdbc.sql("UPDATE usuario SET telegram_chat_id = 777 WHERE id_usuario = 2").update();
        String codigo = "V".repeat(32);
        jdbc.sql("INSERT INTO codigo_vinculacion (codigo, usuario_id, creado_en, vence_en, usado_en) VALUES (:codigo, 1, now(), now() + interval '15 minutes', NULL)")
                .param("codigo", codigo)
                .update();

        ResultadoConfirmacion res = servicio.confirmar(codigo, 777);
        assertThat(res).isEqualTo(ResultadoConfirmacion.VINCULADO);

        Long chatUser2 = jdbc.sql("SELECT telegram_chat_id FROM usuario WHERE id_usuario = 2")
                .query(Long.class)
                .optional()
                .orElse(null);
        Long chatUser1 = jdbc.sql("SELECT telegram_chat_id FROM usuario WHERE id_usuario = 1")
                .query(Long.class)
                .single();

        assertThat(chatUser2).isNull();
        assertThat(chatUser1).isEqualTo(777L);
    }

    @Test
    void bloqueoYDesvinculacionBorranElChat() {
        jdbc.sql("UPDATE usuario SET telegram_chat_id = 777 WHERE id_usuario = 1").update();
        int borrados1 = servicio.desvincularPorChat(777);
        assertThat(borrados1).isEqualTo(1);
        Long chat1 = jdbc.sql("SELECT telegram_chat_id FROM usuario WHERE id_usuario = 1")
                .query(Long.class)
                .optional()
                .orElse(null);
        assertThat(chat1).isNull();

        int borrados2 = servicio.desvincularPorChat(888);
        assertThat(borrados2).isEqualTo(0);

        jdbc.sql("UPDATE usuario SET telegram_chat_id = 999 WHERE id_usuario = 2").update();
        String codigo = "V".repeat(32);
        jdbc.sql("INSERT INTO codigo_vinculacion (codigo, usuario_id, creado_en, vence_en, usado_en) VALUES (:codigo, 2, now(), now() + interval '15 minutes', NULL)")
                .param("codigo", codigo)
                .update();

        servicio.desvincular(2);

        Long chat2 = jdbc.sql("SELECT telegram_chat_id FROM usuario WHERE id_usuario = 2")
                .query(Long.class)
                .optional()
                .orElse(null);
        assertThat(chat2).isNull();

        OffsetDateTime usado = jdbc.sql("SELECT usado_en FROM codigo_vinculacion WHERE codigo = :codigo")
                .param("codigo", codigo)
                .query((rs, rowNum) -> rs.getObject(1, OffsetDateTime.class))
                .single();
        assertThat(usado).isNotNull();
    }

    @Test
    void sinUsuarioDelBotNoGenera() {
        ServicioVinculacion servicioSinBot = new ServicioVinculacion(jdbc, tx, Duration.ofMinutes(15), "");
        assertThatThrownBy(() -> servicioSinBot.generarCodigo(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falta TELEGRAM_BOT_USERNAME: no se puede armar el enlace de vinculación");

        Integer count = jdbc.sql("SELECT count(*) FROM codigo_vinculacion")
                .query(Integer.class)
                .single();
        assertThat(count).isEqualTo(0);
    }
}
