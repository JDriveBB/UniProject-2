package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Crear o reemplazar un hito (RF1) con su alerta de inactividad (RF6). diasInactividadAlerta nulo = sin alerta
 * (bloque 30); con valor, de 1 a 60; notificarDocenteInactividad nulo = false (la alerta siempre va al líder; el
 * docente decide si también la recibe; sin días no tiene efecto).
 */
public record SolicitudHito(
        @NotBlank @Size(max = 150) String nombreHito,
        @Size(max = 2000) String descripcion,
        @NotNull @Min(1) Integer orden,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaLimite,
        @Min(1) @Max(60) Integer diasInactividadAlerta,
        Boolean notificarDocenteInactividad) {}
