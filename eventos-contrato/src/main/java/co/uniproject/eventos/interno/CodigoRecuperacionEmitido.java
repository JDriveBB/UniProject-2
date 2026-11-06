package co.uniproject.eventos.interno;

/** Respuesta 200 de RUTA_RECUPERACIONES. El código es secreto: toString no lo muestra. */
public record CodigoRecuperacionEmitido(String codigo) {

    @Override
    public String toString() {
        return "CodigoRecuperacionEmitido[codigo=***]";
    }
}
