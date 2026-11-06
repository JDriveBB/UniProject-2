package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.eventos.interno.ContratoInterno;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodigosRecuperacionTest {

    @Test
    void generaCodigosDe43CaracteresDistintos() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            CodigosRecuperacion.Nuevo n = CodigosRecuperacion.nuevo();
            assertThat(n.texto()).matches(ContratoInterno.PATRON_CODIGO_RECUPERACION);
            assertThat(n.selector()).hasSize(8);
            assertThat(n.hashVerificador()).hasSize(32);
            assertThat(n.toString()).doesNotContain(n.texto());
            vistos.add(n.texto());
        }
        assertThat(vistos).hasSize(200);
    }

    @Test
    void leeSoloElFormatoValido() {
        CodigosRecuperacion.Nuevo n = CodigosRecuperacion.nuevo();
        CodigosRecuperacion.Partes p = CodigosRecuperacion.leer(n.texto()).orElseThrow();
        assertThat(p.selector()).isEqualTo(n.selector());
        assertThat(p.verificador()).hasSize(24);
        assertThat(CodigosRecuperacion.leer(null)).isEmpty();
        assertThat(CodigosRecuperacion.leer(n.texto().substring(1))).isEmpty();
        assertThat(CodigosRecuperacion.leer(n.texto() + "A")).isEmpty();
        assertThat(CodigosRecuperacion.leer(n.texto().substring(1) + "+")).isEmpty();
        assertThat(CodigosRecuperacion.leer(" " + n.texto().substring(1))).isEmpty();
    }

    @Test
    void comparaElVerificadorPorSuHash() {
        CodigosRecuperacion.Nuevo n = CodigosRecuperacion.nuevo();
        CodigosRecuperacion.Partes p = CodigosRecuperacion.leer(n.texto()).orElseThrow();
        assertThat(CodigosRecuperacion.coincide(p.verificador(), n.hashVerificador())).isTrue();
        byte[] otro = p.verificador().clone();
        otro[0] ^= 1;
        assertThat(CodigosRecuperacion.coincide(otro, n.hashVerificador())).isFalse();
        assertThat(CodigosRecuperacion.coincide(p.verificador(), null)).isFalse();
        assertThat(n.hashVerificador()).isNotEqualTo(p.verificador());
    }
}
