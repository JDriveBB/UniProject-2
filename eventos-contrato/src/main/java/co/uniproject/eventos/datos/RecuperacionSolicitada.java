package co.uniproject.eventos.datos;

import java.time.Instant;
import java.util.UUID;

/**
 * Olvidé mi contraseña (bloque 28). No lleva el código: el agente lo pide al núcleo por el endpoint interno justo
 * antes de enviarlo (decisión 28-2). solicitud es el id de recuperacion_contrasena.
 */
public record RecuperacionSolicitada(UUID solicitud, Persona titular, Instant venceEn) implements DatosEvento {}
