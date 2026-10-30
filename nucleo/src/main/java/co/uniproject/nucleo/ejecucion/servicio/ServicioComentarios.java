package co.uniproject.nucleo.ejecucion.servicio;

import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.ejecucion.dominio.ComentarioTarea;
import co.uniproject.nucleo.ejecucion.repositorio.ComentarioTareaRepositorio;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF14: retroalimentación en las tareas en curso, sin tocar su estado (decisión 17b-8). */
@Service
public class ServicioComentarios {

    private static final String VISTA = "SELECT c.id_comentario, c.id_tarea, c.id_autor, u.nombre AS autor, "
            + "u.rol_plataforma, c.contenido, c.fecha_hora FROM comentario_tarea c "
            + "JOIN usuario u ON u.id_usuario = c.id_autor ";

    private final ComentarioTareaRepositorio comentarios;
    private final ConsultasEjecucion consultas;
    private final Permisos permisos;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public ServicioComentarios(ComentarioTareaRepositorio comentarios, ConsultasEjecucion consultas,
                               Permisos permisos, JdbcClient jdbc, Clock reloj) {
        this.comentarios = comentarios;
        this.consultas = consultas;
        this.permisos = permisos;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<ComentarioVista> listar(UsuarioAutenticado usuario, long idTarea) {
        permisos.exigir(permisos.puedeVerTarea(usuario.idUsuario(), idTarea));
        return jdbc.sql(VISTA + "WHERE c.id_tarea = :tarea ORDER BY c.fecha_hora, c.id_comentario")
                .param("tarea", idTarea)
                .query(ServicioComentarios::fila)
                .list();
    }

    @Transactional
    public ComentarioVista comentar(UsuarioAutenticado usuario, long idTarea, SolicitudComentario solicitud) {
        permisos.exigir(permisos.puedeComentarTarea(usuario.idUsuario(), idTarea));
        ContextoHito hito = consultas.hitoDeTarea(idTarea).orElseThrow();
        Instant ahora = Instant.now(reloj);
        if (!EdicionHito.abierta(hito, ahora)) {
            throw new ReglaNegocioException("TAREA_NO_EN_CURSO", "Solo se comentan tareas de hitos en curso");
        }
        ComentarioTarea c = new ComentarioTarea();
        c.setIdTarea(idTarea);
        c.setIdAutor(usuario.idUsuario());
        c.setContenido(solicitud.contenido().strip());
        c.setFechaHora(ahora);
        c = comentarios.saveAndFlush(c);
        return jdbc.sql(VISTA + "WHERE c.id_comentario = :id")
                .param("id", c.getIdComentario())
                .query(ServicioComentarios::fila)
                .single();
    }

    private static ComentarioVista fila(java.sql.ResultSet rs, int fila) throws java.sql.SQLException {
        return new ComentarioVista(rs.getLong("id_comentario"), rs.getLong("id_tarea"), rs.getLong("id_autor"),
                rs.getString("autor"), RolPlataforma.valueOf(rs.getString("rol_plataforma")),
                rs.getString("contenido"), LectorTareas.instante(rs, "fecha_hora"));
    }
}
