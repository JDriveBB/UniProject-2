package co.uniproject.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.uniproject.eventos.datos.*;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import co.uniproject.eventos.interno.ContratoInterno;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class ContratoEventosTest {

    private static final CodecEventos CODEC = new CodecEventos();
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-24T19:05:00Z"), ZoneId.of("America/Bogota"));

    @Test
    void cadaTipoTieneEsquemaYEjemplo() {
        for (TipoEvento tipo : TipoEvento.values()) {
            DatosEvento datos = EjemplosEventos.de(tipo);
            EventoCloud evento = EventoCloud.nuevo(datos, RELOJ);
            String json = CODEC.serializar(evento);
            assertThat(json).isNotNull();
            assertThat(TipoEvento.de(datos)).isEqualTo(tipo);
            assertThat(tipo.tipoDatos()).isEqualTo(datos.getClass());
            assertThat(TipoEvento.desdeClave(tipo.clave())).contains(tipo);
        }
    }

    @Test
    void idaYVueltaConservaLosDatos() {
        for (TipoEvento tipo : TipoEvento.values()) {
            EventoCloud original = EventoCloud.nuevo(EjemplosEventos.de(tipo), RELOJ);
            String json = CODEC.serializar(original);
            EventoCloud leido = CODEC.leer(json.getBytes(StandardCharsets.UTF_8));
            assertThat(leido).isEqualTo(original);
        }
    }

    @Test
    void serializaConLosNombresDelContrato() {
        EventoCloud reasignada = EventoCloud.nuevo(EjemplosEventos.tareaReasignada(), RELOJ);
        String jsonReasignada = CODEC.serializar(reasignada);
        assertThat(jsonReasignada)
                .contains("\"specversion\":\"1.0\"")
                .contains("\"source\":\"/uniproject/nucleo\"")
                .contains("\"type\":\"tarea.reasignada\"")
                .contains("\"time\":\"2026-10-24T19:05:00Z\"")
                .contains("\"integranteAnterior\"")
                .contains("\"chatId\":900000101")
                .contains("\"fecha\":\"2026-10-24T19:05:00Z\"");

        EventoCloud riesgo = EventoCloud.nuevo(EjemplosEventos.tareaRutaCriticaRiesgo(), RELOJ);
        String jsonRiesgo = CODEC.serializar(riesgo);
        assertThat(jsonRiesgo).contains("\"fechaLimiteHito\":\"2026-11-05\"");
    }

    @Test
    void serializarRechazaDatosQueNoCumplen() {
        TareaCorreccion correccionInvalida = new TareaCorreccion(
                EjemplosEventos.PROYECTO,
                EjemplosEventos.EQUIPO,
                EjemplosEventos.TAREA,
                EjemplosEventos.INTEGRANTE_A,
                new Referencia(100, "Estudiante C"),
                "");
        EventoCloud evento = EventoCloud.nuevo(correccionInvalida, RELOJ);
        assertThatThrownBy(() -> CODEC.serializar(evento)).isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    void leerRechazaCuerposInvalidos() throws Exception {
        assertThatThrownBy(() -> CODEC.leer("no es json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(EventoInvalidoException.class);

        assertThatThrownBy(() -> CODEC.leer("{}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(EventoInvalidoException.class);

        assertThatThrownBy(() -> CODEC.leer("{\"type\":\"otro.evento\"}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(EventoInvalidoException.class);

        EventoCloud hitoEnviado = EventoCloud.nuevo(EjemplosEventos.hitoEnviado(), RELOJ);
        String json = CODEC.serializar(hitoEnviado);
        JsonNode raiz = CODEC.mapeador().readTree(json);
        ((ObjectNode) raiz.get("data")).remove("docente");
        String jsonSinDocente = CODEC.mapeador().writeValueAsString(raiz);

        assertThatThrownBy(() -> CODEC.leer(jsonSinDocente.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    void leerToleraCamposDesconocidos() throws Exception {
        EventoCloud hitoEnviado = EventoCloud.nuevo(EjemplosEventos.hitoEnviado(), RELOJ);
        String json = CODEC.serializar(hitoEnviado);
        JsonNode raiz = CODEC.mapeador().readTree(json);
        ((ObjectNode) raiz.get("data")).put("extra", 1);
        String jsonModificado = CODEC.mapeador().writeValueAsString(raiz);

        EventoCloud leido = CODEC.leer(jsonModificado.getBytes(StandardCharsets.UTF_8));
        assertThat(leido.data()).isEqualTo(EjemplosEventos.hitoEnviado());
    }

    @Test
    void personaSinTelegramViajaConChatIdNulo() {
        EventoCloud evaluado = EventoCloud.nuevo(EjemplosEventos.hitoEvaluado(CaracterNota.PROVISIONAL), RELOJ);
        String json = CODEC.serializar(evaluado);
        EventoCloud leido = CODEC.leer(json.getBytes(StandardCharsets.UTF_8));
        HitoEvaluado datos = (HitoEvaluado) leido.data();
        assertThat(datos.integrantes().get(2).chatId()).isNull();
    }

    @Test
    void patronDelCodigoDeVinculacion() {
        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO, "A".repeat(32)))
                .isTrue();
        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO, "aZ09_-".repeat(5) + "ab"))
                .isTrue();

        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO, "A".repeat(31)))
                .isFalse();
        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO, "A".repeat(33)))
                .isFalse();
        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO, "A".repeat(31) + "+"))
                .isFalse();
    }

    @Test
    void mensajeDeEventoInvalidoNoPermiteFalsificarLineasDeLog() {
        assertThatThrownBy(() -> CODEC.leer("{\"type\":\"otro\\r\\nWARN falso\\tx\"}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessage("type ausente o desconocido: otro??WARN falso?x");

        String largo = "x".repeat(5000);
        assertThatThrownBy(() -> CODEC.leer(("{\"type\":\"" + largo + "\"}").getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessage(("type ausente o desconocido: " + largo).substring(0, 500) + "...");
    }

    @Test
    void eventosDeCuentaNoAdmitenCamposExtra() {
        for (DatosEvento datos : List.of(
                EjemplosEventos.recuperacionSolicitada(),
                EjemplosEventos.contrasenaCambiada(OrigenCambioContrasena.RECUPERACION))) {
            String json = CODEC.serializar(EventoCloud.nuevo(datos, RELOJ));
            assertThat(json).doesNotContain("\"codigo\"");
            JsonNode conCodigo = CODEC.mapeador().readTree(json);
            ((ObjectNode) conCodigo.get("data")).put("codigo", "A".repeat(43));
            byte[] cuerpoConCodigo =
                    CODEC.mapeador().writeValueAsString(conCodigo).getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> CODEC.leer(cuerpoConCodigo)).isInstanceOf(EventoInvalidoException.class);
            JsonNode conCorreo = CODEC.mapeador().readTree(json);
            ((ObjectNode) conCorreo.get("data").get("titular")).put("email", "a@u.edu.co");
            byte[] cuerpoConCorreo =
                    CODEC.mapeador().writeValueAsString(conCorreo).getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> CODEC.leer(cuerpoConCorreo)).isInstanceOf(EventoInvalidoException.class);
        }
    }

    @Test
    void patronDelCodigoDeRecuperacion() {
        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO_RECUPERACION, "A".repeat(43)))
                .isTrue();
        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO_RECUPERACION, "aZ09_-".repeat(7) + "b"))
                .isTrue();

        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO_RECUPERACION, "A".repeat(42)))
                .isFalse();
        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO_RECUPERACION, "A".repeat(44)))
                .isFalse();
        assertThat(Pattern.matches(ContratoInterno.PATRON_CODIGO_RECUPERACION, "A".repeat(42) + "+"))
                .isFalse();
    }

    @Test
    void serializaLosEventosDeCursosYEquipos() {
        List<DatosEvento> eventos = List.of(
                EjemplosEventos.cursoEstudianteAgregado(),
                EjemplosEventos.equipoVotacionAbierta(),
                EjemplosEventos.equipoLiderElegido(OrigenLider.VOTACION),
                EjemplosEventos.equipoSinLider(MotivoSinLider.EMPATE),
                EjemplosEventos.equipoMiembroCambio(MovimientoEquipo.ENTRO, true),
                EjemplosEventos.inscripcionPorCerrar(),
                EjemplosEventos.inscripcionResumen());
        StringBuilder todos = new StringBuilder();
        for (DatosEvento datos : eventos) {
            String json = CODEC.serializar(EventoCloud.nuevo(datos, RELOJ));
            assertThat(json).doesNotContain("@");
            todos.append(json);
        }
        assertThat(todos.toString())
                .contains("\"periodo\":\"2026-2\"")
                .contains("\"cierraEn\":\"2026-10-26T19:05:00Z\"")
                .contains("\"origen\":\"VOTACION\"")
                .contains("\"motivo\":\"EMPATE\"")
                .contains("\"movimiento\":\"ENTRO\"")
                .contains("\"porDocente\":true")
                .contains("\"sinEquipo\":2")
                .contains("\"cuposLibres\":3");
    }

    @Test
    void listasObligatoriasNoPuedenIrVacias() {
        EventoCloud votacionVacia = EventoCloud.nuevo(
                new EquipoVotacionAbierta(
                        EjemplosEventos.PROYECTO, EjemplosEventos.EQUIPO, EjemplosEventos.CIERRE_VOTACION, List.of()),
                RELOJ);
        assertThatThrownBy(() -> CODEC.serializar(votacionVacia)).isInstanceOf(EventoInvalidoException.class);

        EventoCloud miembroSinPersonas = EventoCloud.nuevo(
                new EquipoMiembroCambio(
                        EjemplosEventos.PROYECTO,
                        EjemplosEventos.EQUIPO,
                        MovimientoEquipo.ENTRO,
                        true,
                        List.of(),
                        List.of(EjemplosEventos.LIDER_C)),
                RELOJ);
        assertThatThrownBy(() -> CODEC.serializar(miembroSinPersonas)).isInstanceOf(EventoInvalidoException.class);

        EventoCloud porCerrarVacio = EventoCloud.nuevo(
                new InscripcionPorCerrar(EjemplosEventos.PROYECTO, EjemplosEventos.CIERRE_INSCRIPCION, List.of()),
                RELOJ);
        assertThatThrownBy(() -> CODEC.serializar(porCerrarVacio)).isInstanceOf(EventoInvalidoException.class);

        EventoCloud miembroSinRestantes = EventoCloud.nuevo(
                new EquipoMiembroCambio(
                        EjemplosEventos.PROYECTO,
                        EjemplosEventos.EQUIPO,
                        MovimientoEquipo.ENTRO,
                        true,
                        List.of(EjemplosEventos.INTEGRANTE_E),
                        List.of()),
                RELOJ);
        assertThat(CODEC.serializar(miembroSinRestantes)).isNotBlank();
    }
}
