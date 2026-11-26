package co.uniproject.nucleo.configuracionacademica.cursos;

import jakarta.validation.constraints.NotNull;

/** Solicitud para archivar o desarchivar un curso. */
public record SolicitudArchivado(@NotNull Boolean archivado) {}
