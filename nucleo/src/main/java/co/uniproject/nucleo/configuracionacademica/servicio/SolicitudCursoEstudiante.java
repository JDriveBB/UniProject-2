package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.NotNull;

/** Curso elegido por el docente para que cuente en el proyecto para un estudiante repetido. */
public record SolicitudCursoEstudiante(@NotNull Long idCurso) {}
