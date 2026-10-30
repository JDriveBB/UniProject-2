package co.uniproject.nucleo.comun;

import co.uniproject.eventos.Topologia;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Decisión R-6: las fechas límite son días completos de America/Bogota. */
public final class FechasDominio {

    public static final ZoneId ZONA = ZoneId.of(Topologia.ZONA);

    private FechasDominio() {
    }

    /** Instante en que vence la fecha: el inicio del día siguiente en Bogotá. */
    public static Instant venceEn(LocalDate fecha) {
        return fecha.plusDays(1).atStartOfDay(ZONA).toInstant();
    }

    /** Fecha de hoy en Bogotá para ese instante. */
    public static LocalDate hoy(Instant ahora) {
        return LocalDate.ofInstant(ahora, ZONA);
    }
}
