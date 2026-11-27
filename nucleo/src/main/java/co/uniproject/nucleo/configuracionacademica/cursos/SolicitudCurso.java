package co.uniproject.nucleo.configuracionacademica.cursos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para crear o editar un curso académico. */
public record SolicitudCurso(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 20) String periodo,
        @Size(max = 2000) String descripcion) {}
