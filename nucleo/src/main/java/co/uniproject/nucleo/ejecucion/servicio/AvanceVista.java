package co.uniproject.nucleo.ejecucion.servicio;

import java.time.Instant;

public record AvanceVista(long idRegistro, long idUsuario, String usuario, int porcentajeAvance, String observacion,
                          Instant fechaHora) {}
