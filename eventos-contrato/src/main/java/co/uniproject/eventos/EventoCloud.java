package co.uniproject.eventos;

import co.uniproject.eventos.datos.DatosEvento;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/** Evento CloudEvents 1.0 en modo estructurado (plan §8.2). */
public record EventoCloud(String specversion, UUID id, String source, String type,
                          Instant time, String datacontenttype, DatosEvento data) {

    public static final String SPECVERSION = "1.0";
    public static final String DATACONTENTTYPE = "application/json";

    public EventoCloud {
        Objects.requireNonNull(specversion, "specversion");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(datacontenttype, "datacontenttype");
        Objects.requireNonNull(data, "data");
    }

    /** Crea un evento nuevo del núcleo con id aleatorio y la hora del reloj (truncada a segundos). */
    public static EventoCloud nuevo(DatosEvento datos, Clock reloj) {
        return new EventoCloud(SPECVERSION, UUID.randomUUID(), Topologia.SOURCE_NUCLEO,
                TipoEvento.de(datos).clave(), Instant.now(reloj).truncatedTo(ChronoUnit.SECONDS),
                DATACONTENTTYPE, datos);
    }
}
