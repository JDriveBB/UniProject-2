package co.uniproject.nucleo.configuracionacademica.votacion;

import co.uniproject.nucleo.configuracionacademica.servicio.ApoyoConfiguracion;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones de persistencia sobre rondas y votos de elección de líder (sin comprobación de permisos).
 *
 * <p>Reglas: solo cuentan votos emitidos por integrantes actuales no anonimizados a favor de integrantes actuales no
 * anonimizados. Al cerrar: sin votos válidos produce SIN_VOTOS; un único máximo produce LIDER_ELEGIDO y nombra líder al
 * ganador como sistema; varios máximos producen EMPATE sin cambiar el líder. Toda escritura exige una transacción
 * activa con el candado del equipo ya tomado (orden: correo_habilitado, curso, proyecto, equipo, filas).
 */
@Component
public class RondasLider {

    private static final String COLUMNAS_RONDA =
            "id_ronda, id_equipo, origen, abre_en, cierra_en, cerrada_en, resultado, id_lider_elegido";

    private final JdbcClient jdbc;
    private final ApoyoConfiguracion apoyo;
    private final RegistroAuditoria auditoria;

    public RondasLider(JdbcClient jdbc, ApoyoConfiguracion apoyo, RegistroAuditoria auditoria) {
        this.jdbc = jdbc;
        this.apoyo = apoyo;
        this.auditoria = auditoria;
    }

