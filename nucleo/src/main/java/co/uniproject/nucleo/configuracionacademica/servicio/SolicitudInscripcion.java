package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Configuración de autoinscripción a equipos: cierre (null la cierra) y duración de las rondas de líder. */
public record SolicitudInscripcion(
        Instant cierreEquipos, @NotNull @Min(1) @Max(336) Integer horasRonda) {}
