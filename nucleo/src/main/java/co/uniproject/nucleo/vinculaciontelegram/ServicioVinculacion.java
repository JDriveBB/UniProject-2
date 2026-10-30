package co.uniproject.nucleo.vinculaciontelegram;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ServicioVinculacion {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Duration vigencia;
    private final String usuarioBot;

    public ServicioVinculacion(JdbcClient jdbc, TransactionTemplate tx,
                               @Value("${uniproject.vinculacion.vigencia}") Duration vigencia,
                               @Value("${uniproject.telegram.usuario-bot}") String usuarioBot) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.vigencia = vigencia;
        this.usuarioBot = usuarioBot;
    }

    public CodigoGenerado generarCodigo(long usuarioId) {
        if (usuarioBot == null || usuarioBot.isBlank()) {
            throw new IllegalStateException("Falta TELEGRAM_BOT_USERNAME: no se puede armar el enlace de vinculación");
        }

        return tx.execute(status -> {
            byte[] b = new byte[24];
            ALEATORIO.nextBytes(b);
            String codigo = Base64.getUrlEncoder().withoutPadding().encodeToString(b);

            jdbc.sql("UPDATE codigo_vinculacion SET usado_en = now() WHERE usuario_id = :usuarioId AND usado_en IS NULL")
                    .param("usuarioId", usuarioId)
                    .update();

            OffsetDateTime venceEnOffset = jdbc.sql(
                            "INSERT INTO codigo_vinculacion (codigo, usuario_id, vence_en) VALUES (:codigo, :usuarioId, now() + make_interval(secs => :segundos)) RETURNING vence_en")
                    .param("codigo", codigo)
                    .param("usuarioId", usuarioId)
                    .param("segundos", vigencia.toSeconds())
                    .query((rs, rowNum) -> rs.getObject(1, OffsetDateTime.class))
                    .single();

            Instant venceEn = venceEnOffset.toInstant();
            return new CodigoGenerado(codigo, venceEn, "https://t.me/" + usuarioBot + "?start=" + codigo);
        });
    }

    public ResultadoConfirmacion confirmar(String codigo, long chatId) {
        return tx.execute(status -> {
            Optional<Long> usuarioIdOpt = jdbc.sql(
                            "UPDATE codigo_vinculacion SET usado_en = now() WHERE codigo = :codigo AND usado_en IS NULL AND vence_en > now() RETURNING usuario_id")
                    .param("codigo", codigo)
                    .query(Long.class)
                    .optional();

            if (usuarioIdOpt.isEmpty()) {
                Integer count = jdbc.sql("SELECT count(*) FROM codigo_vinculacion WHERE codigo = :codigo")
                        .param("codigo", codigo)
                        .query(Integer.class)
                        .single();
                if (count != null && count > 0) {
                    return ResultadoConfirmacion.CODIGO_VENCIDO_O_USADO;
                } else {
                    return ResultadoConfirmacion.CODIGO_INEXISTENTE;
                }
            }

            long usuarioId = usuarioIdOpt.get();

            jdbc.sql("UPDATE usuario SET telegram_chat_id = NULL WHERE telegram_chat_id = :chatId AND id_usuario <> :usuarioId")
                    .param("chatId", chatId)
                    .param("usuarioId", usuarioId)
                    .update();

            jdbc.sql("UPDATE usuario SET telegram_chat_id = :chatId WHERE id_usuario = :usuarioId")
                    .param("chatId", chatId)
                    .param("usuarioId", usuarioId)
                    .update();

            return ResultadoConfirmacion.VINCULADO;
        });
    }

    public void desvincular(long usuarioId) {
        tx.executeWithoutResult(status -> {
            jdbc.sql("UPDATE usuario SET telegram_chat_id = NULL WHERE id_usuario = :usuarioId")
                    .param("usuarioId", usuarioId)
                    .update();
            jdbc.sql("UPDATE codigo_vinculacion SET usado_en = now() WHERE usuario_id = :usuarioId AND usado_en IS NULL")
                    .param("usuarioId", usuarioId)
                    .update();
        });
    }

    public int desvincularPorChat(long chatId) {
        Integer afectadas = tx.execute(status -> jdbc.sql("UPDATE usuario SET telegram_chat_id = NULL WHERE telegram_chat_id = :chatId")
                .param("chatId", chatId)
                .update());
        return afectadas != null ? afectadas : 0;
    }
}
