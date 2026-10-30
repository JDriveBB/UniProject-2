package co.uniproject.nucleo.configuracionacademica.web;

import co.uniproject.nucleo.configuracionacademica.servicio.CriterioVista;
import co.uniproject.nucleo.configuracionacademica.servicio.HitoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioHitos;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudCriterio;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudHito;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Hito del proyecto (no confundir con /api/hitos-equipo/…, el hito de cada equipo, bloques 17 y 19). */
@RestController
@RequestMapping("/api/hitos/{idHito}")
public class ControladorHitos {

    private final ServicioHitos hitos;

    public ControladorHitos(ServicioHitos hitos) {
        this.hitos = hitos;
    }

    @PutMapping
    public HitoVista actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                @PathVariable("idHito") long idHito,
                                @Valid @RequestBody SolicitudHito solicitud) {
        return hitos.actualizar(usuario, idHito, solicitud);
    }

    @PostMapping("/criterios")
    @ResponseStatus(HttpStatus.CREATED)
    public CriterioVista crearCriterio(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @PathVariable("idHito") long idHito,
                                       @Valid @RequestBody SolicitudCriterio solicitud) {
        return hitos.crearCriterio(usuario, idHito, solicitud);
    }

    @PutMapping("/criterios/{idCriterio}")
    public CriterioVista actualizarCriterio(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @PathVariable("idHito") long idHito,
                                            @PathVariable("idCriterio") long idCriterio,
                                            @Valid @RequestBody SolicitudCriterio solicitud) {
        return hitos.actualizarCriterio(usuario, idHito, idCriterio, solicitud);
    }

    @DeleteMapping("/criterios/{idCriterio}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCriterio(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                 @PathVariable("idHito") long idHito,
                                 @PathVariable("idCriterio") long idCriterio) {
        hitos.eliminarCriterio(usuario, idHito, idCriterio);
    }
}