    /** Integrante elegible para votar y ser votado en una ronda. */
    public record CandidatoFila(long idUsuario, String nombre) {}

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<RondaLiderFila> sinCerrar(long idEquipo) {
        return jdbc.sql("SELECT " + COLUMNAS_RONDA
                        + " FROM ronda_lider WHERE id_equipo = :equipo AND cerrada_en IS NULL")
                .param("equipo", idEquipo)
                .query(RondasLider::mapearRonda)
                .optional();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<RondaLiderFila> ultima(long idEquipo) {
        return jdbc.sql("SELECT " + COLUMNAS_RONDA + " FROM ronda_lider WHERE id_equipo = :equipo "
                        + "ORDER BY id_ronda DESC LIMIT 1")
                .param("equipo", idEquipo)
                .query(RondasLider::mapearRonda)
                .optional();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<CandidatoFila> candidatos(long idEquipo) {
        return jdbc.sql("SELECT u.id_usuario, u.nombre FROM miembro_equipo m "
                        + "JOIN usuario u ON u.id_usuario = m.id_usuario "
                        + "WHERE m.id_equipo = :equipo AND u.fecha_anonimizacion IS NULL "
                        + "ORDER BY u.nombre, u.id_usuario")
                .param("equipo", idEquipo)
                .query((rs, fila) -> new CandidatoFila(rs.getLong("id_usuario"), rs.getString("nombre")))
                .list();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, Integer> conteo(long idRonda, long idEquipo) {
        Map<Long, Integer> resultado = new LinkedHashMap<>();
        jdbc.sql("SELECT v.id_candidato, count(*) AS votos "
                        + "FROM voto_lider v "
                        + "JOIN miembro_equipo mv ON mv.id_equipo = :equipo AND mv.id_usuario = v.id_votante "
                        + "JOIN usuario uv ON uv.id_usuario = v.id_votante AND uv.fecha_anonimizacion IS NULL "
                        + "JOIN miembro_equipo mc ON mc.id_equipo = :equipo AND mc.id_usuario = v.id_candidato "
                        + "JOIN usuario uc ON uc.id_usuario = v.id_candidato AND uc.fecha_anonimizacion IS NULL "
                        + "WHERE v.id_ronda = :ronda "
                        + "GROUP BY v.id_candidato")
                .param("equipo", idEquipo)
                .param("ronda", idRonda)
                .query((rs, fila) -> Map.entry(rs.getLong("id_candidato"), rs.getInt("votos")))
                .list()
                .forEach(par -> resultado.put(par.getKey(), par.getValue()));
        return resultado;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long abrir(Long idActor, long idEquipo, OrigenRonda origen, Instant ahora) {
        int horas = jdbc.sql("SELECT p.horas_ronda FROM proyecto p "
                        + "JOIN equipo e ON e.id_proyecto = p.id_proyecto "
                        + "WHERE e.id_equipo = :equipo")
                .param("equipo", idEquipo)
                .query(Integer.class)
                .single();
        Instant cierra = ahora.plus(horas, ChronoUnit.HOURS);
        long idRonda = jdbc.sql("INSERT INTO ronda_lider (id_equipo, origen, abre_en, cierra_en) "
                        + "VALUES (:equipo, :origen, :abre, :cierra) RETURNING id_ronda")
                .param("equipo", idEquipo)
                .param("origen", origen.name())
                .param("abre", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .param("cierra", OffsetDateTime.ofInstant(cierra, ZoneOffset.UTC))
                .query(Long.class)
                .single();
        auditoria.registrar(EntradaAuditoria.cambio(
                idActor, "ronda_lider", idRonda, AccionAuditoria.CREAR, "origen", null, origen.name()));
        // Bloque 4: registrar equipo.votacion_abierta (integrantes del equipo) con RegistroEventos en esta transacción.
        return idRonda;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ResultadoRonda cerrar(RondaLiderFila ronda, Instant ahora) {
        Map<Long, Integer> votos = conteo(ronda.idRonda(), ronda.idEquipo());
        ResultadoRonda resultado;
        Long ganador = null;
        if (votos.isEmpty()) {
            resultado = ResultadoRonda.SIN_VOTOS;
        } else {
            int maximo =
                    votos.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            List<Long> maximos = votos.entrySet().stream()
                    .filter(e -> e.getValue() == maximo)
                    .map(Map.Entry::getKey)
                    .toList();
            if (maximos.size() == 1) {
                resultado = ResultadoRonda.LIDER_ELEGIDO;
                ganador = maximos.getFirst();
                apoyo.nombrarLider(null, ronda.idEquipo(), ganador);
            } else {
                resultado = ResultadoRonda.EMPATE;
            }
        }
        jdbc.sql("UPDATE ronda_lider SET cerrada_en = :ahora, resultado = :resultado, id_lider_elegido = :lider "
                        + "WHERE id_ronda = :r AND cerrada_en IS NULL")
                .param("ahora", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .param("resultado", resultado.name())
                .param("lider", ganador, Types.BIGINT)
                .param("r", ronda.idRonda())
                .update();
        auditoria.registrar(EntradaAuditoria.cambio(
                null,
                "ronda_lider",
                ronda.idRonda(),
                AccionAuditoria.CAMBIAR_ESTADO,
                "resultado",
                null,
                resultado.name()));
        if (resultado == ResultadoRonda.LIDER_ELEGIDO) {
            // Bloque 4: registrar equipo.lider_elegido (equipo y docente).
        } else {
            // Bloque 4: registrar equipo.sin_lider (docente), con el motivo EMPATE o SIN_VOTOS.
        }
        return resultado;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ResultadoRonda> cerrarSiVencida(long idEquipo, Instant ahora) {
        Optional<RondaLiderFila> fila = sinCerrar(idEquipo);
        if (fila.isPresent() && fila.get().vencida(ahora)) {
            return Optional.of(cerrar(fila.get(), ahora));
        }
        return Optional.empty();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean cancelarSinCerrar(Long idActor, long idEquipo, Instant ahora) {
        Optional<RondaLiderFila> fila = sinCerrar(idEquipo);
        if (fila.isEmpty()) {
            return false;
        }
        long idRonda = fila.get().idRonda();
        jdbc.sql("UPDATE ronda_lider SET cerrada_en = :ahora, resultado = 'CANCELADA' "
                        + "WHERE id_ronda = :r AND cerrada_en IS NULL")
                .param("ahora", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .param("r", idRonda)
                .update();
        auditoria.registrar(EntradaAuditoria.cambio(
                idActor,
                "ronda_lider",
                idRonda,
                AccionAuditoria.CAMBIAR_ESTADO,
                "resultado",
                null,
                ResultadoRonda.CANCELADA.name()));
        return true;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarVoto(long idRonda, long idVotante, long idCandidato, Instant ahora) {
        jdbc.sql("INSERT INTO voto_lider (id_ronda, id_votante, id_candidato, votado_en) "
                        + "VALUES (:r, :v, :c, :ahora) "
                        + "ON CONFLICT (id_ronda, id_votante) DO UPDATE SET "
                        + "id_candidato = EXCLUDED.id_candidato, votado_en = EXCLUDED.votado_en")
                .param("r", idRonda)
                .param("v", idVotante)
                .param("c", idCandidato)
                .param("ahora", OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                .update();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void marcarDesempate(Long idActor, long idRonda, long idElegido) {
        jdbc.sql("UPDATE ronda_lider SET id_lider_elegido = :u WHERE id_ronda = :r")
                .param("u", idElegido)
                .param("r", idRonda)
                .update();
        auditoria.registrar(EntradaAuditoria.cambio(
                idActor,
                "ronda_lider",
                idRonda,
                AccionAuditoria.ACTUALIZAR,
                "id_lider_elegido",
                null,
                String.valueOf(idElegido)));
    }

    private static RondaLiderFila mapearRonda(ResultSet rs, int fila) throws SQLException {
        OffsetDateTime cerrada = rs.getObject("cerrada_en", OffsetDateTime.class);
        String res = rs.getString("resultado");
        Long idLider = rs.getObject("id_lider_elegido", Long.class);
        return new RondaLiderFila(
                rs.getLong("id_ronda"),
                rs.getLong("id_equipo"),
                OrigenRonda.valueOf(rs.getString("origen")),
                rs.getObject("abre_en", OffsetDateTime.class).toInstant(),
                rs.getObject("cierra_en", OffsetDateTime.class).toInstant(),
                cerrada == null ? null : cerrada.toInstant(),
                res == null ? null : ResultadoRonda.valueOf(res),
                idLider);
    }
}
