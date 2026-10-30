package co.uniproject.agente.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionTelegram {
    @Bean
    public TelegramClient telegramClient(@Value("${uniproject.telegram.token}") String token) {
        return new OkHttpTelegramClient(token);   // no hace llamadas de red al crearse
    }
}
