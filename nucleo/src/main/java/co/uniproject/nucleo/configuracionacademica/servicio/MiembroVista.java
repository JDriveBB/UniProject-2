package co.uniproject.nucleo.configuracionacademica.servicio;

import co.uniproject.nucleo.configuracionacademica.dominio.RolEquipo;

/** Sin correo: los compañeros solo ven el nombre (Ley 1581, dato mínimo). */
public record MiembroVista(long idUsuario, String nombre, RolEquipo rolEquipo) {}
