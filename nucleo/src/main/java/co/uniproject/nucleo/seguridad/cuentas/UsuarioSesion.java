package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;

/** Lo que el SPA sabe del usuario de la sesión. Sin hash ni chat de Telegram: solo si está vinculado. */
public record UsuarioSesion(long idUsuario, String nombre, String email, RolPlataforma rol, boolean telegramVinculado) {}
