package co.uniproject.agente.telegram;

import co.uniproject.agente.vinculacion.ClienteNucleo;
import co.uniproject.agente.vinculacion.ResultadoVinculacion;
import co.uniproject.eventos.interno.ContratoInterno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class ReceptorTelegram implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReceptorTelegram.class);

    private final TelegramClient cliente;
    private final ClienteNucleo clienteNucleo;

    public ReceptorTelegram(TelegramClient cliente, ClienteNucleo clienteNucleo) {
        this.cliente = cliente;
        this.clienteNucleo = clienteNucleo;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText() || !update.getMessage().getChat().isUserChat()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String texto = update.getMessage().getText().strip();
        String respuesta;

        if (texto.equals("/start")) {
            respuesta = TextosBot.SIN_CODIGO;
        } else if (texto.startsWith("/start ")) {
            String codigo = texto.substring(7).strip();
            if (!codigo.matches(ContratoInterno.PATRON_CODIGO)) {
                respuesta = TextosBot.CODIGO_INEXISTENTE;
            } else {
                ResultadoVinculacion resultado = clienteNucleo.vincular(codigo, chatId);
                respuesta = switch (resultado) {
                    case VINCULADO -> TextosBot.VINCULADO;
                    case CODIGO_INEXISTENTE -> TextosBot.CODIGO_INEXISTENTE;
                    case CODIGO_VENCIDO_O_USADO -> TextosBot.CODIGO_VENCIDO;
                    case ERROR -> TextosBot.ERROR;
                };
            }
        } else {
            respuesta = TextosBot.SOLO_AVISOS;
        }

        try {
            cliente.execute(SendMessage.builder().chatId(chatId).text(respuesta).build());
        } catch (TelegramApiException | RuntimeException e) {
            log.warn("No se pudo enviar la respuesta de Telegram ({})", e.getClass().getName());
        }
    }
}
