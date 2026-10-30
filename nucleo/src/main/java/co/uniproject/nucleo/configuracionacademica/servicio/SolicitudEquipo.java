package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudEquipo(@NotBlank @Size(max = 100) String nombreEquipo) {}
