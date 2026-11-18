package co.uniproject.nucleo.comun.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.module.SimpleModule;

/**
 * Entrada JSON estricta (bloque 41): sin coerción de escalares ("1" no es número ni "true" booleano) y con texto que
 * rechaza el carácter nulo. Solo afecta al mapeador web de Spring; los eventos usan su propio mapeador (CodecEventos).
 */
@Configuration(proxyBeanMethods = false)
class ConfiguracionJson {

    @Bean
    JsonMapperBuilderCustomizer entradaJsonEstricta() {
        SimpleModule modulo = new SimpleModule("textoEstricto");
        modulo.addDeserializer(String.class, new TextoEstrictoDeserializer());
        return builder -> builder
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .addModule(modulo);
    }
}
