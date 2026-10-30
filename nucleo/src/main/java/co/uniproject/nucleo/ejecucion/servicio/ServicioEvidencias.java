package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.comun.error.SolicitudInvalidaException;
import co.uniproject.nucleo.ejecucion.dominio.Evidencia;
import co.uniproject.nucleo.ejecucion.dominio.TipoEvidencia;
import co.uniproject.nucleo.ejecucion.repositorio.EvidenciaRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** RF12 (evidencias), RF13 (encargado o líder), restricción 5 y R-14. */
@Service
public class ServicioEvidencias {

    static final long MAXIMO_BYTES = 10L * 1024 * 1024;

    private static final String VISTA = "SELECT ev.id_evidencia, ev.id_tarea, ev.id_usuario, u.nombre AS autor, "
            + "ev.tipo_evidencia, ev.url, ev.nombre_archivo, ev.fecha_subida FROM evidencia ev "
            + "JOIN usuario u ON u.id_usuario = ev.id_usuario ";

    private final EvidenciaRepositorio evidencias;
    private final ConsultasEjecucion consultas;
    private final AlmacenEvidencias almacen;
    private final Permisos permisos;
    private final RegistroAuditoria auditoria;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioEvidencias(EvidenciaRepositorio evidencias, ConsultasEjecucion consultas, AlmacenEvidencias almacen,
                              Permisos permisos, RegistroAuditoria auditoria, JdbcClient jdbc, Clock reloj) {
        this.evidencias = evidencias;
        this.consultas = consultas;
        this.almacen = almacen;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional
    public EvidenciaVista adjuntarArchivo(UsuarioAutenticado usuario, long idTarea, MultipartFile archivo) {
        preparar(usuario, idTarea);
        if (archivo == null || archivo.isEmpty()) {
            throw new SolicitudInvalidaException("ARCHIVO_VACIO", "Adjunta un archivo PDF, PNG o JPEG");
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw demasiadoGrande();
        }
        byte[] contenido = leer(archivo);
        TipoArchivo tipo = TipoArchivo.detectar(contenido).orElseThrow(() -> new SolicitudInvalidaException(
                "TIPO_NO_PERMITIDO", "Solo se aceptan archivos PDF, PNG o JPEG"));
        String clave = almacen.guardar(contenido, tipo.extension());
        return registrar(usuario, idTarea, tipo.tipoEvidencia(), clave,
                TipoArchivo.nombreSeguro(archivo.getOriginalFilename(), tipo.extension()));
    }

    @Transactional
    public EvidenciaVista adjuntarEnlace(UsuarioAutenticado usuario, long idTarea, SolicitudEnlace solicitud) {
        preparar(usuario, idTarea);
        validarEnlace(solicitud.url());
        return registrar(usuario, idTarea, TipoEvidencia.ENLACE, solicitud.url(), null);
    }

    @Transactional(readOnly = true)
    public List<EvidenciaVista> listar(UsuarioAutenticado usuario, long idTarea) {
        permisos.exigir(permisos.puedeVerTarea(usuario.idUsuario(), idTarea));
        return jdbc.sql(VISTA + "WHERE ev.id_tarea = :tarea ORDER BY ev.fecha_subida DESC, ev.id_evidencia DESC")
                .param("tarea", idTarea)
                .query(ServicioEvidencias::fila)
                .list();
    }

    /** Decisión 17b-6. Id inexistente o tarea ajena → 403; enlace o archivo perdido → 404. */
    @Transactional(readOnly = true)
    public ArchivoEvidencia descargar(UsuarioAutenticado usuario, long idEvidencia) {
        Optional<Evidencia> evidencia = evidencias.findById(idEvidencia);
        permisos.exigir(evidencia.isPresent()
                && permisos.puedeVerTarea(usuario.idUsuario(), evidencia.get().getIdTarea()));
        Evidencia e = evidencia.get();
        if (e.getTipoEvidencia() == TipoEvidencia.ENLACE) {
            throw new RecursoNoEncontradoException("Esta evidencia es un enlace, no un archivo");
        }
        Path ruta = almacen.ruta(e.getUrl());
        if (!Files.isRegularFile(ruta)) {
            throw new RecursoNoEncontradoException("El archivo de la evidencia no está disponible");
        }
        return new ArchivoEvidencia(ruta, TipoArchivo.deClave(e.getUrl()).mime(), e.getNombreArchivo());
    }

    private void preparar(UsuarioAutenticado usuario, long idTarea) {
        permisos.exigir(permisos.puedeAdjuntarEvidencia(usuario.idUsuario(), idTarea));
        ContextoHito hito = consultas.hitoDeTarea(idTarea).orElseThrow();
        EdicionHito.exigirAbierta(hito, Instant.now(reloj));
    }

    /** Guarda la fila, atiende la corrección pendiente (17b-10) y audita (17b-11). */
    private EvidenciaVista registrar(UsuarioAutenticado usuario, long idTarea, TipoEvidencia tipo, String url,
                                     String nombreArchivo) {
        Instant ahora = Instant.now(reloj);
        Evidencia e = new Evidencia();
        e.setIdTarea(idTarea);
        e.setIdUsuario(usuario.idUsuario());
        e.setTipoEvidencia(tipo);
        e.setUrl(url);
        e.setNombreArchivo(nombreArchivo);
        e.setFechaSubida(ahora);
        e = evidencias.saveAndFlush(e);
        auditoria.registrar(EntradaAuditoria.cambio(usuario.idUsuario(), "tarea", idTarea,
                AccionAuditoria.ADJUNTAR_EVIDENCIA, "evidencia", null, tipo.name() + " " + e.getIdEvidencia()));
        int atendidas = jdbc.sql("UPDATE solicitud_correccion SET estado = 'ATENDIDA', fecha_atencion = :ahora "
                        + "WHERE id_tarea = :tarea AND estado = 'PENDIENTE'")
                .param("ahora", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .param("tarea", idTarea)
                .update();
        if (atendidas > 0) {
            auditoria.registrar(EntradaAuditoria.cambio(usuario.idUsuario(), "tarea", idTarea,
                    AccionAuditoria.ACTUALIZAR, "solicitud_correccion", "PENDIENTE", "ATENDIDA"));
        }
        return jdbc.sql(VISTA + "WHERE ev.id_evidencia = :id")
                .param("id", e.getIdEvidencia())
                .query(ServicioEvidencias::fila)
                .single();
    }

    /** Decisión 17b-5: http(s) en minúscula (como el CHECK de V3) y con host. */
    static void validarEnlace(String url) {
        boolean valido;
        try {
            URI uri = new URI(url);
            valido = (url.startsWith("http://") || url.startsWith("https://")) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            valido = false;
        }
        if (!valido) {
            throw new SolicitudInvalidaException("ENLACE_INVALIDO", "El enlace debe empezar por http:// o https://");
        }
    }

    static SolicitudInvalidaException demasiadoGrande() {
        return new SolicitudInvalidaException("ARCHIVO_DEMASIADO_GRANDE",
                "El archivo supera 10 MB. Súbelo a un repositorio y adjunta el enlace");
    }

    private static byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
    }

    private static EvidenciaVista fila(java.sql.ResultSet rs, int fila) throws java.sql.SQLException {
        return EvidenciaVista.de(rs.getLong("id_evidencia"), rs.getLong("id_tarea"), rs.getLong("id_usuario"),
                rs.getString("autor"), TipoEvidencia.valueOf(rs.getString("tipo_evidencia")), rs.getString("url"),
                rs.getString("nombre_archivo"), LectorTareas.instante(rs, "fecha_subida"));
    }
}
