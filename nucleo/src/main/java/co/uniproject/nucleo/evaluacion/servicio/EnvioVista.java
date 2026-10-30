package co.uniproject.nucleo.evaluacion.servicio;

import java.time.Instant;

public record EnvioVista(long idEnvio, long idHitoEquipo, long idUsuarioEnvia, String enviadoPor, Instant fechaEnvio) {}
