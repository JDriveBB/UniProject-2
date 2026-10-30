package co.uniproject.nucleo.trazabilidad.reporte;

import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.Calificacion;
import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.Cambio;
import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.Entrega;
import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.Integrante;
import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.NotaIndividual;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorReportePdfTest {

    private static final Instant GENERADO = Instant.parse("2026-11-02T15:30:00Z");
    private final GeneradorReportePdf generador = new GeneradorReportePdf();

    private static String texto(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static int paginas(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.getNumberOfPages();
        }
    }

    private static ReporteTrazabilidad reporte(String equipo, List<Cambio> cambios) {
        return new ReporteTrazabilidad("Proyecto de prueba", equipo, "Ana María Pérez", GENERADO,
                List.of(new Integrante("José Núñez", "Líder"), new Integrante("Luisa Gómez", "Integrante")),
                cambios,
                List.of(new Entrega("Hito 1", Instant.parse("2026-10-20T22:05:00Z"), "Luisa Gómez")),
                List.of(new Calificacion("Hito 1", new BigDecimal("4.1"), "provisional", List.of(
                        new NotaIndividual("José Núñez", new BigDecimal("4.25"), new BigDecimal("4.3")),
                        new NotaIndividual("Luisa Gómez", new BigDecimal("3.5"), null)))));
    }

    private static List<Cambio> dosCambios() {
        return List.of(
                new Cambio(Instant.parse("2026-10-15T14:00:00Z"), "José Núñez", "REASIGNAR_TAREA", "encargado: Luisa -> José"),
                new Cambio(Instant.parse("2026-10-16T14:00:00Z"), "Luisa Gómez", "CAMBIAR_ESTADO", "estado: EN_PROGRESO -> COMPLETADA"));
    }

    @Test
    void empiezaComoPdf() {
        byte[] pdf = generador.generar(reporte("Equipo Ñandú", dosCambios()));
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void contieneLasSeccionesYLosDatos() throws IOException {
        String t = texto(generador.generar(reporte("Equipo Ñandú", dosCambios())));
        assertThat(t).contains("Reporte de trazabilidad - UniProject", "Proyecto: Proyecto de prueba",
                "Equipo: Equipo Ñandú", "Docente: Ana María Pérez", "Generado: 02/11/2026 10:30 (hora de Colombia)",
                "Integrantes", "- José Núñez (Líder)", "Historial de cambios",
                "15/10/2026 09:00 | José Núñez | REASIGNAR_TAREA | encargado: Luisa -> José", "Entregas",
                "Hito 1: enviado el 20/10/2026 17:05 por Luisa Gómez", "Calificaciones",
                "Hito 1: nota grupal 4,10 (provisional)", "- José Núñez: sugerida 4,25, final 4,30",
                "- Luisa Gómez: sugerida 3,50, final pendiente", "Página 1 de 1");
    }

    @Test
    void listasVaciasDicenSinRegistros() throws IOException {
        ReporteTrazabilidad vacio = new ReporteTrazabilidad("P", "E", "D", GENERADO, List.of(), List.of(), List.of(), List.of());
        String t = texto(generador.generar(vacio));
        assertThat(t.split("\\(sin registros\\)", -1)).hasSize(5);
    }

    @Test
    void loQueLaFuenteNoCodificaSaleComoInterrogacion() throws IOException {
        String t = texto(generador.generar(reporte("Equipo 😀 → A\tB", dosCambios())));
        assertThat(t).contains("Equipo: Equipo ? ? A B");
    }

    @Test
    void historialLargoOcupaVariasPaginasNumeradas() throws IOException {
        List<Cambio> muchos = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            muchos.add(new Cambio(Instant.parse("2026-10-15T14:00:00Z"), "José Núñez", "ACTUALIZAR_AVANCE", "avance: " + i));
        }
        byte[] pdf = generador.generar(reporte("Equipo A", muchos));
        int n = paginas(pdf);
        assertThat(n).isGreaterThanOrEqualTo(3);
        assertThat(texto(pdf)).contains("Página 1 de " + n, "Página " + n + " de " + n);
    }

    @Test
    void textoLargoSeParteEnLineas() throws IOException {
        String largo = "palabra ".repeat(80) + "x".repeat(300);
        byte[] pdf = generador.generar(reporte("Equipo A", List.of(
                new Cambio(Instant.parse("2026-10-15T14:00:00Z"), "José Núñez", "ACTUALIZAR", largo))));
        String t = texto(pdf);
        assertThat(t).contains("palabra palabra", "xxxxxxxxxx");
        assertThat(paginas(pdf)).isEqualTo(1);
    }
}
