package co.uniproject.nucleo.ejecucion.servicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RF12: enlace a un repositorio. Se recorta antes de validar. */
public record SolicitudEnlace(@NotBlank @Size(max = 2048) String url) {

    public SolicitudEnlace {
        url = url == null ? null : url.strip();
    }
}
