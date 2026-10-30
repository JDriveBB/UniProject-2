package co.uniproject.nucleo.ejecucion.servicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudComentario(@NotBlank @Size(max = 2000) String contenido) {}
