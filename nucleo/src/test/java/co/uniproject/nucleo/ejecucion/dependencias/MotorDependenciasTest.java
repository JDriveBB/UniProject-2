package co.uniproject.nucleo.ejecucion.dependencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MotorDependenciasTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T08:00:00Z");

    private static Duration h(long horas) {
        return Duration.ofHours(horas);
    }

    private static Duration m(long minutos) {
        return Duration.ofMinutes(minutos);
    }

    private static TareaPlan t(long id, long horas, int avance, Long... prerrequisitos) {
        return new TareaPlan(id, h(horas), avance, Set.of(prerrequisitos));
    }

    private static List<TareaPlan> ejemplo(int a1, int a2, int a3, int a4, int a5) {
        return List.of(t(1, 3, a1), t(2, 2, a2, 1L), t(3, 4, a3, 1L), t(4, 1, a4, 2L, 3L), t(5, 2, a5));
    }

    private static Map<Long, Instant> limite(Instant fecha) {
        return Map.of(1L, fecha, 2L, fecha, 3L, fecha, 4L, fecha, 5L, fecha);
    }

    private static Map<Long, RiesgoTarea> porId(List<RiesgoTarea> riesgos) {
        return riesgos.stream().collect(Collectors.toMap(RiesgoTarea::tareaId, r -> r));
    }

    @Test
    void ordenTopologicoDeterminista() {
        assertThat(MotorDependencias.ordenTopologico(ejemplo(0, 0, 0, 0, 0))).containsExactly(1L, 2L, 3L, 4L, 5L);
        List<TareaPlan> invertida = new ArrayList<>(ejemplo(0, 0, 0, 0, 0));
        Collections.reverse(invertida);
        assertThat(MotorDependencias.ordenTopologico(invertida)).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void rutaCriticaDelEjemplo() {
        RutaCritica r = MotorDependencias.rutaCritica(ejemplo(0, 0, 0, 0, 0));
        assertThat(r.orden()).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(r.duracionTotal()).isEqualTo(h(8));
        assertThat(r.criticas()).containsExactly(1L, 3L, 4L);
        assertThat(r.tiempos().get(2L)).isEqualTo(new Tiempos(h(3), h(5), h(5), h(7), h(2)));
        assertThat(r.tiempos().get(3L)).isEqualTo(new Tiempos(h(3), h(7), h(3), h(7), h(0)));
        assertThat(r.tiempos().get(5L)).isEqualTo(new Tiempos(h(0), h(2), h(6), h(8), h(6)));
    }

    @Test
    void planVacio() {
        RutaCritica r = MotorDependencias.rutaCritica(List.of());
        assertThat(r.orden()).isEmpty();
        assertThat(r.criticas()).isEmpty();
        assertThat(r.duracionTotal()).isEqualTo(Duration.ZERO);
        assertThat(MotorDependencias.evaluarRiesgo(List.of(), AHORA, Map.of())).isEmpty();
    }

    @Test
    void riesgoSinRetraso() {
        List<RiesgoTarea> lista =
                MotorDependencias.evaluarRiesgo(ejemplo(100, 50, 25, 0, 0), AHORA, limite(AHORA.plus(h(10))), 24);
        assertThat(lista).extracting(RiesgoTarea::tareaId).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(lista).extracting(RiesgoTarea::nivel).containsOnly(NivelRiesgo.BAJO);
        Map<Long, RiesgoTarea> r = porId(lista);
        assertThat(r.get(3L)).isEqualTo(new RiesgoTarea(3L, NivelRiesgo.BAJO, AHORA.plus(h(3)), h(6)));
        assertThat(r.get(4L)).isEqualTo(new RiesgoTarea(4L, NivelRiesgo.BAJO, AHORA.plus(h(4)), h(6)));
    }

    @Test
    void riesgoAltoCuandoSeComprometeLaFecha() {
        Map<Long, RiesgoTarea> r = porId(
                MotorDependencias.evaluarRiesgo(ejemplo(100, 50, 25, 0, 0), AHORA, limite(AHORA.plus(m(210))), 24));
        assertThat(r.get(1L)).isEqualTo(new RiesgoTarea(1L, NivelRiesgo.BAJO, AHORA, m(-30)));
        assertThat(r.get(2L)).isEqualTo(new RiesgoTarea(2L, NivelRiesgo.BAJO, AHORA.plus(h(1)), m(90)));
        assertThat(r.get(3L)).isEqualTo(new RiesgoTarea(3L, NivelRiesgo.ALTO, AHORA.plus(h(3)), m(-30)));
        assertThat(r.get(4L)).isEqualTo(new RiesgoTarea(4L, NivelRiesgo.ALTO, AHORA.plus(h(4)), m(-30)));
        assertThat(r.get(5L)).isEqualTo(new RiesgoTarea(5L, NivelRiesgo.BAJO, AHORA.plus(h(2)), m(90)));
    }

    @Test
    void riesgoMedioConPocaHolgura() {
        Map<Long, RiesgoTarea> r = porId(
                MotorDependencias.evaluarRiesgo(ejemplo(100, 50, 25, 0, 0), AHORA, limite(AHORA.plus(m(250))), 24));
        assertThat(r.get(2L).nivel()).isEqualTo(NivelRiesgo.BAJO);
        assertThat(r.get(2L).holgura()).isEqualTo(m(130));
        assertThat(r.get(3L)).isEqualTo(new RiesgoTarea(3L, NivelRiesgo.MEDIO, AHORA.plus(h(3)), m(10)));
        assertThat(r.get(4L)).isEqualTo(new RiesgoTarea(4L, NivelRiesgo.MEDIO, AHORA.plus(h(4)), m(10)));
        assertThat(r.get(5L).nivel()).isEqualTo(NivelRiesgo.BAJO);
    }

    @Test
    void limitePropioDeLaTarea() {
        Map<Long, Instant> limites = new HashMap<>(limite(AHORA.plus(h(10))));
        limites.put(5L, AHORA.plus(m(90)));
        Map<Long, RiesgoTarea> r =
                porId(MotorDependencias.evaluarRiesgo(ejemplo(100, 50, 25, 0, 0), AHORA, limites, 24));
        assertThat(r.get(5L)).isEqualTo(new RiesgoTarea(5L, NivelRiesgo.ALTO, AHORA.plus(h(2)), m(-30)));
        assertThat(r.get(4L).nivel()).isEqualTo(NivelRiesgo.BAJO);
    }

    @Test
    void riesgoConHorasDeTrabajoPorDia() {
        List<TareaPlan> cadena = List.of(t(1, 6, 0), t(2, 5, 0, 1L), t(3, 8, 0, 2L));

        Instant limiteTresDias = AHORA.plus(h(72));
        Map<Long, RiesgoTarea> alto = porId(MotorDependencias.evaluarRiesgo(
                cadena, AHORA, Map.of(1L, limiteTresDias, 2L, limiteTresDias, 3L, limiteTresDias)));
        assertThat(alto.get(3L)).isEqualTo(new RiesgoTarea(3L, NivelRiesgo.ALTO, AHORA.plus(h(114)), h(-7)));

        Instant limiteSeisDias = AHORA.plus(h(144));
        Map<Long, RiesgoTarea> bajo = porId(MotorDependencias.evaluarRiesgo(
                cadena, AHORA, Map.of(1L, limiteSeisDias, 2L, limiteSeisDias, 3L, limiteSeisDias)));
        assertThat(bajo.values()).extracting(RiesgoTarea::nivel).containsOnly(NivelRiesgo.BAJO);
        assertThat(bajo.get(3L)).isEqualTo(new RiesgoTarea(3L, NivelRiesgo.BAJO, AHORA.plus(h(114)), h(5)));
    }

    @Test
    void bloqueadasPorPrerrequisitoIncompleto() {
        assertThat(MotorDependencias.bloqueadas(ejemplo(100, 50, 25, 0, 0))).containsExactly(4L);
        assertThat(MotorDependencias.bloqueadas(ejemplo(0, 0, 0, 0, 0))).containsExactly(2L, 3L, 4L);
        assertThat(MotorDependencias.bloqueadas(ejemplo(100, 100, 100, 0, 0))).isEmpty();
    }

    @Test
    void agregarPrerrequisitosExcluyeLosCiclicos() {
        ResultadoDependencias r1 =
                MotorDependencias.agregarPrerrequisitos(ejemplo(0, 0, 0, 0, 0), 1L, Set.of(4L, 5L, 1L));
        assertThat(r1.aceptados()).containsExactly(5L);
        assertThat(r1.ciclicos()).containsExactly(1L, 4L);

        ResultadoDependencias r2 = MotorDependencias.agregarPrerrequisitos(ejemplo(0, 0, 0, 0, 0), 4L, Set.of(2L, 5L));
        assertThat(r2.aceptados()).containsExactly(2L, 5L);
        assertThat(r2.ciclicos()).isEmpty();

        assertThatThrownBy(() -> MotorDependencias.agregarPrerrequisitos(ejemplo(0, 0, 0, 0, 0), 4L, Set.of(99L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cicloGuardadoLanza() {
        List<TareaPlan> ciclo = List.of(t(10, 1, 0, 11L), t(11, 1, 0, 10L));
        assertThatThrownBy(() -> MotorDependencias.ordenTopologico(ciclo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Las dependencias forman un ciclo");
        assertThatThrownBy(() -> MotorDependencias.rutaCritica(ciclo)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validaciones() {
        assertThatThrownBy(() -> new TareaPlan(1, h(-1), 0, Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TareaPlan(1, h(1), 101, Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TareaPlan(1, h(1), -1, Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TareaPlan(1, h(1), 0, Set.of(1L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MotorDependencias.ordenTopologico(List.of(t(1, 1, 0), t(1, 2, 0))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MotorDependencias.ordenTopologico(List.of(t(1, 1, 0, 99L))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MotorDependencias.evaluarRiesgo(ejemplo(0, 0, 0, 0, 0), AHORA, Map.of(1L, AHORA), 24))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MotorDependencias.evaluarRiesgo(ejemplo(0, 0, 0, 0, 0), AHORA, limite(AHORA), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MotorDependencias.evaluarRiesgo(ejemplo(0, 0, 0, 0, 0), AHORA, limite(AHORA), 25))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cientoCincuentaTareasEnMenosDeDosSegundos() {
        List<TareaPlan> cadena = new ArrayList<>();
        Map<Long, Instant> limites = new HashMap<>();
        for (long i = 1; i <= 150; i++) {
            cadena.add(i == 1 ? t(i, 1, 0) : t(i, 1, 0, i - 1));
            limites.put(i, AHORA.plus(h(200)));
        }
        assertTimeout(Duration.ofSeconds(2), () -> {
            RutaCritica r = MotorDependencias.rutaCritica(cadena);
            assertThat(r.duracionTotal()).isEqualTo(h(150));
            assertThat(r.criticas()).hasSize(150);
            assertThat(MotorDependencias.evaluarRiesgo(cadena, AHORA, limites, 24))
                    .extracting(RiesgoTarea::nivel)
                    .containsOnly(NivelRiesgo.BAJO);
        });
    }
}
