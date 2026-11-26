package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Se agrega por correo: el docente conoce los correos institucionales, no los ids. Recorta antes de validar. */
public record SolicitudMiembro(
        @NotBlank @Email @Size(max = 254) String email, Boolean pasarCupo) {

    public SolicitudMiembro {
        email = email == null ? null : email.strip();
    }
}
