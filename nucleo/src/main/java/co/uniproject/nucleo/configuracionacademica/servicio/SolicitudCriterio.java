package co.uniproject.nucleo.configuracionacademica.servicio;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Criterio de la rúbrica de un hito (RF1). Columnas numeric(5,2). */
public record SolicitudCriterio(
        @NotBlank @Size(max = 500) String descripcion,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") @Digits(integer = 3, fraction = 2)
        BigDecimal pesoPorcentaje,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 3, fraction = 2)
        BigDecimal puntajeMaximo) {}
