package co.uniproject.nucleo.ejecucion.web;

import co.uniproject.nucleo.ejecucion.servicio.MiTareaVista;
import co.uniproject.nucleo.ejecucion.servicio.ResultadoDependenciasVista;
import co.uniproject.nucleo.ejecucion.servicio.ServicioAvance;
import co.uniproject.nucleo.ejecucion.servicio.ServicioDependencias;
import co.uniproject.nucleo.ejecucion.servicio.ServicioTareas;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudAvance;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudDependencias;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudTarea;
import co.uniproject.nucleo.ejecucion.servicio.TareaDetalle;
import co.uniproject.nucleo.ejecucion.servicio.TareaVista;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tareas")
public class ControladorTareas {

    private final ServicioTareas tareas;
    private final ServicioDependencias dependencias;
    private final ServicioAvance avance;

    public ControladorTareas(ServicioTareas tareas, ServicioDependencias dependencias, ServicioAvance avance) {
        this.tareas = tareas;
        this.dependencias = dependencias;
        this.avance = avance;
    }

    @GetMapping("/mias")
    public List<MiTareaVista> mias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return tareas.mias(usuario);
    }

    @GetMapping("/{idTarea}")
    public TareaDetalle detalle(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                @PathVariable("idTarea") long idTarea) {
        return tareas.detalle(usuario, idTarea);
    }

    @PutMapping("/{idTarea}")
    public TareaVista editar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                             @PathVariable("idTarea") long idTarea,
                             @Valid @RequestBody SolicitudTarea solicitud) {
        return tareas.editar(usuario, idTarea, solicitud);
    }

    @PostMapping("/{idTarea}/dependencias")
    public ResultadoDependenciasVista agregarDependencias(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                          @PathVariable("idTarea") long idTarea,
                                                          @Valid @RequestBody SolicitudDependencias solicitud) {
        return dependencias.agregar(usuario, idTarea, solicitud);
    }

    @DeleteMapping("/{idTarea}/dependencias/{idPrerrequisito}")
    public TareaVista quitarDependencia(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @PathVariable("idTarea") long idTarea,
                                        @PathVariable("idPrerrequisito") long idPrerrequisito) {
        return dependencias.quitar(usuario, idTarea, idPrerrequisito);
    }

    @PutMapping("/{idTarea}/avance")
    public TareaVista actualizarAvance(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @PathVariable("idTarea") long idTarea,
                                       @Valid @RequestBody SolicitudAvance solicitud) {
        return avance.actualizar(usuario, idTarea, solicitud);
    }
}
