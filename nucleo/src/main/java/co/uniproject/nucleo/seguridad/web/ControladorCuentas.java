package co.uniproject.nucleo.seguridad.web;

import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.cuentas.SolicitudRegistro;
import co.uniproject.nucleo.seguridad.cuentas.UsuarioSesion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorCuentas {

    private final ServicioCuentas cuentas;

    public ControladorCuentas(ServicioCuentas cuentas) {
        this.cuentas = cuentas;
    }

    /** Alta con correo habilitado. No inicia sesión: el SPA lleva a la pantalla de ingreso. */
    @PostMapping("/api/cuentas/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioSesion registrar(@Valid @RequestBody SolicitudRegistro solicitud) {
        return cuentas.registrar(solicitud);
    }
}
