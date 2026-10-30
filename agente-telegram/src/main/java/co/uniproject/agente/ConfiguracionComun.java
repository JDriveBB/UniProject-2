package co.uniproject.agente;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.Topologia;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans compartidos del agente: reloj y códec de eventos. */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionComun {

    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of(Topologia.ZONA));
    }

    @Bean
    public CodecEventos codecEventos() {
        return new CodecEventos();
    }
}
