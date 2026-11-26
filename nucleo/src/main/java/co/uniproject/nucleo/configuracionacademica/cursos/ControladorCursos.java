package co.uniproject.nucleo.configuracionacademica.cursos;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.EnlaceRegistroVista;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cursos académicos, sus estudiantes y sus habilitaciones de registro. Los permisos los revisa el servicio. */
@RestController
@RequestMapping("/api/cursos")
public class ControladorCursos {

    private final ServicioCursos cursos;

    public ControladorCursos(ServicioCursos cursos) {
        this.cursos = cursos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoVista crear(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody SolicitudCurso solicitud) {
        return cursos.crear(usuario, solicitud);
    }

    @GetMapping
    public List<CursoVista> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return cursos.listar(usuario);
    }

    @GetMapping("/{idCurso}")
    public CursoDetalle detalle(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idCurso") long idCurso) {
        return cursos.detalle(usuario, idCurso);
    }

    @PutMapping("/{idCurso}")
    public CursoVista editar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idCurso") long idCurso,
            @Valid @RequestBody SolicitudCurso solicitud) {
        return cursos.editar(usuario, idCurso, solicitud);
    }

    @DeleteMapping("/{idCurso}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idCurso") long idCurso) {
        cursos.borrar(usuario, idCurso);
    }

    @PutMapping("/{idCurso}/archivado")
    public CursoVista cambiarArchivado(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idCurso") long idCurso,
            @Valid @RequestBody SolicitudArchivado solicitud) {
        return cursos.cambiarArchivado(usuario, idCurso, solicitud);
    }

    @GetMapping("/{idCurso}/companeros")
    public List<CompaneroVista> companeros(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable("idCurso") long idCurso) {
        return cursos.companeros(usuario, idCurso);
    }

    @PostMapping("/{idCurso}/correos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoCorreosCurso agregarCorreos(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idCurso") long idCurso,
            @Valid @RequestBody SolicitudCorreosCurso solicitud) {
        return cursos.agregarCorreos(usuario, idCurso, solicitud);
    }

    @DeleteMapping("/{idCurso}/correos/{idCorreoHabilitado}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirarCorreo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idCurso") long idCurso,
            @PathVariable("idCorreoHabilitado") long idCorreoHabilitado) {
        cursos.retirarCorreo(usuario, idCurso, idCorreoHabilitado);
    }

    @PostMapping("/{idCurso}/correos/{idCorreoHabilitado}/enlace")
    public ResponseEntity<EnlaceRegistroVista> generarEnlace(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idCurso") long idCurso,
            @PathVariable("idCorreoHabilitado") long idCorreoHabilitado) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(cursos.generarEnlace(usuario, idCurso, idCorreoHabilitado));
    }

    @DeleteMapping("/{idCurso}/estudiantes/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarEstudiante(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable("idCurso") long idCurso,
            @PathVariable("idUsuario") long idUsuario) {
        cursos.quitarEstudiante(usuario, idCurso, idUsuario);
    }
}
