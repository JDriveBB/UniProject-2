package co.uniproject.agente.vinculacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionClienteNucleo {
    @Bean
    public ClienteNucleo clienteNucleo(RestClient.Builder builder,
                                       @Value("${uniproject.nucleo-url}") String urlNucleo,
                                       @Value("${uniproject.token-interno}") String tokenInterno) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        return new ClienteNucleoRest(builder.requestFactory(fabrica), urlNucleo, tokenInterno);
    }
}
