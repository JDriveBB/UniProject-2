package co.uniproject.nucleo.notificaciones.bandeja;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.eventos.DestinatariosEvento.Papel;
import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.datos.EquipoMiembroCambio;
import co.uniproject.eventos.datos.MotivoSinLider;
import co.uniproject.eventos.datos.MovimientoEquipo;
import co.uniproject.eventos.datos.OrigenLider;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import java.util.List;
import org.junit.jupiter.api.Test;

class MensajesBandejaTest {

    @Test
    void unMensajePorTipo() {
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.tareaRutaCriticaRiesgo(), Papel.UNICO))
                .isEqualTo(
                        "La tarea \"Diagramas de secuencia\" de la ruta crítica pone en riesgo el hito \"Entrega de diagramas\" (avance 40 %).");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.equipoInactividad(true), Papel.UNICO))
                .isEqualTo("Estudiante B lleva 3 días sin registrar actividad en Equipo 4.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.hitoEnviado(), Papel.UNICO))
                .isEqualTo("Equipo 4 envió el hito \"Entrega de diagramas\" para evaluación.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.hitoEvaluado(CaracterNota.PROVISIONAL), Papel.UNICO))
                .isEqualTo("El hito \"Entrega de diagramas\" de Equipo 4 tiene nota provisional.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.hitoEvaluado(CaracterNota.DEFINITIVA), Papel.UNICO))
                .isEqualTo("El hito \"Entrega de diagramas\" de Equipo 4 tiene nota definitiva.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.tareaReasignada(), Papel.NUEVO))
                .isEqualTo("Se te asignó la tarea \"Diagramas de secuencia\" en Equipo 4.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.tareaReasignada(), Papel.ANTERIOR))
                .isEqualTo("La tarea \"Diagramas de secuencia\" se reasignó a Estudiante E.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.tareaCorreccion(), Papel.UNICO))
                .isEqualTo(
                        "Estudiante C pidió corregir la tarea \"Diagramas de secuencia\": Falta el caso \"sin conexión\" & revisar <notas>");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.cursoEstudianteAgregado(), Papel.UNICO))
                .isEqualTo("Docente D te agregó al curso \"Diseño de Software I\" (2026-2).");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.equipoVotacionAbierta(), Papel.UNICO))
                .isEqualTo("Se abrió la votación de líder de Equipo 4. Ya puedes votar.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.equipoLiderElegido(OrigenLider.VOTACION), Papel.UNICO))
                .isEqualTo("Estudiante C es el nuevo líder de Equipo 4, elegido por votación.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.equipoLiderElegido(OrigenLider.DESEMPATE), Papel.UNICO))
                .isEqualTo("Estudiante C es el nuevo líder de Equipo 4, elegido por el docente tras un empate.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.equipoSinLider(MotivoSinLider.EMPATE), Papel.UNICO))
                .isEqualTo("La votación de líder de Equipo 4 terminó en empate. Elige al líder entre los empatados.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.equipoSinLider(MotivoSinLider.SIN_VOTOS), Papel.UNICO))
                .isEqualTo("La votación de líder de Equipo 4 terminó sin votos. Nombra un líder o abre otra votación.");
        assertThat(MensajesBandeja.mensaje(
                        EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.ENTRO, true), Papel.NUEVO))
                .isEqualTo("El docente te agregó a Equipo 4 en \"Proyecto final\".");
        assertThat(MensajesBandeja.mensaje(
                        EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.ENTRO, true), Papel.UNICO))
                .isEqualTo("Estudiante E entró a Equipo 4.");
        var dosPersonas = new EquipoMiembroCambio(
                EjemplosEventos.PROYECTO,
                EjemplosEventos.EQUIPO,
                MovimientoEquipo.ENTRO,
                true,
                List.of(EjemplosEventos.INTEGRANTE_E, EjemplosEventos.INTEGRANTE_A),
                List.of(EjemplosEventos.LIDER_C));
        assertThat(MensajesBandeja.mensaje(dosPersonas, Papel.UNICO))
                .isEqualTo("Estudiante E y Estudiante A entraron a Equipo 4.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.inscripcionPorCerrar(), Papel.UNICO))
                .isEqualTo(
                        "La inscripción a equipos de \"Proyecto final\" cierra en menos de 24 horas y aún no tienes equipo.");
        assertThat(MensajesBandeja.mensaje(EjemplosEventos.inscripcionResumen(), Papel.UNICO))
                .isEqualTo(
                        "La inscripción a equipos de \"Proyecto final\" cierra en menos de 24 horas: 2 estudiantes sin equipo y 3 cupos libres.");
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
        assertThat(MensajesBandeja.referencia(EjemplosEventos.cursoEstudianteAgregado()))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("curso", 30));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.equipoVotacionAbierta()))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("equipo", 4));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.equipoLiderElegido(OrigenLider.VOTACION)))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("equipo", 4));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.equipoSinLider(MotivoSinLider.EMPATE)))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("equipo", 4));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.ENTRO, true)))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("equipo", 4));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.inscripcionPorCerrar()))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("proyecto", 12));
        assertThat(MensajesBandeja.referencia(EjemplosEventos.inscripcionResumen()))
                .isEqualTo(new MensajesBandeja.ReferenciaBandeja("proyecto", 12));
    }

    @Test
    void recortaA500Caracteres() {
        assertThat(MensajesBandeja.recortar("x".repeat(600))).hasSize(500).endsWith("...");
        assertThat(MensajesBandeja.recortar("corto")).isEqualTo("corto");
    }
}
