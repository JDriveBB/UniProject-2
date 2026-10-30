package co.uniproject.nucleo.notificaciones.programador;

import co.uniproject.nucleo.notificaciones.RegistroEventos;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class RevisionInactividad {

    private static final Logger log = LoggerFactory.getLogger(RevisionInactividad.class);

    private final DetectorInactividad detector;
    private final EstadoAlertas estado;
    private final RegistroEventos registro;
    private final TransactionTemplate tx;
    private final Clock reloj;

    public RevisionInactividad(DetectorInactividad detector, EstadoAlertas estado,
                               RegistroEventos registro, TransactionTemplate tx, Clock reloj) {
        this.detector = detector;
        this.estado = estado;
        this.registro = registro;
        this.tx = tx;
        this.reloj = reloj;
    }

    public int revisar() {
        Instant ahora = Instant.now(reloj);
        AtomicInteger publicados = new AtomicInteger();
        for (CasoInactividad caso : detector.buscarInactivos(ahora)) {
            try {
                tx.executeWithoutResult(s -> {
                    Optional<Instant> ultima = estado.ultimaAlertaInactividad(caso.proyectoId(), caso.estudianteId());
                    if (ultima.isEmpty() || ultima.get().isBefore(caso.ultimaActividad())) {
                        registro.registrar(caso.datos());
                        estado.guardarAlertaInactividad(caso.proyectoId(), caso.estudianteId(), ahora);
                        publicados.incrementAndGet();
                    }
                });
            } catch (RuntimeException e) {
                log.error("Falló la revisión de inactividad del estudiante {} en el proyecto {}",
                        caso.estudianteId(), caso.proyectoId(), e);
            }
        }
        return publicados.get();
    }
}
