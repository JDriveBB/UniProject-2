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
 * Decisión 27-12: una sesión abierta de una cuenta ya anonimizada deja de servir en su siguiente petición (401 sin
 * cuerpo, como NoAutenticado). No es un bean: lo crea ConfiguracionSeguridad dentro de la cadena web, así no se
 * registra dos veces como filtro del servlet.
 */
public class FiltroCuentaSuprimida extends OncePerRequestFilter {

    private final JdbcClient jdbc;

    public FiltroCuentaSuprimida(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !peticion.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        Optional<UsuarioAutenticado> usuario = UsuarioAutenticado.actual();
        if (usuario.isPresent() && suprimida(usuario.get().idUsuario())) {
            HttpSession sesion = peticion.getSession(false);
            if (sesion != null) {
                sesion.invalidate();
            }
            SecurityContextHolder.clearContext();
            respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }

    private boolean suprimida(long idUsuario) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = :id "
                        + "AND fecha_anonimizacion IS NOT NULL)")
                .param("id", idUsuario)
                .query(Boolean.class)
                .single();
    }
}
