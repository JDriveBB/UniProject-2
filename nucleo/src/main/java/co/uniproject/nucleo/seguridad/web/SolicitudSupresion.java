package co.uniproject.nucleo.seguridad.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Confirmación con la contraseña actual (decisión 27-1). Nunca se registra ni se devuelve. */
public record SolicitudSupresion(@NotNull @Size(max = 200) String contrasena) {}
