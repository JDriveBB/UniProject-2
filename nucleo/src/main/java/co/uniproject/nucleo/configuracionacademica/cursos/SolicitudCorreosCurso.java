package co.uniproject.nucleo.configuracionacademica.cursos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Correos institucionales de estudiantes que se agregan o habilitan en el curso. Recorta cada correo antes de validar;
 * un null dentro de la lista se conserva para que @NotBlank lo rechace con 400.
 */
public record SolicitudCorreosCurso(
        @NotNull @Size(min = 1, max = 200) List<@NotBlank @Email @Size(max = 254) String> emails) {

    public SolicitudCorreosCurso {
        emails = emails == null
                ? null
                : emails.stream().map(e -> e == null ? null : e.strip()).toList();
    }
}
