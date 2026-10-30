package co.uniproject.nucleo.notificaciones.web;

import co.uniproject.nucleo.notificaciones.bandeja.Bandeja;
import co.uniproject.nucleo.notificaciones.bandeja.ServicioBandeja;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Bandeja P10 del usuario de la sesión. */
@RestController
@RequestMapping("/api/notificaciones")
public class ControladorBandeja {

    private final ServicioBandeja servicio;

    public ControladorBandeja(ServicioBandeja servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Bandeja bandeja(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.bandeja(usuario.idUsuario());
    }

    @PostMapping("/{idNotificacion}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeida(@AuthenticationPrincipal UsuarioAutenticado usuario,
                            @PathVariable("idNotificacion") long idNotificacion) {
        servicio.marcarLeida(usuario.idUsuario(), idNotificacion);
    }

    @PostMapping("/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicio.marcarTodasLeidas(usuario.idUsuario());
    }
}
