package co.uniproject.nucleo.seguridad.web;

import co.uniproject.nucleo.seguridad.cuentas.LimitadorRecuperacion;
import co.uniproject.nucleo.seguridad.cuentas.LimitadorRecuperacion.Tipo;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.cuentas.SolicitudRegistro;
import co.uniproject.nucleo.seguridad.cuentas.UsuarioSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorCuentas {

    private final ServicioCuentas cuentas;
    private final LimitadorRecuperacion limitador;

    public ControladorCuentas(ServicioCuentas cuentas, LimitadorRecuperacion limitador) {
        this.cuentas = cuentas;
        this.limitador = limitador;
    }

    /**
     * Alta con correo habilitado. No inicia sesión: el SPA lleva a la pantalla de ingreso. Límite por IP: un alta
     * exitosa devuelve su cupo (en una sala con NAT todos se registran desde la misma IP); cuentan los fallidos.
     */
    @PostMapping("/api/cuentas/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioSesion registrar(@Valid @RequestBody SolicitudRegistro solicitud, HttpServletRequest peticion) {
        String ip = peticion.getRemoteAddr();
        limitador.exigir(Tipo.REGISTRO_IP, ip);
        UsuarioSesion creada = cuentas.registrar(solicitud);
        limitador.devolver(Tipo.REGISTRO_IP, ip);
        return creada;
    }
}
