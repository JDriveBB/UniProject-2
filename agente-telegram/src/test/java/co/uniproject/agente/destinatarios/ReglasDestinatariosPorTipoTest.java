package co.uniproject.agente.destinatarios;

import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.datos.*;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasDestinatariosPorTipoTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-24T19:05:00Z"), ZoneId.of("America/Bogota"));
    private final ReglasDestinatariosPorTipo reglas = new ReglasDestinatariosPorTipo();

    private EventoCloud ev(DatosEvento datos) {
        return EventoCloud.nuevo(datos, reloj);
    }

    @Test
    void riesgoIntegrantesConTelegramYDocente() {
        List<Destinatario> resultado = reglas.destinatarios(ev(EjemplosEventos.tareaRutaCriticaRiesgo()));
        assertThat(resultado).containsExactly(
                new Destinatario(100L, 900000100L, Variante.UNICA),
                new Destinatario(101L, 900000101L, Variante.UNICA),
                new Destinatario(200L, 900000200L, Variante.UNICA)
        );
    }

    @Test
    void inactividadSoloLider() {
        List<Destinatario> resultado = reglas.destinatarios(ev(EjemplosEventos.equipoInactividad(false)));
        assertThat(resultado).containsExactly(
                new Destinatario(100L, 900000100L, Variante.UNICA)
        );
    }

    @Test
    void inactividadConDocenteSiLoPidio() {
        List<Destinatario> resultado = reglas.destinatarios(ev(EjemplosEventos.equipoInactividad(true)));
        assertThat(resultado).containsExactly(
                new Destinatario(100L, 900000100L, Variante.UNICA),
                new Destinatario(200L, 900000200L, Variante.UNICA)
        );
    }

    @Test
    void hitoEnviadoAlDocente() {
        List<Destinatario> resultado = reglas.destinatarios(ev(EjemplosEventos.hitoEnviado()));
        assertThat(resultado).containsExactly(
                new Destinatario(200L, 900000200L, Variante.UNICA)
        );
    }

    @Test
    void hitoEvaluadoIntegrantesConTelegram() {
        List<Destinatario> resultado = reglas.destinatarios(ev(EjemplosEventos.hitoEvaluado(CaracterNota.DEFINITIVA)));
        assertThat(resultado).containsExactly(
                new Destinatario(100L, 900000100L, Variante.UNICA),
                new Destinatario(101L, 900000101L, Variante.UNICA)
        );
    }

    @Test
    void reasignadaNuevoYAnterior() {
        List<Destinatario> resultado = reglas.destinatarios(ev(EjemplosEventos.tareaReasignada()));
        assertThat(resultado).containsExactly(
                new Destinatario(103L, 900000103L, Variante.NUEVO),
                new Destinatario(101L, 900000101L, Variante.ANTERIOR)
        );
    }

    @Test
    void correccionAlResponsable() {
        List<Destinatario> resultado = reglas.destinatarios(ev(EjemplosEventos.tareaCorreccion()));
        assertThat(resultado).containsExactly(
                new Destinatario(101L, 900000101L, Variante.UNICA)
        );
    }

    @Test
    void sinRepetidosYSinChatNulo() {
        var datos = new TareaRutaCriticaRiesgo(
                EjemplosEventos.PROYECTO,
                EjemplosEventos.EQUIPO,
                EjemplosEventos.TAREA,
                new Referencia(101, "Estudiante A"),
                40,
                EjemplosEventos.HITO,
                LocalDate.of(2026, 11, 5),
                List.of(EjemplosEventos.LIDER_C, EjemplosEventos.INTEGRANTE_B_SIN_TELEGRAM),
                new Persona(100, "Estudiante C", 900000100L)
        );
        List<Destinatario> resultado = reglas.destinatarios(ev(datos));
        assertThat(resultado).containsExactly(
                new Destinatario(100L, 900000100L, Variante.UNICA)
        );
    }
}
