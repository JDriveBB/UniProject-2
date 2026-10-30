package co.uniproject.agente.plantillas;

import co.uniproject.agente.destinatarios.Variante;
import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.datos.DatosEvento;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderizadorMustacheTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-24T19:05:00Z"), ZoneId.of("America/Bogota"));
    private final RenderizadorMustache renderizador = new RenderizadorMustache(new CodecEventos());

    private EventoCloud ev(DatosEvento datos) {
        return EventoCloud.nuevo(datos, reloj);
    }

    @Test
    void riesgo() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.tareaRutaCriticaRiesgo()), Variante.UNICA);
        assertThat(aviso.html()).isEqualTo(
                "<b>Tarea en riesgo</b>\n" +
                "La tarea «Diagramas de secuencia», a cargo de Estudiante A, va en 40 % y pone en riesgo el hito «Entrega de diagramas», que vence el 05/11/2026."
        );
        assertThat(aviso.textoBoton()).isEqualTo("Ver tarea");
        assertThat(aviso.ruta()).isEqualTo("/tareas/45");
    }

    @Test
    void inactividad() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.equipoInactividad(false)), Variante.UNICA);
        assertThat(aviso.html()).isEqualTo(
                "<b>Alerta de inactividad</b>\n" +
                "Estudiante B no registra actividad en sus tareas desde el 20/10/2026. El periodo configurado para el proyecto «Proyecto final» es de 3 días."
        );
        assertThat(aviso.textoBoton()).isEqualTo("Ver tablero del equipo");
        assertThat(aviso.ruta()).isEqualTo("/equipos/4/tablero");
    }

    @Test
    void hitoEnviado() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.hitoEnviado()), Variante.UNICA);
        assertThat(aviso.html()).isEqualTo(
                "<b>Hito enviado a revisión</b>\n" +
                "El Equipo 4 envió el hito «Entrega de diagramas» del proyecto «Proyecto final»."
        );
        assertThat(aviso.textoBoton()).isEqualTo("Evaluar hito");
        assertThat(aviso.ruta()).isEqualTo("/hitos/7/evaluar");
    }

    @Test
    void hitoEvaluadoProvisional() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.hitoEvaluado(CaracterNota.PROVISIONAL)), Variante.UNICA);
        assertThat(aviso.html()).isEqualTo(
                "<b>Hito evaluado</b>\n" +
                "El docente calificó el hito «Entrega de diagramas». La nota es provisional.\n" +
                "Mientras la nota sea provisional, el equipo aún puede editar sus tareas hasta la fecha límite del hito."
        );
        assertThat(aviso.textoBoton()).isEqualTo("Ver evaluación");
        assertThat(aviso.ruta()).isEqualTo("/hitos/7/evaluacion");
    }

    @Test
    void hitoEvaluadoDefinitiva() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.hitoEvaluado(CaracterNota.DEFINITIVA)), Variante.UNICA);
        assertThat(aviso.html()).isEqualTo(
                "<b>Hito evaluado</b>\n" +
                "El docente calificó el hito «Entrega de diagramas». La nota es definitiva."
        );
        assertThat(aviso.textoBoton()).isEqualTo("Ver evaluación");
        assertThat(aviso.ruta()).isEqualTo("/hitos/7/evaluacion");
    }

    @Test
    void reasignadaNuevo() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.tareaReasignada()), Variante.NUEVO);
        assertThat(aviso.html()).isEqualTo(
                "<b>Nueva tarea asignada</b>\n" +
                "Estudiante C te asignó la tarea «Diagramas de secuencia», que antes estaba a cargo de Estudiante A."
        );
        assertThat(aviso.textoBoton()).isEqualTo("Ver tarea");
        assertThat(aviso.ruta()).isEqualTo("/tareas/45");
    }

    @Test
    void reasignadaAnterior() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.tareaReasignada()), Variante.ANTERIOR);
        assertThat(aviso.html()).isEqualTo(
                "<b>Tarea reasignada</b>\n" +
                "Estudiante C reasignó la tarea «Diagramas de secuencia» a Estudiante E."
        );
        assertThat(aviso.textoBoton()).isEqualTo("Ver mis tareas");
        assertThat(aviso.ruta()).isEqualTo("/mis-tareas");
    }

    @Test
    void correccionEscapaHtml() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.tareaCorreccion()), Variante.UNICA);
        assertThat(aviso.html()).isEqualTo(
                "<b>Corrección solicitada</b>\n" +
                "Estudiante C te pidió rehacer la entrega de «Diagramas de secuencia». Puntos a corregir: Falta el caso &quot;sin conexión&quot; &amp; revisar &lt;notas&gt;."
        );
        assertThat(aviso.textoBoton()).isEqualTo("Ver tarea");
        assertThat(aviso.ruta()).isEqualTo("/tareas/45");
    }

    @Test
    void varianteSinPlantillaLanza() {
        assertThatThrownBy(() -> renderizador.renderizar(ev(EjemplosEventos.tareaReasignada()), Variante.UNICA))
                .isInstanceOf(PlantillaException.class);
        assertThatThrownBy(() -> renderizador.renderizar(ev(EjemplosEventos.hitoEnviado()), Variante.NUEVO))
                .isInstanceOf(PlantillaException.class);
    }
}
