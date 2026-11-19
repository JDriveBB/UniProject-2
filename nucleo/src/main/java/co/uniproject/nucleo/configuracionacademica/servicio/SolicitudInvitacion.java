package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** D-12 20A: correo habilitado de un estudiante sin cuenta que entrará al equipo al registrarse. Recorta antes de validar. */
public record SolicitudInvitacion(
        @NotBlank @Email @Size(max = 254) String email) {

    public SolicitudInvitacion {
        email = email == null ? null : email.strip();
    }
}
