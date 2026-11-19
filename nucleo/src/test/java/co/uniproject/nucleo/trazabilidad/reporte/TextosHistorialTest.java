package co.uniproject.nucleo.trazabilidad.reporte;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TextosHistorialTest {

    private static final Map<Long, String> NOMBRES = Map.of(10L, "Ana", 11L, "Bruno");
    private static final Map<Long, String> TAREAS = Map.of(40L, "Levantar requisitos");

    private static void sinFlechaUnicode(List<String> resultados) {
        assertThat(resultados).allSatisfy(texto -> assertThat(texto).doesNotContain("→"));
    }

    @Test
    void traduceAccionesYEntidades() {
        List<String> resultados = List.of(
                TextosHistorial.accion("CAMBIAR_ESTADO", "tarea"),
                TextosHistorial.accion("SOLICITAR_APOYO", "tarea"),
                TextosHistorial.accion("ENVIAR_HITO", "hito_equipo"),
                TextosHistorial.accion("ACTUALIZAR", "miembro_equipo"),
                TextosHistorial.accion("CREAR", "equipo"),
                TextosHistorial.accion("NUEVA", "otra"),
                TextosHistorial.accion(null, "tarea"),
                TextosHistorial.accion("CREAR", null),
                TextosHistorial.accion(null, null));

        assertThat(resultados)
                .containsExactly(
                        "Cambió el estado · Tarea",
                        "Pidió apoyo · Tarea",
                        "Envió el hito a revisión · Hito",
                        "Modificó · Integrantes",
                        "Creó · Equipo",
                        "NUEVA · otra",
                        " · Tarea",
                        "Creó · ",
                        " · ");
        sinFlechaUnicode(resultados);

        for (AccionAuditoria accion : AccionAuditoria.values()) {
            String traducida = TextosHistorial.accion(accion.name(), "tarea");
            assertThat(traducida).doesNotStartWith(accion.name() + " ·").doesNotContain("→");
        }
    }

    @Test
    void traduceCambiosDePersona() {
        List<String> resultados = List.of(
                TextosHistorial.detalle("id_encargado", "10", "11", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("id_encargado", "null", "11", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("id_encargado", "abc", "10", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("id_encargado", "10", "99", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("id_usuario", null, "11", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("id_usuario", "11", null, "", NOMBRES, TAREAS),
                TextosHistorial.detalle("lider", null, "10", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("id_encargado", null, null, "respaldo", NOMBRES, TAREAS),
                TextosHistorial.detalle("id_encargado", null, null, null, NOMBRES, TAREAS));

        assertThat(resultados)
                .containsExactly(
                        "Encargado: Ana -> Bruno",
                        "Encargado: Sin asignar -> Bruno",
                        "Encargado: Sin asignar -> Ana",
                        "Encargado: Ana -> Exintegrante",
                        "Integrante: Bruno",
                        "Integrante: Bruno",
                        "Líder: Ana",
                        "respaldo",
                        "");
        sinFlechaUnicode(resultados);
    }

    @Test
    void traduceNotasFinalesYGrupalesConComa() {
        List<String> resultados = List.of(
                TextosHistorial.detalle("nota_final:10", "3.50", "4.00", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("nota_final:10", null, "4.00", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("nota_final:99", null, "4.00", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("nota_final:x", null, "4.00", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("nota_final:10", "3.50", null, "respaldo", NOMBRES, TAREAS),
                TextosHistorial.detalle("nota_grupal", "3.50", "4.00", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("nota_grupal", null, "4.00", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("nota_grupal", "3.50", null, "respaldo", NOMBRES, TAREAS));

        assertThat(resultados)
                .containsExactly(
                        "Nota final de Ana: 3,50 -> 4,00",
                        "Nota final de Ana: 4,00",
                        "Nota final de Exintegrante: 4,00",
                        "Nota final de Sin asignar: 4,00",
                        "respaldo",
                        "Nota grupal: 3,50 -> 4,00",
                        "Nota grupal: 4,00",
                        "respaldo");
        sinFlechaUnicode(resultados);
    }

    @Test
    void omiteDetalleEnSolicitudesYTareaParcial() {
        assertThat(TextosHistorial.detalle("solicitud_apoyo", null, "33", "x", NOMBRES, TAREAS))
                .isEmpty();
        assertThat(TextosHistorial.detalle("solicitud_correccion", null, "33", "x", NOMBRES, TAREAS))
                .isEmpty();
        assertThat(TextosHistorial.detalle("tarea_parcial", null, "33", "x", NOMBRES, TAREAS))
                .isEmpty();
    }

    @Test
    void traduceEstadosYAvance() {
        List<String> resultados = List.of(
                TextosHistorial.detalle("estado", "PENDIENTE", "COMPLETADA", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("estado", "EN_PROGRESO", "COMPLETADA", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("estado", "EN_CURSO", "ENVIADO", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("estado", "ENVIADO", "EVALUADO", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("estado", null, "COMPLETADA", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("estado", null, "OTRO", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("estado", "PENDIENTE", null, "estado: PENDIENTE -> -", NOMBRES, TAREAS),
                TextosHistorial.detalle("porcentaje_avance", "0", "100", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("porcentaje_avance", null, "60", "", NOMBRES, TAREAS),
                TextosHistorial.detalle(
                        "porcentaje_avance", "60", null, "porcentaje_avance: 60 -> -", NOMBRES, TAREAS));

        assertThat(resultados)
                .containsExactly(
                        "Estado: Pendiente -> Completada",
                        "Estado: En progreso -> Completada",
                        "Estado: En progreso -> En Revisión",
                        "Estado: En Revisión -> Evaluado",
                        "Estado: Completada",
                        "Estado: OTRO",
                        "estado: PENDIENTE -> -",
                        "Avance: 0 % -> 100 %",
                        "Avance: 60 %",
                        "porcentaje_avance: 60 -> -");
        sinFlechaUnicode(resultados);
    }

    @Test
    void traducePrerrequisitoYEvidencia() {
        List<String> resultados = List.of(
                TextosHistorial.detalle("prerrequisito", null, "40", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("prerrequisito", "40", null, "", NOMBRES, TAREAS),
                TextosHistorial.detalle("prerrequisito", "99", null, "", NOMBRES, TAREAS),
                TextosHistorial.detalle("prerrequisito", null, "abc", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("prerrequisito", null, null, "prerrequisito: - -> -", NOMBRES, TAREAS),
                TextosHistorial.detalle("evidencia", null, "PDF 3", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("evidencia", null, "IMAGEN 5", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("evidencia", null, "ENLACE 1", "", NOMBRES, TAREAS),
                TextosHistorial.detalle("evidencia", null, "OTRO 1", "evidencia: - -> OTRO 1", NOMBRES, TAREAS),
                TextosHistorial.detalle("evidencia", null, null, "evidencia: - -> -", NOMBRES, TAREAS));

        assertThat(resultados)
                .containsExactly(
                        "Prerrequisito agregado: Levantar requisitos",
                        "Prerrequisito quitado: Levantar requisitos",
                        "Prerrequisito quitado: Tarea eliminada",
                        "Prerrequisito agregado: Tarea eliminada",
                        "prerrequisito: - -> -",
                        "Evidencia: PDF",
                        "Evidencia: imagen",
                        "Evidencia: enlace",
                        "evidencia: - -> OTRO 1",
                        "evidencia: - -> -");
        sinFlechaUnicode(resultados);
    }

    @Test
    void usaUltimoRecursoParaCamposDesconocidosONulos() {
        List<String> resultados = List.of(
                TextosHistorial.detalle("otro_campo", "a", "b", "detalle propio", NOMBRES, TAREAS),
                TextosHistorial.detalle(null, null, null, "Diseño de BD", NOMBRES, TAREAS),
                TextosHistorial.detalle(null, null, null, null, NOMBRES, TAREAS));

        assertThat(resultados).containsExactly("detalle propio", "Diseño de BD", "");
        sinFlechaUnicode(resultados);
    }
}
