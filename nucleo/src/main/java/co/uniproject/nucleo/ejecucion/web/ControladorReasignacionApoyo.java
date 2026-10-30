package co.uniproject.nucleo.ejecucion.web;

import co.uniproject.nucleo.ejecucion.servicio.ApoyoVista;
import co.uniproject.nucleo.ejecucion.servicio.CargaIntegrante;
import co.uniproject.nucleo.ejecucion.servicio.ResultadoReasignacion;
import co.uniproject.nucleo.ejecucion.servicio.ServicioApoyo;
import co.uniproject.nucleo.ejecucion.servicio.ServicioReasignacion;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudApoyo;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudReasignacion;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorReasignacionApoyo {

    private final ServicioReasignacion reasignacion;
    private final ServicioApoyo apoyo;

    public ControladorReasignacionApoyo(ServicioReasignacion reasignacion, ServicioApoyo apoyo) {
        this.reasignacion = reasignacion;
        this.apoyo = apoyo;
    }

    @PostMapping("/api/tareas/{idTarea}/reasignacion")
    public ResultadoReasignacion reasignar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @PathVariable("idTarea") long idTarea,
                                           @Valid @RequestBody SolicitudReasignacion solicitud) {
        return reasignacion.reasignar(usuario, idTarea, solicitud);
    }

    @PostMapping("/api/tareas/{idTarea}/apoyo")
    @ResponseStatus(HttpStatus.CREATED)
    public ApoyoVista solicitarApoyo(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @PathVariable("idTarea") long idTarea,
                                     @Valid @RequestBody SolicitudApoyo solicitud) {
        return apoyo.solicitar(usuario, idTarea, solicitud);
    }

    @GetMapping("/api/hitos-equipo/{idHitoEquipo}/apoyos")
    public List<ApoyoVista> apoyos(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                   @PathVariable("idHitoEquipo") long idHitoEquipo) {
        return apoyo.apoyos(usuario, idHitoEquipo);
    }

    @GetMapping("/api/hitos-equipo/{idHitoEquipo}/carga")
    public List<CargaIntegrante> carga(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @PathVariable("idHitoEquipo") long idHitoEquipo) {
        return apoyo.carga(usuario, idHitoEquipo);
    }
}
