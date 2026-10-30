package co.uniproject.nucleo.evaluacion.calificacion;

import co.uniproject.eventos.datos.CaracterNota;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaracterEvaluacionTest {

    private static final LocalDate LIMITE = LocalDate.of(2026, 11, 20);
    /** 2026-11-21 00:00 en Bogotá = 05:00 UTC: el instante en que vence. */
    private static final Instant VENCE = Instant.parse("2026-11-21T05:00:00Z");

    @Test
    void antesDelVencimientoEsProvisional() {
        assertThat(CaracterEvaluacion.de(false, LIMITE, VENCE.minusSeconds(1))).isEqualTo(CaracterNota.PROVISIONAL);
    }

    @Test
    void alVencerPasaADefinitivaSinEscribirNada() {
        assertThat(CaracterEvaluacion.de(false, LIMITE, VENCE)).isEqualTo(CaracterNota.DEFINITIVA);
    }

    @Test
    void registradaDespuesDelVencimientoSiempreEsDefinitiva() {
        assertThat(CaracterEvaluacion.de(true, LIMITE, VENCE.minusSeconds(3600))).isEqualTo(CaracterNota.DEFINITIVA);
    }
}
