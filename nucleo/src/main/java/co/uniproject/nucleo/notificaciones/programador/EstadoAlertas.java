package co.uniproject.nucleo.notificaciones.programador;

import java.time.Instant;
import java.util.Optional;

public interface EstadoAlertas {
    Optional<Boolean> riesgoAnterior(long tareaId);
    void guardarRiesgo(long tareaId, boolean enRiesgo, Instant ahora);
    Optional<Instant> ultimaAlertaInactividad(long proyectoId, long estudianteId);
    void guardarAlertaInactividad(long proyectoId, long estudianteId, Instant ahora);
}
