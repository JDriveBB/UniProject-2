package co.uniproject.nucleo.seguridad;

import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Principal de la sesión web (lo deja el login del bloque 14b): sin hash de contraseña ni chat de Telegram.
 * Los controladores lo reciben con @AuthenticationPrincipal.
 */
public record UsuarioAutenticado(long idUsuario, String nombre, String email, RolPlataforma rol) implements Serializable {

    /** El usuario de la sesión actual, si hay uno. */
    public static Optional<UsuarioAutenticado> actual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof UsuarioAutenticado u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }
}
