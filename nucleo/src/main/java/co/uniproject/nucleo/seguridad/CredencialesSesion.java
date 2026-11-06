package co.uniproject.nucleo.seguridad;

import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Versión de credenciales de la sesión (decisión 28-9). El login la guarda en la sesión; FiltroCuentaSuprimida la
 * compara con usuario.version_credenciales, que sube con cada cambio de contraseña.
 */
@Component
public class CredencialesSesion {

    public static final String ATRIBUTO = "uniproject.versionCredenciales";

    private final JdbcClient jdbc;

    public CredencialesSesion(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Se lee ANTES de autenticar: si la contraseña cambia en medio, la sesión nace vieja y cae en la siguiente petición. */
    public int versionPorCorreo(String email) {
        return jdbc.sql("SELECT version_credenciales FROM usuario WHERE email = :email")
                .param("email", ServicioCuentas.normalizarEmail(email))
                .query(Integer.class)
                .optional()
                .orElse(0);
    }

    /** Sesiones de antes del bloque 28 no tienen el atributo: cuentan como versión 0 (la de la columna por defecto). */
    public static int versionDe(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        Object valor = sesion == null ? null : sesion.getAttribute(ATRIBUTO);
        return valor instanceof Integer version ? version : 0;
    }
}
