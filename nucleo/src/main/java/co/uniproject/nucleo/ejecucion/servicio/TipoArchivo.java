package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.ejecucion.dominio.TipoEvidencia;
import java.util.Optional;

/** Restricción 5 y R-14: tipos de archivo aceptados, reconocidos por sus primeros bytes. */
public enum TipoArchivo {
    PDF("pdf", "application/pdf", TipoEvidencia.PDF),
    PNG("png", "image/png", TipoEvidencia.IMAGEN),
    JPEG("jpg", "image/jpeg", TipoEvidencia.IMAGEN);

    private static final byte[] FIRMA_PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final String extension;
    private final String mime;
    private final TipoEvidencia tipoEvidencia;

    TipoArchivo(String extension, String mime, TipoEvidencia tipoEvidencia) {
        this.extension = extension;
        this.mime = mime;
        this.tipoEvidencia = tipoEvidencia;
    }

    public String extension() {
        return extension;
    }

    public String mime() {
        return mime;
    }

    public TipoEvidencia tipoEvidencia() {
        return tipoEvidencia;
    }

    /** El tipo lo deciden los primeros bytes, no el nombre ni el Content-Type que mandó el cliente. */
    public static Optional<TipoArchivo> detectar(byte[] contenido) {
        if (empiezaCon(contenido, FIRMA_PDF)) {
            return Optional.of(PDF);
        }
        if (empiezaCon(contenido, FIRMA_PNG)) {
            return Optional.of(PNG);
        }
        if (empiezaCon(contenido, FIRMA_JPEG)) {
            return Optional.of(JPEG);
        }
        return Optional.empty();
    }

    /** Por la extensión de la clave guardada (evidencias/<uuid>.<ext>). */
    public static TipoArchivo deClave(String clave) {
        for (TipoArchivo t : values()) {
            if (clave.endsWith("." + t.extension)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Clave de evidencia sin extensión conocida");
    }

    /** Nombre original sin ruta, controles ni comillas, a lo sumo 255 caracteres; vacío → "evidencia.<ext>". */
    public static String nombreSeguro(String original, String extension) {
        String nombre = original == null ? "" : original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        nombre = nombre.replaceAll("[\\p{Cc}\\p{Zl}\\p{Zp}\"]", "").strip();
        if (nombre.isEmpty()) {
            return "evidencia." + extension;
        }
        int[] puntos = nombre.codePoints().limit(255).toArray();
        return new String(puntos, 0, puntos.length);
    }

    private static boolean empiezaCon(byte[] datos, byte[] firma) {
        if (datos.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (datos[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
