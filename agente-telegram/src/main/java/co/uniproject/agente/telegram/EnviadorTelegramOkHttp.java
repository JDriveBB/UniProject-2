package co.uniproject.agente.telegram;

import co.uniproject.agente.plantillas.Aviso;
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
                String apiResponse = e.getApiResponse();
                if (apiResponse != null) {
                    String lower = apiResponse.toLowerCase();
                    if (lower.contains("can't parse entities") || lower.contains("button_url_invalid")) {
                        return ResultadoEnvio.MENSAJE_INVALIDO;
                    }
                }
                return ResultadoEnvio.CHAT_INVALIDO;
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

    private String escapar(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
