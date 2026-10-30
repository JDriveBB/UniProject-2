package co.uniproject.nucleo.ejecucion.servicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudApoyo(@NotBlank @Size(max = 500) String mensaje) {}
