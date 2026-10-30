package co.uniproject.nucleo.notificaciones.bandeja;

import co.uniproject.eventos.DestinatariosEvento.Papel;
import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MensajesBandejaTest {

    @Test
    void unMensajePorTipo() {
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.tareaRutaCriticaRiesgo(), Papel.UNICO)).isEqualTo(
                "La tarea \"Diagramas de secuencia\" de la ruta crítica pone en riesgo el hito \"Entrega de diagramas\" (avance 40 %).");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.equipoInactividad(true), Papel.UNICO)).isEqualTo(
                "Estudiante B lleva 3 días sin registrar actividad en Equipo 4.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.hitoEnviado(), Papel.UNICO)).isEqualTo(
                "Equipo 4 envió el hito \"Entrega de diagramas\" para evaluación.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.hitoEvaluado(CaracterNota.PROVISIONAL), Papel.UNICO))
                .isEqualTo("El hito \"Entrega de diagramas\" de Equipo 4 tiene nota provisional.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.hitoEvaluado(CaracterNota.DEFINITIVA), Papel.UNICO))
                .isEqualTo("El hito \"Entrega de diagramas\" de Equipo 4 tiene nota definitiva.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.tareaReasignada(), Papel.NUEVO)).isEqualTo(
                "Se te asignó la tarea \"Diagramas de secuencia\" en Equipo 4.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.tareaReasignada(), Papel.ANTERIOR)).isEqualTo(
                "La tarea \"Diagramas de secuencia\" se reasignó a Estudiante E.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.tareaCorreccion(), Papel.UNICO)).isEqualTo(
                "Estudiante C pidió corregir la tarea \"Diagramas de secuencia\": Falta el caso \"sin conexión\" & revisar <notas>");
    }

    @Test
    void cadaAvisoApuntaASuEntidad() {
        assertThat(MensajesBandeja.referencia(EjemplosEventos.tareaRutaCriticaRiesgo()))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("tarea", 45));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.equipoInactividad(false)))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("equipo", 4));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.hitoEnviado()))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("hito_equipo", 7));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.hitoEvaluado(CaracterNota.PROVISIONAL)))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("hito_equipo", 7));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.tareaReasignada()))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("tarea", 45));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.tareaCorreccion()))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("tarea", 45));
    }

    @Test
    void recortaA500Caracteres() {
        assertThat(MensajesBandeja.recortar("x".repeat(600))).hasSize(500).endsWith("...");
        assertThat(MensajesBandeja.recortar("corto")).isEqualTo("corto");
    }
}
