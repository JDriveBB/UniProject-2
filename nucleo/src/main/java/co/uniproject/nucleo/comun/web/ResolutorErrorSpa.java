package co.uniproject.nucleo.comun.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

/**
 * Bloque 43: una ruta desconocida que no es de la API responde 404 pero con index.html, para que React muestre su
 * página "No encontrada" en vez de la página "Whitelabel". Solo actúa cuando el navegador pide HTML (gancho de Spring
 * Boot para las vistas de error HTML); no refleja nada de la URL en la respuesta.
 */
@Component
public class ResolutorErrorSpa implements ErrorViewResolver {

    private static final List<String> PREFIJOS_EXCLUIDOS = List.of("/api", "/interno", "/actuator");

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        if (status != HttpStatus.NOT_FOUND) {
            return null;
        }
        String metodo = request.getMethod();
        if (!"GET".equals(metodo) && !"HEAD".equals(metodo)) {
            return null;
        }
        Object original = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (!(original instanceof String ruta) || esDeServidor(ruta) || tienePuntoElUltimoSegmento(ruta)) {
            return null;
        }
        return new ModelAndView("forward:/index.html", Map.of(), HttpStatus.NOT_FOUND);
    }

    private static boolean esDeServidor(String ruta) {
        for (String prefijo : PREFIJOS_EXCLUIDOS) {
            if (ruta.equals(prefijo) || ruta.startsWith(prefijo + "/")) {
                return true;
            }
        }
        return false;
    }

    private static boolean tienePuntoElUltimoSegmento(String ruta) {
        return ruta.substring(ruta.lastIndexOf('/') + 1).contains(".");
    }
}
