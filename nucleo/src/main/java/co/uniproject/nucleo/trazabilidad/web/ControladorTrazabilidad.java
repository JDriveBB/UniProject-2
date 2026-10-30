package co.uniproject.nucleo.trazabilidad.web;

import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.trazabilidad.servicio.EntradaHistorial;
import co.uniproject.nucleo.trazabilidad.servicio.PanelProyecto;
import co.uniproject.nucleo.trazabilidad.servicio.ServicioHistorial;
import co.uniproject.nucleo.trazabilidad.servicio.ServicioPanel;
import co.uniproject.nucleo.trazabilidad.servicio.ServicioReporte;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorTrazabilidad {

    private final ServicioPanel panel;
    private final ServicioHistorial historial;
    private final ServicioReporte reporte;

    public ControladorTrazabilidad(ServicioPanel panel, ServicioHistorial historial, ServicioReporte reporte) {
        this.panel = panel;
        this.historial = historial;
        this.reporte = reporte;
    }

    @GetMapping("/api/proyectos/{idProyecto}/panel")
    public PanelProyecto panel(@AuthenticationPrincipal UsuarioAutenticado usuario,
                               @PathVariable("idProyecto") long idProyecto) {
        return panel.panel(usuario, idProyecto);
    }

    @GetMapping("/api/equipos/{idEquipo}/historial")
    public List<EntradaHistorial> historial(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @PathVariable("idEquipo") long idEquipo,
                                            @RequestParam(name = "limite", defaultValue = "200") int limite) {
        return historial.consultar(usuario, idEquipo, limite);
    }

    @GetMapping("/api/equipos/{idEquipo}/reporte.pdf")
    public ResponseEntity<byte[]> reporte(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                          @PathVariable("idEquipo") long idEquipo) {
        byte[] pdf = reporte.generar(usuario, idEquipo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("reporte-equipo-" + idEquipo + ".pdf").build()
                                .toString())
                .body(pdf);
    }
}
