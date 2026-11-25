package co.uniproject.nucleo.vinculaciontelegram.web;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.vinculaciontelegram.CodigoGenerado;
import co.uniproject.nucleo.vinculaciontelegram.ServicioPerfilTelegram;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Botones "Vincular Telegram" y "Desvincular" del perfil (D5, D7, plan sección 8.8). */
@RestController
@RequestMapping("/api/perfil/telegram")
public class ControladorPerfilTelegram {

    private final ServicioPerfilTelegram servicio;

    public ControladorPerfilTelegram(ServicioPerfilTelegram servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/codigo")
    @ResponseStatus(HttpStatus.CREATED)
    public CodigoGenerado codigo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.generarCodigo(usuario.idUsuario());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincular(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicio.desvincular(usuario.idUsuario());
    }
}
