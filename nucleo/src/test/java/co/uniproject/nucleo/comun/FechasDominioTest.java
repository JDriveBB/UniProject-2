package co.uniproject.nucleo.comun;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FechasDominioTest {

    @Test
    void venceAlTerminarElDiaEnBogota() {
        assertThat(FechasDominio.venceEn(LocalDate.of(2026, 9, 15))).isEqualTo(Instant.parse("2026-09-16T05:00:00Z"));
    }

    @Test
    void hoyEsLaFechaDeBogota() {
        assertThat(FechasDominio.hoy(Instant.parse("2026-09-11T04:59:59Z"))).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(FechasDominio.hoy(Instant.parse("2026-09-11T05:00:00Z"))).isEqualTo(LocalDate.of(2026, 9, 11));
    }
}
