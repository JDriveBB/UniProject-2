package co.uniproject.nucleo.trazabilidad.reporte;

import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.Calificacion;
import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.Cambio;
import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.Entrega;
import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.Integrante;
import co.uniproject.nucleo.trazabilidad.reporte.ReporteTrazabilidad.NotaIndividual;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * RF15: reporte de trazabilidad en PDF con Apache PDFBox 3 (Apache-2.0). Síncrono, en memoria, sin guardar el archivo
 * (Ley 1581). Helvetica estándar (WinAnsi): tildes y ñ salen bien; lo que la fuente no codifica sale como '?'.
 */
public final class GeneradorReportePdf {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Bogota"));
    private static final float MARGEN = 50f;
    private static final float TAM_TITULO = 16f;
    private static final float TAM_SECCION = 12f;
    private static final float TAM_TEXTO = 10f;
    private static final float TAM_PIE = 8f;
    private static final float INTERLINEA = 1.4f;

    private final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generar(ReporteTrazabilidad r) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Lienzo l = new Lienzo(doc);
            l.linea("Reporte de trazabilidad - UniProject", negrita, TAM_TITULO);
            l.linea("Proyecto: " + r.proyecto(), normal, TAM_TEXTO);
            l.linea("Equipo: " + r.equipo(), normal, TAM_TEXTO);
            l.linea("Docente: " + r.docente(), normal, TAM_TEXTO);
            l.linea("Generado: " + FECHA.format(r.generadoEn()) + " (hora de Colombia)", normal, TAM_TEXTO);

            seccion(l, "Integrantes", r.integrantes().isEmpty());
            for (Integrante i : r.integrantes()) {
                l.linea("- " + i.nombre() + " (" + i.rol() + ")", normal, TAM_TEXTO);
            }
            seccion(l, "Historial de cambios", r.cambios().isEmpty());
            for (Cambio c : r.cambios()) {
                l.linea(FECHA.format(c.fechaHora()) + " | " + c.autor() + " | " + c.accion() + " | " + c.detalle(),
                        normal, TAM_TEXTO);
            }
            seccion(l, "Entregas", r.entregas().isEmpty());
            for (Entrega e : r.entregas()) {
                l.linea(e.hito() + ": enviado el " + FECHA.format(e.fechaEnvio()) + " por " + e.enviadoPor(),
                        normal, TAM_TEXTO);
            }
            seccion(l, "Calificaciones", r.calificaciones().isEmpty());
            for (Calificacion c : r.calificaciones()) {
                l.linea(c.hito() + ": nota grupal " + nota(c.notaGrupal()) + " (" + c.caracter() + ")",
                        negrita, TAM_TEXTO);
                for (NotaIndividual n : c.notas()) {
                    String fin = n.notaFinal() == null ? "pendiente" : nota(n.notaFinal());
                    l.linea("- " + n.estudiante() + ": sugerida " + nota(n.notaSugerida()) + ", final " + fin,
                            normal, TAM_TEXTO);
                }
            }
            l.cerrar();
            numerarPaginas(doc);
            doc.getDocumentInformation().setTitle("Reporte de trazabilidad");
            doc.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void seccion(Lienzo l, String titulo, boolean vacia) throws IOException {
        l.espacio();
        l.linea(titulo, negrita, TAM_SECCION);
        if (vacia) {
            l.linea("(sin registros)", normal, TAM_TEXTO);
        }
    }

    private void numerarPaginas(PDDocument doc) throws IOException {
        int total = doc.getNumberOfPages();
        int numero = 0;
        for (PDPage pagina : doc.getPages()) {
            numero++;
            try (PDPageContentStream pie = new PDPageContentStream(doc, pagina,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                pie.beginText();
                pie.setFont(normal, TAM_PIE);
                pie.newLineAtOffset(MARGEN, MARGEN / 2);
                pie.showText("Página " + numero + " de " + total);
                pie.endText();
            }
        }
    }

    private static String nota(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Controles y separadores a espacio; lo que la fuente no puede codificar, a '?'. */
    static String limpiar(String texto, PDType1Font fuente) {
        StringBuilder sb = new StringBuilder(texto.length());
        texto.codePoints().forEach(cp -> {
            if (Character.isISOControl(cp) || Character.isSpaceChar(cp)) {
                sb.append(' ');
                return;
            }
            String s = new String(Character.toChars(cp));
            try {
                fuente.encode(s);
                sb.append(s);
            } catch (IllegalArgumentException | IOException e) {
                sb.append('?');
            }
        });
        return sb.toString();
    }

    private static float medir(String texto, PDType1Font fuente, float tamano) throws IOException {
        return fuente.getStringWidth(texto) / 1000f * tamano;
    }

    /** Parte el texto en líneas que caben en el ancho útil; una palabra más larga que el ancho se corta. */
    private static List<String> partir(String texto, PDType1Font fuente, float tamano) throws IOException {
        float ancho = PDRectangle.LETTER.getWidth() - 2 * MARGEN;
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ", -1)) {
            String candidata = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (medir(candidata, fuente, tamano) <= ancho) {
                actual.setLength(0);
                actual.append(candidata);
                continue;
            }
            if (!actual.isEmpty()) {
                lineas.add(actual.toString());
                actual.setLength(0);
            }
            String resto = palabra;
            while (medir(resto, fuente, tamano) > ancho) {
                int corte = resto.length() - 1;
                while (corte > 1 && medir(resto.substring(0, corte), fuente, tamano) > ancho) {
                    corte--;
                }
                lineas.add(resto.substring(0, corte));
                resto = resto.substring(corte);
            }
            actual.append(resto);
        }
        lineas.add(actual.toString());
        return lineas;
    }

    /** Escribe líneas de arriba abajo y abre una página nueva cuando la siguiente no cabe. */
    private static final class Lienzo {

        private final PDDocument doc;
        private PDPageContentStream contenido;
        private float y;

        Lienzo(PDDocument doc) throws IOException {
            this.doc = doc;
            this.contenido = abrirPagina();
        }

        private PDPageContentStream abrirPagina() throws IOException {
            PDPage pagina = new PDPage(PDRectangle.LETTER);
            doc.addPage(pagina);
            y = PDRectangle.LETTER.getHeight() - MARGEN;
            return new PDPageContentStream(doc, pagina);
        }

        void linea(String texto, PDType1Font fuente, float tamano) throws IOException {
            for (String parte : partir(limpiar(texto, fuente), fuente, tamano)) {
                float alto = tamano * INTERLINEA;
                if (y - alto < MARGEN) {
                    contenido.close();
                    contenido = abrirPagina();
                }
                y -= alto;
                contenido.beginText();
                contenido.setFont(fuente, tamano);
                contenido.newLineAtOffset(MARGEN, y);
                contenido.showText(parte);
                contenido.endText();
            }
        }

        void espacio() {
            y -= TAM_TEXTO;
        }

        void cerrar() throws IOException {
            contenido.close();
        }
    }
}
