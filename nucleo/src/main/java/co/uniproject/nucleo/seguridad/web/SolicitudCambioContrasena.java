package co.uniproject.nucleo.seguridad.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cambio con sesión (28-1 A). Nunca se registra ni se devuelve. */
public record SolicitudCambioContrasena(@NotNull @Size(max = 200) String contrasenaActual,
                                        @NotNull @Size(max = 200) String contrasenaNueva) {

    @Override
    public String toString() {
        return "SolicitudCambioContrasena[***]";
    }
}
