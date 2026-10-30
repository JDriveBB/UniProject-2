package co.uniproject.nucleo.ejecucion.servicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** RF5: evidencia revisada y puntos a corregir. ≤ 1000 porque es el "resumen" del evento tarea.correccion. */
public record PeticionCorreccion(@NotNull Long idEvidencia, @NotBlank @Size(max = 1000) String observaciones) {}
