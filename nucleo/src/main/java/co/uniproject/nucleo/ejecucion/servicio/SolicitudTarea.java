package co.uniproject.nucleo.ejecucion.servicio;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Crear o reemplazar (PUT) una tarea (RF2). idEncargado nulo = sin asignar; fechaLimite nula = la del hito;
 * esPrerrequisitoCritico nulo = false. horas_estimadas es numeric(6,2).
 */
public record SolicitudTarea(
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 2000) String descripcion,
        Long idEncargado,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2)
        BigDecimal horasEstimadas,
        LocalDate fechaLimite,
        Boolean esPrerrequisitoCritico) {}
