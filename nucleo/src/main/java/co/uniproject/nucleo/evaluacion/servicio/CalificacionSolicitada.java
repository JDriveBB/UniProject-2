package co.uniproject.nucleo.evaluacion.servicio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CalificacionSolicitada(
        @NotNull Long idCriterio,
        @NotNull @DecimalMin("0") @Digits(integer = 3, fraction = 2) BigDecimal nota,
        @Size(max = 2000) String retroalimentacion) {}
