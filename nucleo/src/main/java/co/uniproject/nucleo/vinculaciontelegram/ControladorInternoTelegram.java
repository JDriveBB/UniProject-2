package co.uniproject.nucleo.vinculaciontelegram;

import co.uniproject.eventos.interno.ContratoInterno;
import co.uniproject.eventos.interno.SolicitudBloqueo;
import co.uniproject.eventos.interno.SolicitudVinculacion;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorInternoTelegram {

    private static final Logger log = LoggerFactory.getLogger(ControladorInternoTelegram.class);
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

    /**
     * Dos confirmaciones simultáneas del mismo chat (decisiones-tecnicas-java §12): la segunda choca con
     * UNIQUE (usuario.telegram_chat_id) y su transacción se deshace, así que el código sigue sin usar.
     * Conflicto transitorio: 409 sin cuerpo y una línea de log sin datos (ni chat, ni código, ni Detail).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Void> conflicto(DataIntegrityViolationException e) {
        log.warn("Conflicto de integridad en la vinculación de Telegram, responde 409 ({})", e.getClass().getName());
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
