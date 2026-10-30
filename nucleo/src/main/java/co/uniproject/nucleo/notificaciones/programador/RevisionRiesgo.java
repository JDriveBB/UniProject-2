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
public class RevisionRiesgo {

    private static final Logger log = LoggerFactory.getLogger(RevisionRiesgo.class);

    private final EvaluadorRiesgo evaluador;
    private final EstadoAlertas estado;
    private final RegistroEventos registro;
    private final TransactionTemplate tx;
    private final Clock reloj;

    public RevisionRiesgo(EvaluadorRiesgo evaluador, EstadoAlertas estado,
                          RegistroEventos registro, TransactionTemplate tx, Clock reloj) {
        this.evaluador = evaluador;
        this.estado = estado;
        this.registro = registro;
        this.tx = tx;
        this.reloj = reloj;
    }

    public int revisar() {
        Instant ahora = Instant.now(reloj);
        AtomicInteger publicados = new AtomicInteger();
        for (EvaluacionRiesgo ev : evaluador.evaluarTareasRutaCritica()) {
            try {
                tx.executeWithoutResult(s -> {
                    Optional<Boolean> anterior = estado.riesgoAnterior(ev.tareaId());
                    if (ev.enRiesgo() && (anterior.isEmpty() || Boolean.FALSE.equals(anterior.get()))) {
                        registro.registrar(ev.datos());
                        estado.guardarRiesgo(ev.tareaId(), true, ahora);
                        publicados.incrementAndGet();
                    } else if (anterior.isEmpty() || !anterior.get().equals(ev.enRiesgo())) {
                        estado.guardarRiesgo(ev.tareaId(), ev.enRiesgo(), ahora);
                    }
                });
            } catch (RuntimeException e) {
                log.error("Falló la revisión de riesgo de la tarea {}", ev.tareaId(), e);
            }
        }
        return publicados.get();
    }
}
