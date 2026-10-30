package co.uniproject.eventos;

import co.uniproject.eventos.DestinatariosEvento.Destino;
import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.eventos.datos.TareaRutaCriticaRiesgo;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DestinatariosEventoTest {

    private static List<String> resumen(List<Destino> destinos) {
        return destinos.stream().map(d -> d.persona().id() + ":" + d.papel()).toList();
    }

    @Test
    void riesgoIncluyeAQuienNoTieneTelegram() {
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.tareaRutaCriticaRiesgo())))
                .containsExactly("100:UNICO", "101:UNICO", "102:UNICO", "200:UNICO");
    }

    @Test
    void inactividadAlLiderYAlDocenteSiLoPidio() {
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.equipoInactividad(false))))
                .containsExactly("100:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.equipoInactividad(true))))
                .containsExactly("100:UNICO", "200:UNICO");
    }

    @Test
    void losDemasTipos() {
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.hitoEnviado()))).containsExactly("200:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.hitoEvaluado(CaracterNota.DEFINITIVA))))
                .containsExactly("100:UNICO", "101:UNICO", "102:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.tareaReasignada())))
                .containsExactly("103:NUEVO", "101:ANTERIOR");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.tareaCorreccion()))).containsExactly("101:UNICO");
    }

    @Test
    void sinRepetidosGanaLaPrimeraAparicion() {
        var datos = new TareaRutaCriticaRiesgo(EjemplosEventos.PROYECTO, EjemplosEventos.EQUIPO, EjemplosEventos.TAREA,
                new Referencia(101, "Estudiante A"), 40, EjemplosEventos.HITO, LocalDate.of(2026, 11, 5),
                List.of(EjemplosEventos.LIDER_C, EjemplosEventos.INTEGRANTE_B_SIN_TELEGRAM),
                new Persona(100, "Estudiante C", 900000100L));
        assertThat(resumen(DestinatariosEvento.de(datos))).containsExactly("100:UNICO", "102:UNICO");
    }
}
