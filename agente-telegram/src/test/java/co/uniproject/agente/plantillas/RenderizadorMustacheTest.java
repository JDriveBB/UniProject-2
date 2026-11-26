package co.uniproject.agente.plantillas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.uniproject.agente.destinatarios.Variante;
import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.datos.DatosEvento;
import co.uniproject.eventos.datos.EquipoMiembroCambio;
import co.uniproject.eventos.datos.MotivoSinLider;
import co.uniproject.eventos.datos.MovimientoEquipo;
import co.uniproject.eventos.datos.OrigenCambioContrasena;
import co.uniproject.eventos.datos.OrigenLider;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class RenderizadorMustacheTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-24T19:05:00Z"), ZoneId.of("America/Bogota"));
    private final RenderizadorMustache renderizador = new RenderizadorMustache(new CodecEventos());

    private EventoCloud ev(DatosEvento datos) {
        return EventoCloud.nuevo(datos, reloj);
    }

    @Test
    void riesgo() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.tareaRutaCriticaRiesgo()), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Tarea en riesgo</b>\n"
                                + "La tarea «Diagramas de secuencia», a cargo de Estudiante A, va en 40 % y pone en riesgo el hito «Entrega de diagramas», que vence el 05/11/2026.");
        assertThat(aviso.textoBoton()).isEqualTo("Ver tarea");
        assertThat(aviso.ruta()).isEqualTo("/tareas/45");
    }

    @Test
    void inactividad() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.equipoInactividad(false)), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Alerta de inactividad</b>\n"
                                + "Estudiante B no registra actividad en sus tareas desde el 20/10/2026. El periodo configurado para el proyecto «Proyecto final» es de 3 días.");
        assertThat(aviso.textoBoton()).isEqualTo("Ver tablero del equipo");
        assertThat(aviso.ruta()).isEqualTo("/equipos/4/tablero");
    }

    @Test
    void hitoEnviado() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.hitoEnviado()), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo("<b>Hito enviado a revisión</b>\n"
                        + "El Equipo 4 envió el hito «Entrega de diagramas» del proyecto «Proyecto final».");
        assertThat(aviso.textoBoton()).isEqualTo("Evaluar hito");
        assertThat(aviso.ruta()).isEqualTo("/hitos/7/evaluar");
    }

    @Test
    void hitoEvaluadoProvisional() {
        Aviso aviso =
                renderizador.renderizar(ev(EjemplosEventos.hitoEvaluado(CaracterNota.PROVISIONAL)), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Hito evaluado</b>\n"
                                + "El docente calificó el hito «Entrega de diagramas». La nota es provisional.\n"
                                + "Mientras la nota sea provisional, el equipo aún puede editar sus tareas hasta la fecha límite del hito.");
        assertThat(aviso.textoBoton()).isEqualTo("Ver evaluación");
        assertThat(aviso.ruta()).isEqualTo("/hitos/7/evaluacion");
    }

    @Test
    void hitoEvaluadoDefinitiva() {
        Aviso aviso =
                renderizador.renderizar(ev(EjemplosEventos.hitoEvaluado(CaracterNota.DEFINITIVA)), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo("<b>Hito evaluado</b>\n"
                        + "El docente calificó el hito «Entrega de diagramas». La nota es definitiva.");
        assertThat(aviso.textoBoton()).isEqualTo("Ver evaluación");
        assertThat(aviso.ruta()).isEqualTo("/hitos/7/evaluacion");
    }

    @Test
    void reasignadaNuevo() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.tareaReasignada()), Variante.NUEVO);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Nueva tarea asignada</b>\n"
                                + "Estudiante C te asignó la tarea «Diagramas de secuencia», que antes estaba a cargo de Estudiante A.");
        assertThat(aviso.textoBoton()).isEqualTo("Ver tarea");
        assertThat(aviso.ruta()).isEqualTo("/tareas/45");
    }

    @Test
    void reasignadaAnterior() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.tareaReasignada()), Variante.ANTERIOR);
        assertThat(aviso.html())
                .isEqualTo("<b>Tarea reasignada</b>\n"
                        + "Estudiante C reasignó la tarea «Diagramas de secuencia» a Estudiante E.");
        assertThat(aviso.textoBoton()).isEqualTo("Ver mis tareas");
        assertThat(aviso.ruta()).isEqualTo("/mis-tareas");
    }

    @Test
    void correccionEscapaHtml() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.tareaCorreccion()), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Corrección solicitada</b>\n"
                                + "Estudiante C te pidió rehacer la entrega de «Diagramas de secuencia». Puntos a corregir: Falta el caso &quot;sin conexión&quot; &amp; revisar &lt;notas&gt;.");
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

    private static final String CODIGO = "Ab3_-".repeat(8) + "xyz";

    @Test
    void recuperacionLlevaElCodigoYElEnlace() {
        Aviso aviso = renderizador.renderizarRecuperacion(ev(EjemplosEventos.recuperacionSolicitada()), CODIGO);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Restablecer tu contraseña</b>\n"
                                + "Se pidió restablecer la contraseña de tu cuenta de UniProject. Abre el enlace o copia este código en la pantalla «Restablecer contraseña»:\n"
                                + "<code>"
                                + CODIGO + "</code>\n"
                                + "Vence a las 14:20 (hora de Colombia) y sirve una sola vez. Si no lo pediste, ignora este mensaje: tu contraseña no cambia.");
        assertThat(aviso.textoBoton()).isEqualTo("Restablecer contraseña");
        assertThat(aviso.ruta()).isEqualTo("/restablecer-contrasena#codigo=" + CODIGO);
        assertThat(aviso.html()).doesNotContain("Estudiante A");
    }

    @Test
    void recuperacionExigeUnCodigoValido() {
        assertThatThrownBy(() -> renderizador.renderizar(ev(EjemplosEventos.recuperacionSolicitada()), Variante.UNICA))
                .isInstanceOf(PlantillaException.class);
        assertThatThrownBy(() ->
                        renderizador.renderizarRecuperacion(ev(EjemplosEventos.recuperacionSolicitada()), "corto"))
                .isInstanceOf(PlantillaException.class)
                .hasMessageNotContaining("corto");
        assertThatThrownBy(() -> renderizador.renderizarRecuperacion(ev(EjemplosEventos.hitoEnviado()), CODIGO))
                .isInstanceOf(PlantillaException.class);
    }

    @Test
    void contrasenaCambiada() {
        Aviso perfil = renderizador.renderizar(
                ev(EjemplosEventos.contrasenaCambiada(OrigenCambioContrasena.PERFIL)), Variante.UNICA);
        assertThat(perfil.html())
                .isEqualTo(
                        "<b>Tu contraseña cambió</b>\n"
                                + "La contraseña de tu cuenta de UniProject se cambió el 24/10/2026 a las 14:05 desde tu perfil. Se cerraron las demás sesiones abiertas. Si no fuiste tú, pide un enlace nuevo en «¿Olvidaste tu contraseña?» y avisa a tu docente.");
        assertThat(perfil.textoBoton()).isEqualTo("Ir a UniProject");
        assertThat(perfil.ruta()).isEqualTo("/ingresar");
        Aviso enlace = renderizador.renderizar(
                ev(EjemplosEventos.contrasenaCambiada(OrigenCambioContrasena.RECUPERACION)), Variante.UNICA);
        assertThat(enlace.html()).contains("a las 14:05 con un enlace de recuperación.");
    }

    @Test
    void cursoEstudianteAgregado() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.cursoEstudianteAgregado()), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Nuevo curso</b>\n"
                                + "Docente D te agregó al curso «Diseño de Software I» (2026-2). Ahí verás a tus compañeros y tus proyectos.");
        assertThat(aviso.textoBoton()).isEqualTo("Ver mis cursos");
        assertThat(aviso.ruta()).isEqualTo("/mis-cursos");
    }

    @Test
    void equipoVotacionAbierta() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.equipoVotacionAbierta()), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Votación de líder abierta</b>\n"
                                + "Ya puedes votar por el líder de «Equipo 4» en el proyecto «Proyecto final». La votación cierra el 26/10/2026 a las 14:05.");
        assertThat(aviso.textoBoton()).isEqualTo("Votar");
        assertThat(aviso.ruta()).isEqualTo("/equipos/4/votacion");
    }

    @Test
    void equipoLiderElegido() {
        Aviso votacion =
                renderizador.renderizar(ev(EjemplosEventos.equipoLiderElegido(OrigenLider.VOTACION)), Variante.UNICA);
        assertThat(votacion.html())
                .isEqualTo(
                        "<b>Nuevo líder</b>\n"
                                + "Estudiante C es el nuevo líder de «Equipo 4» en el proyecto «Proyecto final», elegido por votación.");
        assertThat(votacion.textoBoton()).isEqualTo("Ver votación");
        assertThat(votacion.ruta()).isEqualTo("/equipos/4/votacion");

        Aviso desempate =
                renderizador.renderizar(ev(EjemplosEventos.equipoLiderElegido(OrigenLider.DESEMPATE)), Variante.UNICA);
        assertThat(desempate.html())
                .isEqualTo(
                        "<b>Nuevo líder</b>\n"
                                + "Estudiante C es el nuevo líder de «Equipo 4» en el proyecto «Proyecto final», elegido por el docente tras un empate.");
    }

    @Test
    void equipoSinLider() {
        Aviso empate =
                renderizador.renderizar(ev(EjemplosEventos.equipoSinLider(MotivoSinLider.EMPATE)), Variante.UNICA);
        assertThat(empate.html())
                .isEqualTo(
                        "<b>Equipo sin líder</b>\n"
                                + "La votación de líder de «Equipo 4» en el proyecto «Proyecto final» terminó en empate. Elige al líder entre los empatados.");
        assertThat(empate.textoBoton()).isEqualTo("Ver votación");
        assertThat(empate.ruta()).isEqualTo("/equipos/4/votacion");

        Aviso sinVotos =
                renderizador.renderizar(ev(EjemplosEventos.equipoSinLider(MotivoSinLider.SIN_VOTOS)), Variante.UNICA);
        assertThat(sinVotos.html())
                .isEqualTo(
                        "<b>Equipo sin líder</b>\n"
                                + "La votación de líder de «Equipo 4» en el proyecto «Proyecto final» terminó sin votos. Nombra un líder o abre otra votación.");
    }

    @Test
    void equipoMiembroCambio() {
        Aviso aviso = renderizador.renderizar(
                ev(EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.ENTRO, true)), Variante.UNICA);
        assertThat(aviso.html()).isEqualTo("<b>Cambio en tu equipo</b>\n" + "Estudiante E entró a «Equipo 4».");
        assertThat(aviso.textoBoton()).isEqualTo("Ver tablero del equipo");
        assertThat(aviso.ruta()).isEqualTo("/equipos/4/tablero");

        Aviso escapado = renderizador.renderizar(
                ev(new EquipoMiembroCambio(
                        EjemplosEventos.PROYECTO,
                        EjemplosEventos.EQUIPO,
                        MovimientoEquipo.ENTRO,
                        false,
                        List.of(new Persona(103, "Ana <A> & B", 900000103L)),
                        List.of(EjemplosEventos.LIDER_C))),
                Variante.UNICA);
        assertThat(escapado.html())
                .isEqualTo("<b>Cambio en tu equipo</b>\n" + "Ana &lt;A&gt; &amp; B entró a «Equipo 4».");

        assertThatThrownBy(() -> renderizador.renderizar(
                        ev(EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.ENTRO, true)), Variante.ANTERIOR))
                .isInstanceOf(PlantillaException.class);
    }

    @Test
    void equipoMiembroCambioNuevo() {
        Aviso aviso = renderizador.renderizar(
                ev(EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.ENTRO, true)), Variante.NUEVO);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Nuevo equipo</b>\n" + "El docente te agregó a «Equipo 4» en el proyecto «Proyecto final».");
        assertThat(aviso.textoBoton()).isEqualTo("Ver tablero del equipo");
        assertThat(aviso.ruta()).isEqualTo("/equipos/4/tablero");
    }

    @Test
    void inscripcionPorCerrar() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.inscripcionPorCerrar()), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>La inscripción a equipos está por cerrar</b>\n"
                                + "Aún no tienes equipo en el proyecto «Proyecto final». La inscripción cierra el 25/10/2026 a las 12:00; después, el docente te asignará uno.");
        assertThat(aviso.textoBoton()).isEqualTo("Elegir equipo");
        assertThat(aviso.ruta()).isEqualTo("/proyectos/12/inscripcion");
    }

    @Test
    void inscripcionResumen() {
        Aviso aviso = renderizador.renderizar(ev(EjemplosEventos.inscripcionResumen()), Variante.UNICA);
        assertThat(aviso.html())
                .isEqualTo(
                        "<b>Cierre de equipos en menos de 24 horas</b>\n"
                                + "La inscripción del proyecto «Proyecto final» cierra el 25/10/2026 a las 12:00. Hay 2 estudiantes sin equipo y 3 cupos libres.");
        assertThat(aviso.textoBoton()).isEqualTo("Ver equipos del proyecto");
        assertThat(aviso.ruta()).isEqualTo("/proyectos/12");
    }
}
