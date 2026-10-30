package co.uniproject.nucleo.notificaciones.programador;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EvaluadorRiesgoSinDominio implements EvaluadorRiesgo {

    private static final Logger log = LoggerFactory.getLogger(EvaluadorRiesgoSinDominio.class);

    public EvaluadorRiesgoSinDominio() {
        log.info("Sin dominio todavía: EvaluadorRiesgo devuelve una lista vacía");
    }

    @Override
    public List<EvaluacionRiesgo> evaluarTareasRutaCritica() {
        return List.of();
    }
}
