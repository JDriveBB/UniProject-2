package co.uniproject.nucleo.comun.web;

import co.uniproject.nucleo.comun.error.AccesoDenegadoException;
import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errores de la API del dominio en un solo formato (RespuestaError). No aplica a /interno/**: el controlador interno
 * de Telegram está fuera de basePackages y conserva sus códigos. Los 403 quedan en log_auditoria (entrega1 §2.6).
 */
@RestControllerAdvice(basePackages = {
        "co.uniproject.nucleo.comun",
        "co.uniproject.nucleo.configuracionacademica",
        "co.uniproject.nucleo.ejecucion",
        "co.uniproject.nucleo.evaluacion",
        "co.uniproject.nucleo.notificaciones",
        "co.uniproject.nucleo.seguridad",
        "co.uniproject.nucleo.trazabilidad",
        "co.uniproject.nucleo.vinculaciontelegram.web"})
public class ManejadorErrores {

    private final RegistroAuditoria auditoria;

    public ManejadorErrores(RegistroAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<RespuestaError> accesoDenegado(AccesoDenegadoException e, HttpServletRequest peticion) {
        Long idUsuario = UsuarioAutenticado.actual().map(UsuarioAutenticado::idUsuario).orElse(null);
        auditoria.registrar(EntradaAuditoria.accesoDenegado(idUsuario, peticion.getMethod(), peticion.getRequestURI()));
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", e.getMessage(), List.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage(), List.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<RespuestaError> reglaNegocio(ReglaNegocioException e) {
        return respuesta(HttpStatus.CONFLICT, e.getCodigo(), e.getMessage(), e.getDetalles());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<RespuestaError> solicitudInvalida(SolicitudInvalidaException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getCodigo(), e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", "Datos inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> cuerpoIlegible(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO", "El cuerpo de la petición no es válido", List.of());
    }

    private static ResponseEntity<RespuestaError> respuesta(HttpStatus estado, String codigo, String mensaje,
                                                            List<String> detalles) {
        return ResponseEntity.status(estado).body(new RespuestaError(codigo, mensaje, detalles));
    }
}
