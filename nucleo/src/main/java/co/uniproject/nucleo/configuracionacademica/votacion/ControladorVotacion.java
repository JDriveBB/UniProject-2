package co.uniproject.nucleo.configuracionacademica.votacion;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipos/{idEquipo}/ronda")
public class ControladorVotacion {

    private final ServicioVotacion votacion;

    public ControladorVotacion(ServicioVotacion votacion) {
        this.votacion = votacion;
    }

    @GetMapping
    public VotacionVista vista(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idEquipo") long idEquipo) {
        return votacion.vista(usuario, idEquipo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VotacionVista abrir(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idEquipo") long idEquipo) {
        return votacion.abrir(usuario, idEquipo);
    }

    @PutMapping("/voto")
    public VotacionVista votar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idEquipo") long idEquipo,
            @Valid @RequestBody SolicitudVoto solicitud) {
        return votacion.votar(usuario, idEquipo, solicitud);
    }

    @PutMapping("/desempate")
    public VotacionVista desempatar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idEquipo") long idEquipo,
            @Valid @RequestBody SolicitudDesempate solicitud) {
        return votacion.desempatar(usuario, idEquipo, solicitud);
    }
}
