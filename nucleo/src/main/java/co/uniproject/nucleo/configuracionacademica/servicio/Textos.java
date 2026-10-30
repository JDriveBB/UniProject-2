package co.uniproject.nucleo.configuracionacademica.servicio;

/** Normalización de textos de las solicitudes. */
public final class Textos {

    private Textos() {
    }

    /** null o en blanco → null; si no, sin espacios alrededor. */
    public static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
