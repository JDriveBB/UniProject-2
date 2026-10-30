package co.uniproject.nucleo.ejecucion.servicio;

import java.math.BigDecimal;

/** Decisión 18-7. horasRestantes con 2 decimales. */
public record CargaIntegrante(long idUsuario, String nombre, String rolEquipo, int tareasPendientes,
                              int tareasCompletadas, BigDecimal horasRestantes) {}
