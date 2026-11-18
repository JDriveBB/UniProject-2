package co.uniproject.nucleo.comun.web;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Texto de entrada JSON estricto (bloque 41): solo acepta cadenas JSON y rechaza el carácter nulo (que PostgreSQL no
 * guarda y que antes terminaba en un 500). Números, booleanos, objetos y arreglos ya no se convierten a texto. El
 * mensaje de error nunca incluye el valor recibido.
 */
final class TextoEstrictoDeserializer extends StdScalarDeserializer<String> {

    TextoEstrictoDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.VALUE_STRING) {
            return ctxt.reportInputMismatch(this, "se esperaba texto");
        }
        String texto = p.getString();
        if (texto.indexOf('\0') >= 0) {
            return ctxt.reportInputMismatch(this, "texto con carácter nulo");
        }
        return texto;
    }
}
