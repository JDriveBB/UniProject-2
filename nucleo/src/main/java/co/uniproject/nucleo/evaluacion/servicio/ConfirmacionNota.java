package co.uniproject.nucleo.evaluacion.servicio;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConfirmacionNota(@NotNull @DecimalMin("0") @DecimalMax("5") @Digits(integer = 1, fraction = 2)
                               BigDecimal notaFinal) {}
