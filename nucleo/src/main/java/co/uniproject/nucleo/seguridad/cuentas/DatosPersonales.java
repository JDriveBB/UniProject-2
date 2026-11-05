package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import java.time.Instant;

/**
 * Derecho a conocer (Ley 1581, decisión 27-15): lo que UniProject guarda de la cuenta. Sin hash de contraseña ni chat
 * de Telegram: solo si está vinculado. fechaAutorizacion es la prueba visible de la autorización.
 */
public record DatosPersonales(long idUsuario, String nombre, String email, RolPlataforma rol, boolean telegramVinculado,
                              boolean autorizaTratamientoDatos, Instant fechaAutorizacion, Instant fechaRegistro) {}
