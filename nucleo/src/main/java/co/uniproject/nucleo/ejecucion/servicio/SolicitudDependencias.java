package co.uniproject.nucleo.ejecucion.servicio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Caso de uso "Establecer dependencias": tareas prerrequisito de la tarea de la ruta. */
public record SolicitudDependencias(@NotNull @Size(min = 1, max = 50) List<@NotNull Long> prerrequisitos) {}
