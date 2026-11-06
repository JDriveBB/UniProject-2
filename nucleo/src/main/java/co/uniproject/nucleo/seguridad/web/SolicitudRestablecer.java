package co.uniproject.nucleo.seguridad.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Código de Telegram y contraseña nueva. Las reglas de la contraseña las aplica el servicio (CONTRASENA_INVALIDA). */
public record SolicitudRestablecer(@NotNull @Size(max = 100) String codigo,
                                   @NotNull @Size(max = 200) String contrasenaNueva) {

    @Override
    public String toString() {
        return "SolicitudRestablecer[***]";
    }
}
