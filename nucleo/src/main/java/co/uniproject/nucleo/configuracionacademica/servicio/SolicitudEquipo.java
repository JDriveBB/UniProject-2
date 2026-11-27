package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudEquipo(
        @NotBlank @Size(max = 100) String nombreEquipo,
        Long idCurso,
        @Min(1) @Max(100) Integer cupo) {}
