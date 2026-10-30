package co.uniproject.nucleo.ejecucion.web;

import co.uniproject.nucleo.ejecucion.servicio.ComentarioVista;
import co.uniproject.nucleo.ejecucion.servicio.CorreccionVista;
import co.uniproject.nucleo.ejecucion.servicio.PeticionCorreccion;
import co.uniproject.nucleo.ejecucion.servicio.ServicioComentarios;
import co.uniproject.nucleo.ejecucion.servicio.ServicioCorrecciones;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudComentario;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tareas/{idTarea}")
public class ControladorComentariosCorrecciones {

    private final ServicioComentarios comentarios;
    private final ServicioCorrecciones correcciones;

    public ControladorComentariosCorrecciones(ServicioComentarios comentarios, ServicioCorrecciones correcciones) {
        this.comentarios = comentarios;
        this.correcciones = correcciones;
    }

    @GetMapping("/comentarios")
    public List<ComentarioVista> comentarios(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @PathVariable("idTarea") long idTarea) {
        return comentarios.listar(usuario, idTarea);
    }

    @PostMapping("/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioVista comentar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                    @PathVariable("idTarea") long idTarea,
                                    @Valid @RequestBody SolicitudComentario solicitud) {
        return comentarios.comentar(usuario, idTarea, solicitud);
    }

    @GetMapping("/correcciones")
    public List<CorreccionVista> correcciones(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                              @PathVariable("idTarea") long idTarea) {
        return correcciones.listar(usuario, idTarea);
    }

    @PostMapping("/correcciones")
    @ResponseStatus(HttpStatus.CREATED)
    public CorreccionVista solicitarCorreccion(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                               @PathVariable("idTarea") long idTarea,
                                               @Valid @RequestBody PeticionCorreccion peticion) {
        return correcciones.solicitar(usuario, idTarea, peticion);
    }
}
