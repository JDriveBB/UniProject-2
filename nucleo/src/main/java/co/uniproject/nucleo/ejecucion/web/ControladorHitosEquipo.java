package co.uniproject.nucleo.ejecucion.web;

import co.uniproject.nucleo.ejecucion.servicio.RutaCriticaVista;
import co.uniproject.nucleo.ejecucion.servicio.ServicioDependencias;
import co.uniproject.nucleo.ejecucion.servicio.ServicioTareas;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudTarea;
import co.uniproject.nucleo.ejecucion.servicio.TableroHito;
import co.uniproject.nucleo.ejecucion.servicio.TareaVista;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** El "hito" de las rutas es id_hito_equipo (decisión R-7). */
@RestController
@RequestMapping("/api/hitos-equipo/{idHitoEquipo}")
public class ControladorHitosEquipo {

    private final ServicioTareas tareas;
    private final ServicioDependencias dependencias;

    public ControladorHitosEquipo(ServicioTareas tareas, ServicioDependencias dependencias) {
        this.tareas = tareas;
        this.dependencias = dependencias;
    }

    @GetMapping("/tareas")
    public TableroHito tablero(@AuthenticationPrincipal UsuarioAutenticado usuario,
                               @PathVariable("idHitoEquipo") long idHitoEquipo) {
        return tareas.tablero(usuario, idHitoEquipo);
    }

    @PostMapping("/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public TareaVista crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
                            @PathVariable("idHitoEquipo") long idHitoEquipo,
                            @Valid @RequestBody SolicitudTarea solicitud) {
        return tareas.crear(usuario, idHitoEquipo, solicitud);
    }

    @GetMapping("/ruta-critica")
    public RutaCriticaVista rutaCritica(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @PathVariable("idHitoEquipo") long idHitoEquipo) {
        return dependencias.rutaCritica(usuario, idHitoEquipo);
    }
}
