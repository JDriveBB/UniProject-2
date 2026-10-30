package co.uniproject.nucleo.evaluacion.servicio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** notaGrupal: opcional; si viene, reemplaza la calculada de la rúbrica (decisión 19-4). */
public record SolicitudEvaluacion(
        @NotEmpty List<@Valid CalificacionSolicitada> criterios,
        @DecimalMin("0") @DecimalMax("5") @Digits(integer = 1, fraction = 2) BigDecimal notaGrupal,
        @Size(max = 4000) String retroalimentacion) {}
