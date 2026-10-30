package co.uniproject.nucleo.vinculaciontelegram;

import co.uniproject.eventos.interno.ContratoInterno;
import co.uniproject.eventos.interno.SolicitudBloqueo;
import co.uniproject.eventos.interno.SolicitudVinculacion;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorInternoTelegram {

    private static final Pattern PATRON_CODIGO = Pattern.compile(ContratoInterno.PATRON_CODIGO);

    private final ServicioVinculacion servicio;

    public ControladorInternoTelegram(ServicioVinculacion servicio) {
        this.servicio = servicio;
    }

    @PostMapping(path = ContratoInterno.RUTA_VINCULACIONES, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> vincular(@RequestBody SolicitudVinculacion solicitud) {
        if (solicitud == null
                || solicitud.codigo() == null
                || !PATRON_CODIGO.matcher(solicitud.codigo()).matches()
                || solicitud.chatId() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        ResultadoConfirmacion resultado = servicio.confirmar(solicitud.codigo(), solicitud.chatId());
        return switch (resultado) {
            case VINCULADO -> ResponseEntity.noContent().build();
            case CODIGO_INEXISTENTE -> ResponseEntity.notFound().build();
            case CODIGO_VENCIDO_O_USADO -> ResponseEntity.status(HttpStatus.GONE).build();
        };
    }

    @PostMapping(path = ContratoInterno.RUTA_BLOQUEOS, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> bloqueo(@RequestBody SolicitudBloqueo solicitud) {
        if (solicitud == null || solicitud.chatId() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        servicio.desvincularPorChat(solicitud.chatId());
        return ResponseEntity.noContent().build();
    }
}
