package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear varios equipos con cupo en un solo paso. */
public record SolicitudEquiposLote(
        @NotBlank @Size(max = 90) String prefijo,
        @Min(1) @Max(50) Integer cantidad,
        Long idCurso,
        @NotNull @Min(1) @Max(100) Integer cupo) {}
