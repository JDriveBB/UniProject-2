package co.uniproject.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.eventos.DestinatariosEvento.Destino;
import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.datos.MotivoSinLider;
import co.uniproject.eventos.datos.MovimientoEquipo;
import co.uniproject.eventos.datos.OrigenCambioContrasena;
import co.uniproject.eventos.datos.OrigenLider;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.eventos.datos.TareaRutaCriticaRiesgo;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

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
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.hitoEnviado())))
                .containsExactly("200:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.hitoEvaluado(CaracterNota.DEFINITIVA))))
                .containsExactly("100:UNICO", "101:UNICO", "102:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.tareaReasignada())))
                .containsExactly("103:NUEVO", "101:ANTERIOR");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.tareaCorreccion())))
                .containsExactly("101:UNICO");
    }

    @Test
    void sinRepetidosGanaLaPrimeraAparicion() {
        var datos = new TareaRutaCriticaRiesgo(
                EjemplosEventos.PROYECTO,
                EjemplosEventos.EQUIPO,
                EjemplosEventos.TAREA,
                new Referencia(101, "Estudiante A"),
                40,
                EjemplosEventos.HITO,
                LocalDate.of(2026, 11, 5),
                List.of(EjemplosEventos.LIDER_C, EjemplosEventos.INTEGRANTE_B_SIN_TELEGRAM),
                new Persona(100, "Estudiante C", 900000100L));
        assertThat(resumen(DestinatariosEvento.de(datos))).containsExactly("100:UNICO", "102:UNICO");
    }

    @Test
    void eventosDeCuentaSoloAlTitular() {
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.recuperacionSolicitada())))
                .containsExactly("101:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.contrasenaCambiada(OrigenCambioContrasena.PERFIL))))
                .containsExactly("101:UNICO");
    }

    @Test
    void cursosYEquipos() {
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.cursoEstudianteAgregado())))
                .containsExactly("101:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.equipoVotacionAbierta())))
                .containsExactly("100:UNICO", "101:UNICO", "102:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.equipoLiderElegido(OrigenLider.VOTACION))))
                .containsExactly("100:UNICO", "101:UNICO", "102:UNICO", "200:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.equipoSinLider(MotivoSinLider.EMPATE))))
                .containsExactly("200:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.ENTRO, true))))
                .containsExactly("103:NUEVO", "100:UNICO", "101:UNICO", "102:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.ENTRO, false))))
                .containsExactly("100:UNICO", "101:UNICO", "102:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.SALIO, true))))
                .containsExactly("100:UNICO", "101:UNICO", "102:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.inscripcionPorCerrar())))
                .containsExactly("101:UNICO", "102:UNICO");
        assertThat(resumen(DestinatariosEvento.de(EjemplosEventos.inscripcionResumen())))
                .containsExactly("200:UNICO");
    }
}
