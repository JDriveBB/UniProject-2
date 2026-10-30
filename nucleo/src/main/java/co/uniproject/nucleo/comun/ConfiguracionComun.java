package co.uniproject.nucleo.comun;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.Topologia;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Beans compartidos del núcleo: programador interno (@Scheduled), reloj y códec de eventos. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
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
