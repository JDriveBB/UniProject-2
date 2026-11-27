package co.uniproject.nucleo.notificaciones;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.datos.TareaCorreccion;
import co.uniproject.nucleo.notificaciones.bandeja.NombresEnNotificaciones;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Cambia el nombre (y, si se pide, borra el chat) de una persona en las cargas de evento_saliente, publicadas o no, y
 * rehace desde el evento ya corregido el texto de sus avisos en la bandeja (decisiones 27-5, 27-6 y 27-15;
 * Aclaración 1) y, en la supresión, el resumen de las correcciones que pidió el titular (bloque 41). Todo por id: un
 * homónimo no se toca y no importa si la persona sigue en el equipo.
 */
@Component
public class PersonasEnEventos {

    /** Campos de data que guardan una Persona o la Referencia de una persona (eventos-contrato, paquete datos). */
    static final List<String> CAMPOS_PERSONA = List.of(
            "responsable",
            "docente",
            "integrantes",
            "estudiante",
            "lider",
            "integranteAnterior",
            "integranteNuevo",
            "titular",
            "personas",
            "estudiantes");

    /** Filtro grueso en la BD (cualquier id igual dentro de data); el filtro exacto es CAMPOS_PERSONA. */
    private static final String RUTA = "$.data.** ? (@.id == $id)";

    private final JdbcClient jdbc;
    private final CodecEventos codec;
    private final JsonMapper mapeador;
    private final NombresEnNotificaciones notificaciones;

    private record Fila(UUID id, String carga) {}

    public PersonasEnEventos(JdbcClient jdbc, CodecEventos codec, NombresEnNotificaciones notificaciones) {
        this.jdbc = jdbc;
        this.codec = codec;
        this.mapeador = codec.mapeador();
        this.notificaciones = notificaciones;
    }

    /** Devuelve cuántos eventos cambió. Debe correr dentro de la transacción del servicio que lo llama. */
    public int reemplazar(long idUsuario, String nombre, boolean quitarChat) {
        return reemplazar(idUsuario, nombre, quitarChat, null);
    }

    /**
     * Como el de 3 parámetros y, si resumenCorreccion no es null, también cambia el resumen de las correcciones que
     * pidió el titular (data.lider.id) por ese texto.
     */
    public int reemplazar(long idUsuario, String nombre, boolean quitarChat, String resumenCorreccion) {
        List<Fila> filas = jdbc.sql("SELECT id, carga::text AS carga FROM evento_saliente "
                        + "WHERE jsonb_path_exists(carga, CAST(:ruta AS jsonpath), "
                        + "jsonb_build_object('id', CAST(:id AS bigint))) ORDER BY creado_en FOR UPDATE")
                .param("ruta", RUTA)
                .param("id", idUsuario)
                .query((rs, fila) -> new Fila(rs.getObject("id", UUID.class), rs.getString("carga")))
                .list();
        int cambiados = 0;
        for (Fila f : filas) {
            JsonNode raiz = mapeador.readTree(f.carga());
            if (!(raiz.get("data") instanceof ObjectNode data)) {
                continue;
            }
            boolean cambio = false;
            for (String campo : CAMPOS_PERSONA) {
                JsonNode valor = data.get(campo);
                if (valor == null) {
                    continue;
                }
                if (valor.isArray()) {
                    for (int i = 0; i < valor.size(); i++) {
                        cambio |= cambiarPersona(valor.get(i), idUsuario, nombre, quitarChat);
                    }
                } else {
                    cambio |= cambiarPersona(valor, idUsuario, nombre, quitarChat);
                }
            }
            if (resumenCorreccion != null && esCorreccionDelLider(f.carga(), idUsuario)) {
                data.put("resumen", resumenCorreccion);
                cambio = true;
            }
            if (cambio) {
                String json = mapeador.writeValueAsString(raiz);
                jdbc.sql("UPDATE evento_saliente SET carga = CAST(:carga AS jsonb) WHERE id = :id")
                        .param("carga", json)
                        .param("id", f.id())
                        .update();
                notificaciones.regenerar(
                        codec.leer(f.carga().getBytes(StandardCharsets.UTF_8)),
                        codec.leer(json.getBytes(StandardCharsets.UTF_8)));
                cambiados++;
            }
        }
        return cambiados;
    }

    /** Decide con el evento tipado (no con la cadena type) y sobre la carga original, aún sin cambios. */
    private boolean esCorreccionDelLider(String carga, long idUsuario) {
        return codec.leer(carga.getBytes(StandardCharsets.UTF_8)).data() instanceof TareaCorreccion c
                && c.lider() != null
                && c.lider().id() == idUsuario;
    }

    private static boolean cambiarPersona(JsonNode nodo, long idUsuario, String nombre, boolean quitarChat) {
        if (!(nodo instanceof ObjectNode persona)) {
            return false;
        }
        JsonNode id = persona.get("id");
        if (id == null || !id.isIntegralNumber() || id.longValue() != idUsuario) {
            return false;
        }
        persona.put("nombre", nombre);
        if (quitarChat && persona.has("chatId")) {
            persona.putNull("chatId");
        }
        return true;
    }
}
