package co.uniproject.agente;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilidadTelegramBotsTest {

    @Test
    void serializaMensajeHtmlConBoton() throws Exception {
        InlineKeyboardButton boton = InlineKeyboardButton.builder()
                .text("Ver tarea")
                .url("https://example.org/tareas/45")
                .build();

        InlineKeyboardRow fila = new InlineKeyboardRow(boton);
        InlineKeyboardMarkup teclado = new InlineKeyboardMarkup(List.of(fila));

        SendMessage mensaje = SendMessage.builder()
                .chatId(123L)
                .text("<b>Prueba</b>")
                .parseMode(ParseMode.HTML)
                .replyMarkup(teclado)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(mensaje);

        assertTrue(json.contains("\"parse_mode\":\"HTML\"") || json.contains("\"parse_mode\":\"html\""));
        assertTrue(json.contains("\"url\":\"https://example.org/tareas/45\""));
    }

    @Test
    void deserializaUpdateDeStart() throws Exception {
        String json = "{\"update_id\":1,\"message\":{\"message_id\":2,\"date\":1700000000,\"chat\":{\"id\":123,\"type\":\"private\"},\"from\":{\"id\":123,\"is_bot\":false,\"first_name\":\"A\"},\"text\":\"/start abc\"}}";

        ObjectMapper mapper = new ObjectMapper();
        Update update = mapper.readValue(json, Update.class);

        assertEquals(123L, update.getMessage().getChatId());
        assertEquals("/start abc", update.getMessage().getText());
        assertTrue(update.getMessage().getChat().isUserChat());
    }

    @Test
    void creaClienteOkHttp() {
        assertDoesNotThrow(() -> new OkHttpTelegramClient("123:token-falso"));
    }
}
