package co.uniproject.nucleo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/** Decisión 23-2: el contrato OpenAPI y los @*Mapping de /api/** tienen exactamente las mismas operaciones. */
class ContratoOpenApiTest extends ApiIntegracionBase {

    /** Surefire corre con el directorio del módulo (nucleo/) como directorio de trabajo. */
    private static final Path CONTRATO = Path.of("..", "docs", "openapi", "openapi.yaml");
    private static final Pattern RUTA = Pattern.compile("^  (/api/\\S+):\\s*$");
    private static final Pattern METODO = Pattern.compile("^    (get|post|put|delete|patch):\\s*$");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeos;

    @Test
    void elContratoTieneExactamenteLasOperacionesDeLaApi() throws IOException {
        Set<String> codigo = operacionesDelCodigo();
        assertThat(codigo).hasSizeGreaterThanOrEqualTo(63);
        assertThat(operacionesDelContrato()).containsExactlyInAnyOrderElementsOf(codigo);
    }

    @Test
    void elContratoDescribeLosErroresYNoExponeLoInterno() throws IOException {
        String texto = Files.readString(CONTRATO);
        assertThat(texto).contains("    RespuestaError:").contains("CSRF_INVALIDO").doesNotContain("/interno/")
                .doesNotContain("telegramChatId").doesNotContain("passwordHash");
    }

    private Set<String> operacionesDelCodigo() {
        Set<String> operaciones = new TreeSet<>();
        mapeos.getHandlerMethods().keySet().forEach(info -> {
            if (info.getPathPatternsCondition() == null) {
                return;
            }
            for (String ruta : info.getPathPatternsCondition().getPatternValues()) {
                if (!ruta.startsWith("/api/")) {
                    continue;
                }
                for (RequestMethod metodo : info.getMethodsCondition().getMethods()) {
                    operaciones.add(metodo.name() + " " + ruta);
                }
            }
        });
        return operaciones;
    }

    private Set<String> operacionesDelContrato() throws IOException {
        Set<String> operaciones = new TreeSet<>();
        String ruta = null;
        for (String linea : Files.readAllLines(CONTRATO)) {
            if (linea.startsWith("components:")) {
                break;
            }
            Matcher r = RUTA.matcher(linea);
            if (r.matches()) {
                ruta = r.group(1);
                continue;
            }
            Matcher m = METODO.matcher(linea);
            if (m.matches() && ruta != null) {
                operaciones.add(m.group(1).toUpperCase(Locale.ROOT) + " " + ruta);
            }
        }
        return operaciones;
    }
}
