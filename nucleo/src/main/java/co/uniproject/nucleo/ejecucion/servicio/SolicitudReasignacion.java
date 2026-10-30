package co.uniproject.nucleo.ejecucion.servicio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** tituloParcial y horasParcial solo cuentan (y son obligatorios) si tipo = PARCIAL. */
public record SolicitudReasignacion(
        @NotNull Long idNuevoEncargado,
        @NotNull TipoReasignacion tipo,
        @Size(max = 150) String tituloParcial,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 4, fraction = 2) BigDecimal horasParcial,
        @Size(max = 500) String motivo) {}
