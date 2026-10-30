package co.uniproject.nucleo.ejecucion.web;

import co.uniproject.nucleo.ejecucion.servicio.ArchivoEvidencia;
import co.uniproject.nucleo.ejecucion.servicio.EvidenciaVista;
import co.uniproject.nucleo.ejecucion.servicio.ServicioEvidencias;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudEnlace;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ControladorEvidencias {

    private final ServicioEvidencias evidencias;

    public ControladorEvidencias(ServicioEvidencias evidencias) {
        this.evidencias = evidencias;
    }

    @PostMapping(path = "/api/tareas/{idTarea}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaVista adjuntarArchivo(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                          @PathVariable("idTarea") long idTarea,
                                          @RequestPart("archivo") MultipartFile archivo) {
        return evidencias.adjuntarArchivo(usuario, idTarea, archivo);
    }

    @PostMapping(path = "/api/tareas/{idTarea}/evidencias", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaVista adjuntarEnlace(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @PathVariable("idTarea") long idTarea,
                                         @Valid @RequestBody SolicitudEnlace solicitud) {
        return evidencias.adjuntarEnlace(usuario, idTarea, solicitud);
    }

    @GetMapping("/api/tareas/{idTarea}/evidencias")
    public List<EvidenciaVista> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @PathVariable("idTarea") long idTarea) {
        return evidencias.listar(usuario, idTarea);
    }

    @GetMapping("/api/evidencias/{idEvidencia}/archivo")
    public ResponseEntity<Resource> archivo(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @PathVariable("idEvidencia") long idEvidencia) {
        ArchivoEvidencia a = evidencias.descargar(usuario, idEvidencia);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.mime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(a.ruta()));
    }
}
