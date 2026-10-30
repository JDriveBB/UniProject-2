package co.uniproject.nucleo.seguridad.cuentas;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de cuenta. autorizaTratamientoDatos debe ser true (Ley 1581 de 2012). */
public record SolicitudRegistro(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(min = 10, max = 72) String contrasena,
        @AssertTrue boolean autorizaTratamientoDatos) {}
