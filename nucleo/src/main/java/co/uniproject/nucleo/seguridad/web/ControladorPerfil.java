package co.uniproject.nucleo.seguridad.web;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.DatosPersonales;
import co.uniproject.nucleo.seguridad.cuentas.LimitadorRecuperacion;
import co.uniproject.nucleo.seguridad.cuentas.ServicioPerfil;
import co.uniproject.nucleo.seguridad.cuentas.ServicioSupresion;
import co.uniproject.nucleo.seguridad.cuentas.UsuarioSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Derechos del titular desde el perfil (Ley 1581, bloque 27). Sin {id} en la ruta: todo actúa sobre la cuenta de la
 * sesión, así que no hay recurso ajeno que autorizar.
 */
@RestController
@RequestMapping("/api/perfil")
public class ControladorPerfil {

    private final ServicioPerfil perfil;
    private final ServicioSupresion supresion;
    private final LimitadorRecuperacion limitador;

    public ControladorPerfil(ServicioPerfil perfil, ServicioSupresion supresion, LimitadorRecuperacion limitador) {
        this.perfil = perfil;
        this.supresion = supresion;
        this.limitador = limitador;
    }

    @GetMapping("/datos")
    public DatosPersonales datos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return perfil.datos(usuario.idUsuario());
    }

    @PutMapping("/nombre")
    public UsuarioSesion cambiarNombre(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @Valid @RequestBody SolicitudNombre solicitud) {
        return perfil.cambiarNombre(usuario.idUsuario(), solicitud.nombre());
    }

    /** Anonimiza la cuenta y cierra esta sesión como el logout; las demás caen en FiltroCuentaSuprimida. */
    @PostMapping("/supresion")
    public ResponseEntity<Void> suprimir(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @Valid @RequestBody SolicitudSupresion solicitud,
                                         HttpServletRequest peticion, HttpServletResponse respuesta) {
        limitador.exigir(LimitadorRecuperacion.Tipo.CAMBIO, "usuario:" + usuario.idUsuario());
        supresion.suprimir(usuario.idUsuario(), solicitud.contrasena());
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        SecurityContextHolder.clearContext();
        respuesta.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("XSRF-TOKEN", "").path("/").maxAge(0).build().toString());
        return ResponseEntity.noContent().build();
    }
}
