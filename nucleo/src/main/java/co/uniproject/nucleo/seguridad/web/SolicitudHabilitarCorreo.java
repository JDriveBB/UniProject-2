package co.uniproject.nucleo.seguridad.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Se quitan los espacios alrededor al construir, antes de validar: @Email ve el correo ya recortado. */
public record SolicitudHabilitarCorreo(@NotBlank @Email @Size(max = 254) String email) {

    public SolicitudHabilitarCorreo {
        email = email == null ? null : email.strip();
    }
}
