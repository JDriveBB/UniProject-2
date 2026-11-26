package co.uniproject.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.uniproject.eventos.datos.MovimientoEquipo;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextosEventosTest {

    @Test
    void listaNombresUneEnEspanolYRechazaVacia() {
        assertThat(TextosEventos.listaNombres(List.of("Ana"))).isEqualTo("Ana");
        assertThat(TextosEventos.listaNombres(List.of("Ana", "Bruno"))).isEqualTo("Ana y Bruno");
        assertThat(TextosEventos.listaNombres(List.of("Ana", "Bruno", "Carla"))).isEqualTo("Ana, Bruno y Carla");
        assertThatThrownBy(() -> TextosEventos.listaNombres(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cantidadAplicaSingularYPlural() {
        assertThat(TextosEventos.cantidad(1, "estudiante", "estudiantes")).isEqualTo("1 estudiante");
        assertThat(TextosEventos.cantidad(0, "estudiante", "estudiantes")).isEqualTo("0 estudiantes");
        assertThat(TextosEventos.cantidad(3, "cupo libre", "cupos libres")).isEqualTo("3 cupos libres");
    }

    @Test
    void verboConjugaEntradaYSalida() {
        assertThat(TextosEventos.verbo(MovimientoEquipo.ENTRO, 1)).isEqualTo("entró a");
        assertThat(TextosEventos.verbo(MovimientoEquipo.ENTRO, 2)).isEqualTo("entraron a");
        assertThat(TextosEventos.verbo(MovimientoEquipo.SALIO, 1)).isEqualTo("salió de");
        assertThat(TextosEventos.verbo(MovimientoEquipo.SALIO, 3)).isEqualTo("salieron de");
    }
}
