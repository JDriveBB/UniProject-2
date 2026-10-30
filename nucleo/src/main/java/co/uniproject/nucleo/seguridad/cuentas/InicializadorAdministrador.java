package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Crea el primer administrador al arrancar (R-17). Nunca escribe en el log el correo ni la contraseña. */
@Component
public class InicializadorAdministrador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdministrador.class);

    private final ServicioCuentas cuentas;
    private final String email;
    private final String nombre;
    private final String contrasena;

    public InicializadorAdministrador(ServicioCuentas cuentas,
                                      @Value("${uniproject.admin.email:}") String email,
                                      @Value("${uniproject.admin.nombre:Administrador}") String nombre,
                                      @Value("${uniproject.admin.contrasena:}") String contrasena) {
        this.cuentas = cuentas;
        this.email = email;
        this.nombre = nombre;
        this.contrasena = contrasena;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || contrasena.isBlank()) {
            log.info("Administrador inicial no configurado (UNIPROJECT_ADMIN_EMAIL o UNIPROJECT_ADMIN_CONTRASENA vacíos)");
            return;
        }
        try {
            if (cuentas.crearAdministradorInicial(email, nombre, contrasena)) {
                log.info("Administrador inicial creado");
            } else {
                log.info("Ya hay un administrador o el correo configurado ya tiene cuenta: no se crea otro");
            }
        } catch (SolicitudInvalidaException e) {
            log.warn("UNIPROJECT_ADMIN_CONTRASENA debe tener al menos 10 caracteres y no más de 72 bytes: no se creó el administrador");
        }
    }
}
