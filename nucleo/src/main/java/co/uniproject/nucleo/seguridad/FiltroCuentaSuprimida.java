package co.uniproject.nucleo.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Una sesión abierta deja de servir en su siguiente petición si la cuenta se anonimizó (decisión 27-12) o si la contraseña cambió después del login (decisión 28-9: la sesión guarda la versión
 * de credenciales y se compara con usuario.version_credenciales). Se invalida la sesión, se limpia el contexto y la
 * petición continúa como anónima: /api/** responde su 401 normal y las páginas de la SPA cargan. No es un bean: lo crea ConfiguracionSeguridad dentro
 * de la cadena web (se aplica a toda ruta con sesión, sea cual sea su codificación), así no se registra dos veces como filtro del servlet.
 */
public class FiltroCuentaSuprimida extends OncePerRequestFilter {

    private final JdbcClient jdbc;

    public FiltroCuentaSuprimida(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        Optional<UsuarioAutenticado> usuario = UsuarioAutenticado.actual();
        if (usuario.isPresent() && !vigente(usuario.get().idUsuario(), CredencialesSesion.versionDe(peticion))) {
            HttpSession sesion = peticion.getSession(false);
            if (sesion != null) {
                sesion.invalidate();
            }
            SecurityContextHolder.clearContext();
        }
        cadena.doFilter(peticion, respuesta);
    }

    private boolean vigente(long idUsuario, int version) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = :id "
                        + "AND fecha_anonimizacion IS NULL AND version_credenciales = :version)")
                .param("id", idUsuario)
                .param("version", version)
                .query(Boolean.class)
                .single();
    }
}
