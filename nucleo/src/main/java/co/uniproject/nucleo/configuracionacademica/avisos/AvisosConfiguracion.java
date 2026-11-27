package co.uniproject.nucleo.configuracionacademica.avisos;

import co.uniproject.eventos.DestinatariosEvento;
import co.uniproject.eventos.datos.CursoEstudianteAgregado;
import co.uniproject.eventos.datos.DatosEvento;
import co.uniproject.eventos.datos.EquipoLiderElegido;
import co.uniproject.eventos.datos.EquipoMiembroCambio;
import co.uniproject.eventos.datos.EquipoSinLider;
import co.uniproject.eventos.datos.EquipoVotacionAbierta;
import co.uniproject.eventos.datos.InscripcionPorCerrar;
import co.uniproject.eventos.datos.InscripcionResumen;
import co.uniproject.eventos.datos.MotivoSinLider;
import co.uniproject.eventos.datos.MovimientoEquipo;
import co.uniproject.eventos.datos.OrigenLider;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.nucleo.notificaciones.RegistroEventos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Constructor único de los siete eventos de cursos, equipos y votación (bloque 50).
 *
 * <p>Reglas: payload mínimo (Ley 1581: solo ids, nombres y chatId de quien recibe, sin correos, notas ni votos), toda
 * lista excluye cuentas anonimizadas y si no hay destinatarios no se registra evento. Registra en la transacción del
 * llamador sin tomar candados adicionales.
 */
@Component
public class AvisosConfiguracion {

    private final JdbcClient jdbc;
    private final RegistroEventos eventos;

