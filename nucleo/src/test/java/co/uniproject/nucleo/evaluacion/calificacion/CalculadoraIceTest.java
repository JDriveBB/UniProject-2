package co.uniproject.nucleo.evaluacion.calificacion;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraIceTest {

    private static BigDecimal d(String valor) { return new BigDecimal(valor); }

    private static TareaCompletada aTiempo(long id, String horas) { return new TareaCompletada(id, d(horas), true); }

    private static TareaCompletada tarde(long id, String horas) { return new TareaCompletada(id, d(horas), false); }

    private static CriterioCalificado criterio(String nota, String maximo, String peso) {
        return new CriterioCalificado(d(nota), d(maximo), d(peso));
    }

    @Test
    void ejemploDelEquipoDeTres() {
        // 11: 4 h a tiempo + 2 h tarde = 5; 12: 3 h a tiempo = 3; 13: nada. Σ = 8, n = 3.
        List<NotaSugerida> r = CalculadoraIce.calcular(d("4.00"), List.of(13L, 11L, 12L),
                List.of(aTiempo(11, "4"), tarde(11, "2"), aTiempo(12, "3")));
        assertThat(r).extracting(NotaSugerida::idEstudiante).containsExactly(11L, 12L, 13L);
        assertThat(r.get(0).aporte()).isEqualByComparingTo("5");
        assertThat(r.get(0).ice()).isEqualTo(d("1.8750"));
        assertThat(r.get(0).notaSugerida()).isEqualTo(d("5.00"));
        assertThat(r.get(1).aporte()).isEqualByComparingTo("3");
        assertThat(r.get(1).ice()).isEqualTo(d("1.1250"));
        assertThat(r.get(1).notaSugerida()).isEqualTo(d("4.25"));
        assertThat(r.get(2).aporte()).isEqualByComparingTo("0");
        assertThat(r.get(2).ice()).isEqualTo(d("0.0000"));
        assertThat(r.get(2).notaSugerida()).isEqualTo(d("2.00"));
    }

    @Test
    void laSumaDeIceEsElTamanoDelEquipo() {
        List<NotaSugerida> r = CalculadoraIce.calcular(d("4.00"), List.of(11L, 12L, 13L),
                List.of(aTiempo(11, "4"), tarde(11, "2"), aTiempo(12, "3")));
        BigDecimal suma = r.stream().map(NotaSugerida::ice).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo("3");
    }

    @Test
    void aportesIgualesRecibenLaNotaGrupal() {
        List<NotaSugerida> r = CalculadoraIce.calcular(d("3.70"), List.of(11L, 12L),
                List.of(aTiempo(11, "2"), aTiempo(12, "2")));
        assertThat(r).extracting(NotaSugerida::ice).containsExactly(d("1.0000"), d("1.0000"));
        assertThat(r).extracting(NotaSugerida::notaSugerida).containsExactly(d("3.70"), d("3.70"));
    }

    @Test
    void sinAportesTodosRecibenLaNotaGrupal() {
        List<NotaSugerida> sinTareas = CalculadoraIce.calcular(d("4.2"), List.of(11L, 12L), List.of());
        assertThat(sinTareas).extracting(NotaSugerida::ice).containsExactly(d("1.0000"), d("1.0000"));
        assertThat(sinTareas).extracting(NotaSugerida::notaSugerida).containsExactly(d("4.20"), d("4.20"));
        List<NotaSugerida> ceroHoras = CalculadoraIce.calcular(d("4.2"), List.of(11L), List.of(aTiempo(11, "0")));
        assertThat(ceroHoras.get(0).notaSugerida()).isEqualTo(d("4.20"));
    }

    @Test
    void redondeoHalfUpConIceRedondeado() {
        // Σ = 3, n = 2: ICE 0,6667 y 1,3333. 3,33 · 0,83335 = 2,7750555 → 2,78; 3,33 · 1,16665 = 3,8849445 → 3,88.
        List<NotaSugerida> r = CalculadoraIce.calcular(d("3.33"), List.of(11L, 12L),
                List.of(aTiempo(11, "1"), aTiempo(12, "2")));
        assertThat(r).extracting(NotaSugerida::ice).containsExactly(d("0.6667"), d("1.3333"));
        assertThat(r).extracting(NotaSugerida::notaSugerida).containsExactly(d("2.78"), d("3.88"));
    }

    @Test
    void lasTardiasValenLaMitadYLaNotaNoPasaDeCinco() {
        // 11: 3 h tarde = 1,5; 12: 3 h a tiempo = 3. Σ = 4,5. 5 · 0,83335 = 4,16675 → 4,17; 5 · 1,16665 = 5,83 → 5,00.
        List<NotaSugerida> r = CalculadoraIce.calcular(d("5.00"), List.of(11L, 12L),
                List.of(tarde(11, "3"), aTiempo(12, "3")));
        assertThat(r.get(0).aporte()).isEqualByComparingTo("1.5");
        assertThat(r).extracting(NotaSugerida::ice).containsExactly(d("0.6667"), d("1.3333"));
        assertThat(r).extracting(NotaSugerida::notaSugerida).containsExactly(d("4.17"), d("5.00"));
    }

    @Test
    void validacionesDelCalculo() {
        assertThatThrownBy(() -> CalculadoraIce.calcular(d("5.01"), List.of(11L), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalculadoraIce.calcular(d("-0.01"), List.of(11L), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalculadoraIce.calcular(d("4.00"), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalculadoraIce.calcular(d("4.00"), List.of(11L), List.of(aTiempo(99, "1"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> aTiempo(11, "-1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notaGrupalDesdeLaRubrica() {
        // 4/5·50 + 8/10·30 + 15/20·20 = 79 → 3,95.
        assertThat(CalculadoraIce.notaGrupal(List.of(criterio("4", "5", "50"), criterio("8", "10", "30"),
                criterio("15", "20", "20")))).isEqualTo(d("3.95"));
        // 2/3·100 = 66,666… → 3,333… → 3,33.
        assertThat(CalculadoraIce.notaGrupal(List.of(criterio("2", "3", "100")))).isEqualTo(d("3.33"));
        assertThat(CalculadoraIce.notaGrupal(List.of(criterio("5", "5", "40"), criterio("10", "10", "60"))))
                .isEqualTo(d("5.00"));
    }

    @Test
    void validacionesDeLaRubrica() {
        assertThatThrownBy(() -> CalculadoraIce.notaGrupal(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalculadoraIce.notaGrupal(List.of(criterio("4", "5", "90"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> criterio("6", "5", "50")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> criterio("-1", "5", "50")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> criterio("1", "0", "50")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> criterio("1", "5", "0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> criterio("1", "5", "100.01")).isInstanceOf(IllegalArgumentException.class);
    }
}
