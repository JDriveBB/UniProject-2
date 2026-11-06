package co.uniproject.nucleo.vinculaciontelegram;

import co.uniproject.eventos.interno.CodigoRecuperacionEmitido;
import co.uniproject.eventos.interno.ContratoInterno;
import co.uniproject.eventos.interno.SolicitudCodigoRecuperacion;
import co.uniproject.nucleo.seguridad.cuentas.ServicioContrasena;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Decisión 28-2: el agente pide el código justo antes de enviarlo. Protegido por FiltroTokenInterno (/interno/**). */
@RestController
public class ControladorInternoRecuperacion {

    private final ServicioContrasena servicio;

    public ControladorInternoRecuperacion(ServicioContrasena servicio) {
        this.servicio = servicio;
    }

    @PostMapping(path = ContratoInterno.RUTA_RECUPERACIONES, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CodigoRecuperacionEmitido> emitir(@RequestBody SolicitudCodigoRecuperacion solicitud) {
        if (solicitud == null || solicitud.solicitud() == null || solicitud.chatId() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        ServicioContrasena.EmisionCodigo emision = servicio.emitirCodigo(solicitud.solicitud(), solicitud.chatId());
        return switch (emision.resultado()) {
            case EMITIDO -> ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(new CodigoRecuperacionEmitido(emision.codigo()));
            case INEXISTENTE -> ResponseEntity.notFound().build();
            case NO_VIGENTE -> ResponseEntity.status(HttpStatus.GONE).build();
        };
    }
}
