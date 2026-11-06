package co.uniproject.eventos.datos;

import java.time.Instant;

/** Aviso de seguridad al titular (decisión 28-11). Nunca lleva la contraseña. */
public record ContrasenaCambiada(Persona titular, Instant fecha, OrigenCambioContrasena origen) implements DatosEvento {}
