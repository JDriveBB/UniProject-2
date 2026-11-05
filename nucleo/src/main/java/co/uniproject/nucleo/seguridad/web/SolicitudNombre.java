package co.uniproject.nucleo.seguridad.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Rectificación del nombre (decisión 27-15). Se recorta antes de validar (Aclaración 1 del 14b). */
public record SolicitudNombre(@NotBlank @Size(max = 120) String nombre) {

    public SolicitudNombre {
        nombre = nombre == null ? null : nombre.strip();
    }
}
