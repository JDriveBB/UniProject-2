package co.uniproject.nucleo.seguridad.web;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/** 403 de la cadena web en el formato RespuestaError. Los rechazos por rol se auditan; los de CSRF no. */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    static final String CUERPO_ACCESO_DENEGADO =
            "{\"codigo\":\"ACCESO_DENEGADO\",\"mensaje\":\"No tienes permiso para esta acción\",\"detalles\":[]}";
    static final String CUERPO_CSRF =
            "{\"codigo\":\"CSRF_INVALIDO\",\"mensaje\":\"La sesión cambió o expiró. Recarga la página e inténtalo de nuevo\",\"detalles\":[]}";

    private final RegistroAuditoria auditoria;

    public ManejadorAccesoDenegado(RegistroAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException e)
            throws IOException {
        boolean csrf = e instanceof CsrfException;
        if (!csrf) {
            Long idUsuario = UsuarioAutenticado.actual().map(UsuarioAutenticado::idUsuario).orElse(null);
            auditoria.registrar(EntradaAuditoria.accesoDenegado(idUsuario, peticion.getMethod(), peticion.getRequestURI()));
        }
        respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write(csrf ? CUERPO_CSRF : CUERPO_ACCESO_DENEGADO);
    }
}
