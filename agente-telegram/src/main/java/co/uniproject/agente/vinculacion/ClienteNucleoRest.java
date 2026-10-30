package co.uniproject.agente.vinculacion;

import co.uniproject.eventos.interno.ContratoInterno;
import co.uniproject.eventos.interno.SolicitudBloqueo;
import co.uniproject.eventos.interno.SolicitudVinculacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

public class ClienteNucleoRest implements ClienteNucleo {

    private static final Logger log = LoggerFactory.getLogger(ClienteNucleoRest.class);

    private final RestClient rest;

    public ClienteNucleoRest(RestClient.Builder builder, String urlNucleo, String tokenInterno) {
        this.rest = builder.baseUrl(urlNucleo)
                .defaultHeader(ContratoInterno.CABECERA_TOKEN, tokenInterno)
                .build();
    }

    @Override
    public ResultadoVinculacion vincular(String codigo, long chatId) {
        try {
            ResponseEntity<Void> respuesta = rest.post()
                    .uri(ContratoInterno.RUTA_VINCULACIONES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SolicitudVinculacion(codigo, chatId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> { })
                    .toBodilessEntity();
            int estado = respuesta.getStatusCode().value();
            if (estado == 204) {
                return ResultadoVinculacion.VINCULADO;
            }
            if (estado == 404) {
                return ResultadoVinculacion.CODIGO_INEXISTENTE;
            }
            if (estado == 410) {
                return ResultadoVinculacion.CODIGO_VENCIDO_O_USADO;
            }
            log.warn("El núcleo no pudo procesar la vinculación (estado {})", estado);
            return ResultadoVinculacion.ERROR;
        } catch (RuntimeException e) {
            log.warn("El núcleo no pudo procesar la vinculación (excepción {})", e.getClass().getName());
            return ResultadoVinculacion.ERROR;
        }
    }

    @Override
    public void informarBloqueo(long chatId) {
        try {
            ResponseEntity<Void> respuesta = rest.post()
                    .uri(ContratoInterno.RUTA_BLOQUEOS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SolicitudBloqueo(chatId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> { })
                    .toBodilessEntity();
            int estado = respuesta.getStatusCode().value();
            if (estado != 204) {
                log.warn("El núcleo no registró el bloqueo (estado {})", estado);
            }
        } catch (RuntimeException e) {
            log.warn("El núcleo no registró el bloqueo (excepción {})", e.getClass().getName());
        }
    }
}