    public AvisosConfiguracion(JdbcClient jdbc, RegistroEventos eventos) {
        this.jdbc = jdbc;
        this.eventos = eventos;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void estudianteAgregadoACurso(long idCurso, long idEstudiante) {
        record DatosCurso(String nombre, String periodo, long idDocente, String nombreDocente) {}
        DatosCurso curso = jdbc.sql("SELECT c.nombre, c.periodo, c.id_docente, d.nombre AS nombre_docente "
                        + "FROM curso c JOIN usuario d ON d.id_usuario = c.id_docente WHERE c.id_curso = :c")
                .param("c", idCurso)
                .query((rs, fila) -> new DatosCurso(
                        rs.getString("nombre"),
                        rs.getString("periodo"),
                        rs.getLong("id_docente"),
                        rs.getString("nombre_docente")))
                .single();
        registrarSiHayDestinatarios(new CursoEstudianteAgregado(
                new Referencia(idCurso, curso.nombre()),
                curso.periodo(),
                new Referencia(curso.idDocente(), curso.nombreDocente()),
                persona(idEstudiante)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void votacionAbierta(long idEquipo, Instant cierraEn) {
        List<Persona> lista = integrantes(idEquipo);
        if (lista.isEmpty()) {
            return;
        }
        ContextoEquipo ctx = contextoEquipo(idEquipo);
        registrarSiHayDestinatarios(new EquipoVotacionAbierta(ctx.proyecto(), ctx.equipo(), cierraEn, lista));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void liderElegido(long idEquipo, long idLider, OrigenLider origen) {
        List<Persona> lista = integrantes(idEquipo);
        if (lista.isEmpty()) {
            return;
        }
        ContextoEquipo ctx = contextoEquipo(idEquipo);
        Persona lider = persona(idLider);
        registrarSiHayDestinatarios(new EquipoLiderElegido(
                ctx.proyecto(),
                ctx.equipo(),
                new Referencia(idLider, lider.nombre()),
                origen,
                lista,
                persona(ctx.idDocente())));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void sinLider(long idEquipo, MotivoSinLider motivo) {
        ContextoEquipo ctx = contextoEquipo(idEquipo);
        registrarSiHayDestinatarios(new EquipoSinLider(ctx.proyecto(), ctx.equipo(), motivo, persona(ctx.idDocente())));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void miembroCambio(long idEquipo, MovimientoEquipo movimiento, boolean porDocente, List<Long> idsPersonas) {
        if (idsPersonas == null || idsPersonas.isEmpty()) {
            return;
        }
        List<Persona> personas = jdbc.sql("SELECT id_usuario, nombre, telegram_chat_id FROM usuario "
                        + "WHERE id_usuario IN (:ids) AND fecha_anonimizacion IS NULL "
                        + "ORDER BY nombre, id_usuario")
                .param("ids", idsPersonas)
                .query(AvisosConfiguracion::mapearPersona)
                .list();
        if (personas.isEmpty()) {
            return;
        }
        Set<Long> excluidos = Set.copyOf(idsPersonas);
        List<Persona> restantes = integrantes(idEquipo).stream()
                .filter(p -> !excluidos.contains(p.id()))
                .toList();
        ContextoEquipo ctx = contextoEquipo(idEquipo);
        registrarSiHayDestinatarios(
                new EquipoMiembroCambio(ctx.proyecto(), ctx.equipo(), movimiento, porDocente, personas, restantes));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cierreProximo(long idProyecto) {
        record DatosProyecto(String nombre, long idDocente, Instant cierre) {}
        DatosProyecto proyecto = jdbc.sql(
                        "SELECT nombre, id_docente, cierre_equipos FROM proyecto WHERE id_proyecto = :p")
                .param("p", idProyecto)
                .query((rs, fila) -> new DatosProyecto(
                        rs.getString("nombre"),
                        rs.getLong("id_docente"),
                        rs.getObject("cierre_equipos", OffsetDateTime.class).toInstant()))
                .single();
        Referencia refProyecto = new Referencia(idProyecto, proyecto.nombre());
        List<Persona> sinEquipo = jdbc.sql("SELECT u.id_usuario, u.nombre, u.telegram_chat_id "
                        + "FROM participante_proyecto pp JOIN usuario u ON u.id_usuario = pp.id_usuario "
                        + "WHERE pp.id_proyecto = :p AND u.fecha_anonimizacion IS NULL "
                        + "AND NOT EXISTS (SELECT 1 FROM miembro_equipo m JOIN equipo e "
                        + "ON e.id_equipo = m.id_equipo "
                        + "WHERE e.id_proyecto = pp.id_proyecto AND m.id_usuario = pp.id_usuario) "
                        + "ORDER BY u.nombre, u.id_usuario")
                .param("p", idProyecto)
                .query(AvisosConfiguracion::mapearPersona)
                .list();
        if (!sinEquipo.isEmpty()) {
            registrarSiHayDestinatarios(new InscripcionPorCerrar(refProyecto, proyecto.cierre(), sinEquipo));
        }
        int cuposLibres = jdbc.sql("SELECT CAST(coalesce(sum(greatest(e.cupo - "
                        + "(SELECT count(*) FROM miembro_equipo m WHERE m.id_equipo = e.id_equipo), 0)), 0) "
                        + "AS integer) "
                        + "FROM equipo e WHERE e.id_proyecto = :p AND e.cupo IS NOT NULL")
                .param("p", idProyecto)
                .query(Integer.class)
                .single();
        registrarSiHayDestinatarios(new InscripcionResumen(
                refProyecto, proyecto.cierre(), sinEquipo.size(), cuposLibres, persona(proyecto.idDocente())));
    }

    private void registrarSiHayDestinatarios(DatosEvento datos) {
        if (!DestinatariosEvento.de(datos).isEmpty()) {
            eventos.registrar(datos);
        }
    }

    private ContextoEquipo contextoEquipo(long idEquipo) {
        return jdbc.sql("SELECT e.id_equipo, e.nombre_equipo, p.id_proyecto, p.nombre, p.id_docente "
                        + "FROM equipo e JOIN proyecto p ON p.id_proyecto = e.id_proyecto WHERE e.id_equipo = :e")
                .param("e", idEquipo)
                .query((rs, fila) -> new ContextoEquipo(
                        new Referencia(rs.getLong("id_proyecto"), rs.getString("nombre")),
                        new Referencia(rs.getLong("id_equipo"), rs.getString("nombre_equipo")),
                        rs.getLong("id_docente")))
                .single();
    }

    private Persona persona(long idUsuario) {
        return jdbc.sql("SELECT id_usuario, nombre, telegram_chat_id FROM usuario WHERE id_usuario = :u")
                .param("u", idUsuario)
                .query(AvisosConfiguracion::mapearPersona)
                .single();
    }

    private List<Persona> integrantes(long idEquipo) {
        return jdbc.sql("SELECT u.id_usuario, u.nombre, u.telegram_chat_id "
                        + "FROM miembro_equipo m JOIN usuario u ON u.id_usuario = m.id_usuario "
                        + "WHERE m.id_equipo = :e AND u.fecha_anonimizacion IS NULL "
                        + "ORDER BY u.nombre, u.id_usuario")
                .param("e", idEquipo)
                .query(AvisosConfiguracion::mapearPersona)
                .list();
    }

    private static Persona mapearPersona(ResultSet rs, int fila) throws SQLException {
        return new Persona(
                rs.getLong("id_usuario"), rs.getString("nombre"), rs.getObject("telegram_chat_id", Long.class));
    }

    private record ContextoEquipo(Referencia proyecto, Referencia equipo, long idDocente) {}
}
