package co.uniproject.agente.telegram;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class ArranqueLongPolling {

    private static final Logger log = LoggerFactory.getLogger(ArranqueLongPolling.class);

    private final ReceptorTelegram receptor;
    private final String token;
    private TelegramBotsLongPollingApplication aplicacion;

    public ArranqueLongPolling(ReceptorTelegram receptor,
                               @Value("${uniproject.telegram.token}") String token) {
        this.receptor = receptor;
        this.token = token;
    }

    @PostConstruct
    public void iniciar() {
        if (token.isBlank()) {
            log.warn("TELEGRAM_BOT_TOKEN vacío: el bot no recibe mensajes");
            return;
        }

        try {
            aplicacion = new TelegramBotsLongPollingApplication();
            aplicacion.registerBot(token, receptor);
            log.info("Bot de Telegram escuchando (long polling)");
        } catch (TelegramApiException e) {
            throw new IllegalStateException("No se pudo registrar el bot de Telegram", e);
        }
    }

    @PreDestroy
    public void detener() {
        if (aplicacion != null) {
            try {
                aplicacion.close();
            } catch (Exception e) {
                log.warn("Error al detener long polling ({})", e.getClass().getName());
            }
        }
    }

    public boolean activo() {
        return aplicacion != null;
    }
}
