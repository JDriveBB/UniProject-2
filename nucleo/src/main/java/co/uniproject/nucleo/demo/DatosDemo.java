package co.uniproject.nucleo.demo;

import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** R-16: carga los datos demo al arrancar con el perfil "demo". Nunca escribe en el log correos ni la contraseña. */
@Component
@Profile("demo")
public class DatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemo.class);

    private final CargaDemo carga;
    private final String contrasena;

    public DatosDemo(CargaDemo carga, @Value("${uniproject.demo.contrasena:}") String contrasena) {
        this.carga = carga;
        this.contrasena = contrasena;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (contrasena == null || contrasena.isBlank()) {
            log.warn("Perfil demo activo pero UNIPROJECT_DEMO_CONTRASENA está vacía: no se cargan datos demo");
            return;
        }
        try {
            if (carga.cargar(contrasena)) {
                log.info("Datos demo cargados");
            } else {
                log.info("Los datos demo ya existían: no se cargan de nuevo");
            }
        } catch (ReglaNegocioException e) {
            log.error("Datos demo NO cargados (regla {}): se revirtió todo", e.getCodigo());
        } catch (SolicitudInvalidaException e) {
            log.error("Datos demo NO cargados (solicitud {}): se revirtió todo", e.getCodigo());
        } catch (RuntimeException e) {
            log.error("Datos demo NO cargados ({}): se revirtió todo", e.getClass().getSimpleName());
        }
    }
}
