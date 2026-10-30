package co.uniproject.nucleo.ejecucion.servicio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** RF12: porcentaje de avance y una observación opcional. */
public record SolicitudAvance(@NotNull @Min(0) @Max(100) Integer porcentajeAvance, @Size(max = 500) String observacion) {}
