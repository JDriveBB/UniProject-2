package co.uniproject.nucleo.seguridad.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Olvidé mi contraseña. Sin @Email: un correo mal escrito responde igual que uno sin cuenta (decisión 28-5). */
public record SolicitudRecuperacion(@NotBlank @Size(max = 254) String email) {

    public SolicitudRecuperacion {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "SolicitudRecuperacion[email=***]";
    }
}
