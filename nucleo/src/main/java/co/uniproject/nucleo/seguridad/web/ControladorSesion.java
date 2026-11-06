package co.uniproject.nucleo.seguridad.web;

import co.uniproject.nucleo.comun.web.RespuestaError;
import co.uniproject.nucleo.seguridad.CredencialesSesion;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.LimitadorRecuperacion;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.cuentas.UsuarioSesion;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sesión del SPA. Tras login y logout el cliente vuelve a pedir GET /api/sesion/csrf (la cookie CSRF rota). */
@RestController
@RequestMapping("/api/sesion")
public class ControladorSesion {

    private final ServicioCuentas cuentas;
    private final SecurityContextRepository repositorioContexto;
    private final RegistroAuditoria auditoria;
    private final CredencialesSesion credenciales;
    private final LimitadorRecuperacion limitador;

    public ControladorSesion(ServicioCuentas cuentas, SecurityContextRepository repositorioContexto,
                             RegistroAuditoria auditoria, CredencialesSesion credenciales,
                             LimitadorRecuperacion limitador) {
        this.cuentas = cuentas;
        this.repositorioContexto = repositorioContexto;
        this.auditoria = auditoria;
        this.credenciales = credenciales;
        this.limitador = limitador;
    }

    /** Deja la cookie XSRF-TOKEN. El SPA la llama al cargar y después de login y logout. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody SolicitudLogin solicitud, HttpServletRequest peticion,
                                   HttpServletResponse respuesta) {
        String ip = peticion.getRemoteAddr();
        String email = ServicioCuentas.normalizarEmail(solicitud.email());
        limitador.exigir(LimitadorRecuperacion.Tipo.LOGIN_IP, ip);
        limitador.exigir(LimitadorRecuperacion.Tipo.LOGIN_CORREO, email);

        int version = credenciales.versionPorCorreo(solicitud.email());
        Optional<UsuarioAutenticado> autenticado = cuentas.autenticar(solicitud.email(), solicitud.contrasena());
        if (autenticado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new RespuestaError("CREDENCIALES_INVALIDAS",
                    "Correo o contraseña incorrectos", List.of()));
        }
        limitador.reiniciar(LimitadorRecuperacion.Tipo.LOGIN_CORREO, email);
        limitador.devolver(LimitadorRecuperacion.Tipo.LOGIN_IP, ip);
        UsuarioAutenticado usuario = autenticado.get();
        descartarSesion(peticion);
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name()))));
        SecurityContextHolder.setContext(contexto);
        repositorioContexto.saveContext(contexto, peticion, respuesta);
        peticion.getSession().setAttribute(CredencialesSesion.ATRIBUTO, version);
        borrarCookieCsrf(respuesta);
        return ResponseEntity.ok(cuentas.sesion(usuario.idUsuario()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest peticion, HttpServletResponse respuesta) {
        UsuarioAutenticado.actual().ifPresent(u -> auditoria.registrar(
                EntradaAuditoria.de(u.idUsuario(), "usuario", u.idUsuario(), AccionAuditoria.LOGOUT)));
        descartarSesion(peticion);
        SecurityContextHolder.clearContext();
        borrarCookieCsrf(respuesta);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/yo")
    public UsuarioSesion yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return cuentas.sesion(usuario.idUsuario());
    }

    /** Fijación de sesión: el identificador anterior deja de servir. */
    private static void descartarSesion(HttpServletRequest peticion) {
        HttpSession anterior = peticion.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
    }

    private static void borrarCookieCsrf(HttpServletResponse respuesta) {
        respuesta.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("XSRF-TOKEN", "").path("/").maxAge(0).build().toString());
    }
}
