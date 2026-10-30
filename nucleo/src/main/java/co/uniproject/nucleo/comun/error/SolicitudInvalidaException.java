package co.uniproject.nucleo.comun.error;

/** 400 con código propio, para validaciones que Bean Validation no expresa (p. ej. tamaño o tipo de un archivo). */
public class SolicitudInvalidaException extends RuntimeException {

    private final String codigo;

    public SolicitudInvalidaException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
