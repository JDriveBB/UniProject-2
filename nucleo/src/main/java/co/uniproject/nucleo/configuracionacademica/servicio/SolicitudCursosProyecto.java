package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Cursos que el docente asigna a un proyecto; con confirmar=false y 2 o más cursos devuelve vista previa. */
public record SolicitudCursosProyecto(
        @NotNull @Size(max = 20) List<@NotNull Long> idsCursos, boolean confirmar) {}
