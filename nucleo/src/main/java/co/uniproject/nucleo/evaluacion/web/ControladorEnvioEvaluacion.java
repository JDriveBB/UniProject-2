package co.uniproject.nucleo.evaluacion.web;

import co.uniproject.nucleo.evaluacion.servicio.CambioNotaGrupal;
import co.uniproject.nucleo.evaluacion.servicio.ConfirmacionNota;
import co.uniproject.nucleo.evaluacion.servicio.EnvioVista;
import co.uniproject.nucleo.evaluacion.servicio.EvaluacionVista;
import co.uniproject.nucleo.evaluacion.servicio.ServicioEnvio;
import co.uniproject.nucleo.evaluacion.servicio.ServicioEvaluacion;
import co.uniproject.nucleo.evaluacion.servicio.SolicitudEvaluacion;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorEnvioEvaluacion {

    private final ServicioEnvio envio;
    private final ServicioEvaluacion evaluacion;

    public ControladorEnvioEvaluacion(ServicioEnvio envio, ServicioEvaluacion evaluacion) {
        this.envio = envio;
        this.evaluacion = evaluacion;
    }

    @PostMapping("/api/hitos-equipo/{idHitoEquipo}/envio")
    @ResponseStatus(HttpStatus.CREATED)
    public EnvioVista enviar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                             @PathVariable("idHitoEquipo") long idHitoEquipo) {
        return envio.enviar(usuario, idHitoEquipo);
    }

    @PostMapping("/api/hitos-equipo/{idHitoEquipo}/evaluacion")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluacionVista evaluar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                   @PathVariable("idHitoEquipo") long idHitoEquipo,
                                   @Valid @RequestBody SolicitudEvaluacion solicitud) {
        return evaluacion.evaluar(usuario, idHitoEquipo, solicitud);
    }

    @GetMapping("/api/hitos-equipo/{idHitoEquipo}/evaluacion")
    public EvaluacionVista consultar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @PathVariable("idHitoEquipo") long idHitoEquipo) {
        return evaluacion.consultar(usuario, idHitoEquipo);
    }

    @PutMapping("/api/evaluaciones/{idEvaluacion}/nota-grupal")
    public EvaluacionVista cambiarNotaGrupal(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @PathVariable("idEvaluacion") long idEvaluacion,
                                             @Valid @RequestBody CambioNotaGrupal cambio) {
        return evaluacion.cambiarNotaGrupal(usuario, idEvaluacion, cambio);
    }

    @PutMapping("/api/evaluaciones/{idEvaluacion}/notas/{idEstudiante}")
    public EvaluacionVista confirmarNota(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @PathVariable("idEvaluacion") long idEvaluacion,
                                         @PathVariable("idEstudiante") long idEstudiante,
                                         @Valid @RequestBody ConfirmacionNota confirmacion) {
        return evaluacion.confirmarNota(usuario, idEvaluacion, idEstudiante, confirmacion);
    }
}
