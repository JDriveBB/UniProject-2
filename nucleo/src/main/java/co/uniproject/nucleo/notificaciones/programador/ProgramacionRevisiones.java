package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.eventos.Topologia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProgramacionRevisiones {

    private static final Logger log = LoggerFactory.getLogger(ProgramacionRevisiones.class);

    private final RevisionRiesgo riesgo;
    private final RevisionInactividad inactividad;

    public ProgramacionRevisiones(RevisionRiesgo riesgo, RevisionInactividad inactividad) {
        this.riesgo = riesgo;
        this.inactividad = inactividad;
    }

    @Scheduled(cron = "${uniproject.programador.cron-riesgo}", zone = Topologia.ZONA)
    public void revisarRiesgo() {
        try {
            int n = riesgo.revisar();
            log.info("Revisión de riesgo: {} alertas nuevas", n);
        } catch (Exception e) {
            log.error("Error durante la revisión de riesgo", e);
        }
    }

    @Scheduled(cron = "${uniproject.programador.cron-inactividad}", zone = Topologia.ZONA)
    public void revisarInactividad() {
        try {
            int n = inactividad.revisar();
            log.info("Revisión de inactividad: {} alertas nuevas", n);
        } catch (Exception e) {
            log.error("Error durante la revisión de inactividad", e);
        }
    }
}
