package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.eventos.Topologia;
import co.uniproject.nucleo.configuracionacademica.votacion.RevisionRondas;
import co.uniproject.nucleo.ejecucion.servicio.RefrescoRiesgo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProgramacionRevisiones {

    private static final Logger log = LoggerFactory.getLogger(ProgramacionRevisiones.class);

    private final RevisionRiesgo riesgo;
    private final RevisionInactividad inactividad;
    private final RefrescoRiesgo refresco;
    private final RevisionRondas rondas;

    public ProgramacionRevisiones(
            RevisionRiesgo riesgo, RevisionInactividad inactividad, RefrescoRiesgo refresco, RevisionRondas rondas) {
        this.riesgo = riesgo;
        this.inactividad = inactividad;
        this.refresco = refresco;
        this.rondas = rondas;
    }

    @Scheduled(cron = "${uniproject.programador.cron-riesgo}", zone = Topologia.ZONA)
    public void revisarRiesgo() {
        try {
            int hitos = refresco.refrescarEnCurso();
            log.info("Recálculo de riesgo: {} hitos", hitos);
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

    @Scheduled(cron = "${uniproject.programador.cron-rondas}", zone = Topologia.ZONA)
    public void revisarRondas() {
        try {
            int cerradas = rondas.cerrarVencidas();
            log.info("Revisión de rondas: {} cerradas", cerradas);
        } catch (Exception e) {
            log.error("Error durante el cierre de rondas vencidas", e);
        }
        try {
            int abiertas = rondas.abrirProgramadas();
            log.info("Revisión de rondas: {} abiertas", abiertas);
        } catch (Exception e) {
            log.error("Error durante la apertura programada de rondas", e);
        }
        try {
            int recordados = rondas.recordarCierres();
            log.info("Revisión de rondas: {} recordatorios de cierre", recordados);
        } catch (Exception e) {
            log.error("Error durante el recordatorio de cierres", e);
        }
    }
}
