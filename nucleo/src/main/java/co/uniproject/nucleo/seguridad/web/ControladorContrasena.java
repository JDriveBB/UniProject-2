package co.uniproject.nucleo.seguridad.web;

import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.seguridad.CredencialesSesion;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.LimitadorRecuperacion;
import co.uniproject.nucleo.seguridad.cuentas.ServicioContrasena;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bloque 28: olvidé mi contraseña (sin sesión, por Telegram) y cambio con sesión. El límite se cuenta antes del
 * servicio y en su propia transacción. La IP es getRemoteAddr(): X-Forwarded-For no se usa (decisión 28-6).
 */
@RestController
public class ControladorContrasena {

    private final ServicioContrasena contrasenas;
    private final LimitadorRecuperacion limitador;

    public ControladorContrasena(ServicioContrasena contrasenas, LimitadorRecuperacion limitador) {
        this.contrasenas = contrasenas;
        this.limitador = limitador;
    }

    /** Siempre 202 sin cuerpo si pasa los límites, exista o no la cuenta (decisión 28-5). */
    @PostMapping("/api/cuentas/recuperacion")
    public ResponseEntity<Void> solicitar(@Valid @RequestBody SolicitudRecuperacion solicitud,
                                          HttpServletRequest peticion) {
        limitador.exigir(LimitadorRecuperacion.Tipo.IP, peticion.getRemoteAddr());
        limitador.exigir(LimitadorRecuperacion.Tipo.CORREO, ServicioCuentas.normalizarEmail(solicitud.email()));
        contrasenas.solicitarRecuperacion(solicitud.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/cuentas/recuperacion/restablecer")
    public ResponseEntity<Void> restablecer(@Valid @RequestBody SolicitudRestablecer solicitud,
                                            HttpServletRequest peticion) {
        limitador.exigir(LimitadorRecuperacion.Tipo.CANJE, peticion.getRemoteAddr());
        if (!contrasenas.restablecer(solicitud.codigo(), solicitud.contrasenaNueva())) {
            throw new SolicitudInvalidaException("CODIGO_INVALIDO", "El enlace no es válido o ya venció. Pide uno nuevo");
        }
        return ResponseEntity.noContent().build();
    }

    /** Esta sesión sigue con un id nuevo y la versión nueva; las demás caen en FiltroCuentaSuprimida (28-9). */
    @PostMapping("/api/perfil/contrasena")
    public ResponseEntity<Void> cambiar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @Valid @RequestBody SolicitudCambioContrasena solicitud,
                                        HttpServletRequest peticion) {
        limitador.exigir(LimitadorRecuperacion.Tipo.CAMBIO, "usuario:" + usuario.idUsuario());
        int version = contrasenas.cambiar(usuario.idUsuario(), solicitud.contrasenaActual(),
                solicitud.contrasenaNueva());
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            try {
                peticion.changeSessionId();
                sesion.setAttribute(CredencialesSesion.ATRIBUTO, version);
            } catch (IllegalStateException e) {
                // Otra petición de esta misma sesión la invalidó en medio: el usuario vuelve a iniciar sesión.
            }
        }
        return ResponseEntity.noContent().build();
    }
}
