package co.uniproject.nucleo.seguridad.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudLogin(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 200) String contrasena) {}
