package co.uniproject.agente.vinculacion;

/** Resultado de pedir el código de recuperación al núcleo. toString no muestra el código (es secreto). */
public record CodigoRecuperacion(Estado estado, String codigo) {

    public enum Estado { EMITIDO, DESCARTADA, ERROR }

    public static CodigoRecuperacion emitido(String codigo) {
        return new CodigoRecuperacion(Estado.EMITIDO, codigo);
    }

    public static CodigoRecuperacion descartada() {
        return new CodigoRecuperacion(Estado.DESCARTADA, null);
    }

    public static CodigoRecuperacion error() {
        return new CodigoRecuperacion(Estado.ERROR, null);
    }

    @Override
    public String toString() {
        return "CodigoRecuperacion[estado=" + estado + "]";
    }
}
