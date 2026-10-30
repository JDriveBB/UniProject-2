package co.uniproject.agente.telegram;

import co.uniproject.agente.plantillas.Aviso;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class EnviadorTelegramOkHttp implements EnviadorTelegram {

    private static final Logger log = LoggerFactory.getLogger(EnviadorTelegramOkHttp.class);

    private final TelegramClient cliente;
    private final String token;
    private final String urlBase;
    private final boolean botonEnlace;

    public EnviadorTelegramOkHttp(TelegramClient cliente,
                                  @Value("${uniproject.telegram.token}") String token,
                                  @Value("${uniproject.url-base}") String urlBase,
                                  @Value("${uniproject.telegram.boton-enlace}") boolean botonEnlace) {
        this.cliente = cliente;
        this.token = token;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.botonEnlace = botonEnlace;
    }

    @Override
    public ResultadoEnvio enviar(long chatId, Aviso aviso) {
        if (token.isBlank()) {
            log.warn("TELEGRAM_BOT_TOKEN vacío: no se puede enviar");
            return ResultadoEnvio.TEMPORAL;
        }

        String url = urlBase + aviso.ruta();
        String texto;
        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(chatId)
                .parseMode(ParseMode.HTML);

        if (botonEnlace) {
            texto = aviso.html();
            InlineKeyboardButton boton = InlineKeyboardButton.builder()
                    .text(aviso.textoBoton())
                    .url(url)
                    .build();
            InlineKeyboardMarkup replyMarkup = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(boton))
                    .build();
            builder.replyMarkup(replyMarkup);
        } else {
            texto = aviso.html() + "\n\n" + escapar(aviso.textoBoton()) + ": " + escapar(url);
        }

        SendMessage mensaje = builder.text(texto).build();

        try {
            cliente.execute(mensaje);
            return ResultadoEnvio.ENVIADO;
        } catch (TelegramApiRequestException e) {
            Integer codigo = e.getErrorCode();
            if (codigo == null) {
                return ResultadoEnvio.TEMPORAL;
            }
            int estado = codigo;
            if (estado == 429 || estado >= 500) {
                return ResultadoEnvio.TEMPORAL;
            }
            if (estado == 403) {
                return ResultadoEnvio.BLOQUEADO;
            }
            if (estado == 400) {
                String motivo = motivo400(e.getApiResponse());
                log.warn("Telegram rechazó el aviso con 400 (motivo {})", motivo);
                return motivo.equals("chat") ? ResultadoEnvio.CHAT_INVALIDO : ResultadoEnvio.MENSAJE_INVALIDO;
            }
            if (estado == 401 || estado == 404) {
                log.error("Telegram rechazó el token del bot (estado {})", estado);
                return ResultadoEnvio.TEMPORAL;
            }
            return ResultadoEnvio.TEMPORAL;
        } catch (TelegramApiException | RuntimeException e) {
            return ResultadoEnvio.TEMPORAL;
        }
    }

    /**
     * Motivo de un 400 de la Bot API (decisiones-tecnicas-java §11). Solo el chat es culpa del destinatario;
     * lo demás (HTML, botón o un 400 que no se reconoce) es del mensaje y va a fallidos, donde queda visible.
     * Devuelve una etiqueta fija: la descripción de Telegram no se registra (trae la URL del botón).
     */
    private static String motivo400(String apiResponse) {
        if (apiResponse == null) {
            return "otro";
        }
        String descripcion = apiResponse.toLowerCase(Locale.ROOT);
        if (descripcion.contains("chat not found") || descripcion.contains("peer_id_invalid")) {
            return "chat";
        }
        if (descripcion.contains("can't parse entities")) {
            return "html";
        }
        if (descripcion.contains("inline keyboard button") || descripcion.contains("button_url_invalid")) {
            return "boton";
        }
        return "otro";
    }

    private String escapar(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
