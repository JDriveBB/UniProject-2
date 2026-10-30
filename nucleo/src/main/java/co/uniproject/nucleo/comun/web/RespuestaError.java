package co.uniproject.nucleo.comun.web;

import java.util.List;

/** Cuerpo JSON de todo error de la API del dominio. codigo: constante en MAYÚSCULAS; mensaje: para el usuario. */
public record RespuestaError(String codigo, String mensaje, List<String> detalles) {}
