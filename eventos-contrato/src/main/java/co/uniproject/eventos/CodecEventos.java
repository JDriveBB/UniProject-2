package co.uniproject.eventos;

import co.uniproject.eventos.datos.DatosEvento;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Serializa, lee y valida eventos contra su JSON Schema. Seguro para varios hilos. Una instancia por aplicación (bean). */
public final class CodecEventos {

    private final JsonMapper mapeador = JsonMapper.builder().build();
    private final Map<TipoEvento, Schema> esquemas = new EnumMap<>(TipoEvento.class);

    public CodecEventos() {
        SchemaRegistry registro = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);
        for (TipoEvento tipo : TipoEvento.values()) {
            try (InputStream in = CodecEventos.class.getClassLoader().getResourceAsStream(tipo.rutaEsquema())) {
                if (in == null) {
                    throw new IllegalStateException("Falta el esquema " + tipo.rutaEsquema());
                }
                esquemas.put(tipo, registro.getSchema(in));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** Mapeador JSON compartido (Jackson 3). El agente lo usa para convertir datos a Map en las plantillas. */
    public JsonMapper mapeador() { return mapeador; }

    /** JSON del evento. Lanza EventoInvalidoException si no cumple el esquema de su tipo. */
    public String serializar(EventoCloud evento) {
        TipoEvento tipo = TipoEvento.desdeClave(evento.type())
                .orElseThrow(() -> new EventoInvalidoException("type desconocido: " + evento.type()));
        String json = mapeador.writeValueAsString(evento);
        validar(tipo, mapeador.readTree(json));
        return json;
    }

    /** Lee y valida un evento recibido. Lanza EventoInvalidoException ante cualquier problema de forma. */
    public EventoCloud leer(byte[] cuerpo) {
        JsonNode raiz;
        try {
            raiz = mapeador.readTree(cuerpo);
        } catch (JacksonException e) {
            throw new EventoInvalidoException("El cuerpo no es JSON válido", e);
        }
        if (raiz == null || !raiz.isObject()) {
            throw new EventoInvalidoException("El cuerpo no es un objeto JSON");
        }
        String clave = null;
        JsonNode nodoTipo = raiz.get("type");
        if (nodoTipo != null) {
            try {
                clave = mapeador.treeToValue(nodoTipo, String.class);
            } catch (JacksonException e) {
                clave = null;
            }
        }
        final String claveLeida = clave;
        TipoEvento tipo = TipoEvento.desdeClave(claveLeida == null ? "" : claveLeida)
                .orElseThrow(() -> new EventoInvalidoException("type ausente o desconocido: " + claveLeida));
        validar(tipo, raiz);
        try {
            Sobre sobre = mapeador.treeToValue(raiz, Sobre.class);
            DatosEvento datos = mapeador.treeToValue(sobre.data(), tipo.tipoDatos());
            return new EventoCloud(sobre.specversion(), sobre.id(), sobre.source(), sobre.type(),
                    sobre.time(), sobre.datacontenttype(), datos);
        } catch (JacksonException | NullPointerException e) {
            throw new EventoInvalidoException("No se pudo convertir el evento " + tipo.clave(), e);
        }
    }

    private void validar(TipoEvento tipo, JsonNode nodo) {
        List<com.networknt.schema.Error> errores = esquemas.get(tipo).validate(nodo);
        if (!errores.isEmpty()) {
            throw new EventoInvalidoException("El evento " + tipo.clave() + " no cumple su esquema: " + errores);
        }
    }

    /** Sobre CloudEvents con data sin convertir. */
    record Sobre(String specversion, UUID id, String source, String type,
                 Instant time, String datacontenttype, JsonNode data) {}
}
