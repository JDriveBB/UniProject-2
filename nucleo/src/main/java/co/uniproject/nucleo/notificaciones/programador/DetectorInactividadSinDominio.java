package co.uniproject.nucleo.notificaciones.programador;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DetectorInactividadSinDominio implements DetectorInactividad {

    private static final Logger log = LoggerFactory.getLogger(DetectorInactividadSinDominio.class);

    public DetectorInactividadSinDominio() {
        log.info("Sin dominio todavía: DetectorInactividad devuelve una lista vacía");
    }

    @Override
    public List<CasoInactividad> buscarInactivos(Instant ahora) {
        return List.of();
    }
}
