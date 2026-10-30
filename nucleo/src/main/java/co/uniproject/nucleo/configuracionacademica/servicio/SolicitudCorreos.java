package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * K2: correos institucionales que podrán crear cuenta en el proyecto. rol: ESTUDIANTE o MONITOR.
 * Recorta cada correo antes de validar (Aclaración 1 del bloque 14b); un null dentro de la lista se conserva para
 * que @NotBlank lo rechace con 400.
 */
public record SolicitudCorreos(
        @NotNull @Size(min = 1, max = 200) List<@NotBlank @Email @Size(max = 254) String> emails,
        @NotNull RolPlataforma rol) {

    public SolicitudCorreos {
        emails = emails == null ? null : emails.stream().map(e -> e == null ? null : e.strip()).toList();
    }
}
