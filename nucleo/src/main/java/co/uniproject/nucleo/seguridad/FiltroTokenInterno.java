package co.uniproject.nucleo.seguridad;

import co.uniproject.eventos.interno.ContratoInterno;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.springframework.web.filter.OncePerRequestFilter;

public class FiltroTokenInterno extends OncePerRequestFilter {

    private final String tokenEsperado;

    public FiltroTokenInterno(String tokenEsperado) {
        this.tokenEsperado = tokenEsperado;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibido = request.getHeader(ContratoInterno.CABECERA_TOKEN);
        if (tokenEsperado == null || tokenEsperado.isBlank() || recibido == null
                || !MessageDigest.isEqual(tokenEsperado.getBytes(UTF_8), recibido.getBytes(UTF_8))) {
            response.setStatus(401);
            return;
        }
        chain.doFilter(request, response);
    }
}
