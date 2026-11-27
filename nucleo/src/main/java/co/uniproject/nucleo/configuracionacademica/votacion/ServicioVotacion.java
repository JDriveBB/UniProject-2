package co.uniproject.nucleo.configuracionacademica.votacion;

import co.uniproject.eventos.datos.OrigenLider;
import co.uniproject.nucleo.comun.error.ReglaNegocioException;
import co.uniproject.nucleo.configuracionacademica.avisos.AvisosConfiguracion;
import co.uniproject.nucleo.configuracionacademica.dominio.Equipo;
import co.uniproject.nucleo.configuracionacademica.repositorio.EquipoRepositorio;
import co.uniproject.nucleo.configuracionacademica.servicio.ApoyoConfiguracion;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.permisos.Permisos;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta, apertura, votación y desempate de las rondas de elección de líder de un equipo. */
@Service
public class ServicioVotacion {

    private final EquipoRepositorio equipos;
    private final RondasLider rondas;
    private final ApoyoConfiguracion apoyo;
    private final Permisos permisos;
    private final JdbcClient jdbc;
    private final AvisosConfiguracion avisos;
    private final Clock reloj;

    public ServicioVotacion(
            EquipoRepositorio equipos,
            RondasLider rondas,
            ApoyoConfiguracion apoyo,
            Permisos permisos,
            JdbcClient jdbc,
            AvisosConfiguracion avisos,
            Clock reloj) {
        this.equipos = equipos;
        this.rondas = rondas;
        this.apoyo = apoyo;
        this.permisos = permisos;
        this.jdbc = jdbc;
        this.avisos = avisos;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public VotacionVista vista(UsuarioAutenticado usuario, long idEquipo) {
        permisos.exigir(permisos.puedeVerEquipo(usuario.idUsuario(), idEquipo));
        Equipo equipo = equipos.findById(idEquipo).orElseThrow();
        return construirVista(usuario, equipo, Instant.now(reloj));
    }

    @Transactional
    public VotacionVista abrir(UsuarioAutenticado usuario, long idEquipo) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        apoyo.bloquearEquipo(idEquipo);
        Instant ahora = Instant.now(reloj);
        List<RondasLider.CandidatoFila> candidatos = rondas.candidatos(idEquipo);
        if (candidatos.isEmpty()) {
            throw new ReglaNegocioException(
                    "EQUIPO_SIN_INTEGRANTES", "El equipo no tiene integrantes que puedan votar");
        }
        Optional<RondaLiderFila> sinCerrar = rondas.sinCerrar(idEquipo);
        if (sinCerrar.isPresent() && sinCerrar.get().abierta(ahora)) {
            throw new ReglaNegocioException("RONDA_ABIERTA", "El equipo ya tiene una votación abierta");
        }
        if (sinCerrar.isPresent()) {
            rondas.cerrar(sinCerrar.get(), ahora);
        }
        rondas.abrir(usuario.idUsuario(), idEquipo, OrigenRonda.DEMANDA, ahora);
        return construirVista(usuario, equipo, ahora);
    }

    @Transactional
    public VotacionVista votar(UsuarioAutenticado usuario, long idEquipo, SolicitudVoto solicitud) {
        permisos.exigir(permisos.esIntegranteDelEquipo(usuario.idUsuario(), idEquipo));
        apoyo.bloquearEquipo(idEquipo);
        permisos.exigir(permisos.esIntegranteDelEquipo(usuario.idUsuario(), idEquipo));
        Instant ahora = Instant.now(reloj);
        Optional<RondaLiderFila> sinCerrar = rondas.sinCerrar(idEquipo);
        if (sinCerrar.isEmpty() || !sinCerrar.get().abierta(ahora)) {
            throw new ReglaNegocioException("SIN_RONDA_ABIERTA", "No hay una votación abierta en este equipo");
        }
        Set<Long> idsCandidatos = rondas.candidatos(idEquipo).stream()
                .map(RondasLider.CandidatoFila::idUsuario)
                .collect(Collectors.toSet());
        if (!idsCandidatos.contains(usuario.idUsuario()) || !idsCandidatos.contains(solicitud.idCandidato())) {
            throw new ReglaNegocioException("CANDIDATO_NO_VALIDO", "Solo puedes votar por un integrante del equipo");
        }
        rondas.registrarVoto(sinCerrar.get().idRonda(), usuario.idUsuario(), solicitud.idCandidato(), ahora);
        Equipo equipo = equipos.findById(idEquipo).orElseThrow();
        return construirVista(usuario, equipo, ahora);
    }

    @Transactional
    public VotacionVista desempatar(UsuarioAutenticado usuario, long idEquipo, SolicitudDesempate solicitud) {
        Equipo equipo = equipoDelDocente(usuario, idEquipo);
        apoyo.bloquearEquipo(idEquipo);
        Instant ahora = Instant.now(reloj);
        Optional<RondaLiderFila> ultima = rondas.ultima(idEquipo);
        if (ultima.isEmpty() || ultima.get().cerradaEn() == null || ultima.get().resultado() != ResultadoRonda.EMPATE) {
            throw new ReglaNegocioException("SIN_EMPATE", "La última votación del equipo no terminó en empate");
        }
        RondaLiderFila ronda = ultima.get();
        if (ronda.idLiderElegido() != null) {
            throw new ReglaNegocioException("EMPATE_RESUELTO", "Ya elegiste al líder de esa votación");
        }
        Map<Long, Integer> conteo = rondas.conteo(ronda.idRonda(), idEquipo);
        int maximo = conteo.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        Set<Long> empatados = maximo == 0
                ? Set.of()
                : conteo.entrySet().stream()
                        .filter(e -> e.getValue() == maximo)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
        if (!empatados.contains(solicitud.idUsuario())) {
            throw new ReglaNegocioException("NO_ESTA_EMPATADO", "Esa persona no está entre los empatados");
        }
        apoyo.nombrarLider(usuario.idUsuario(), idEquipo, solicitud.idUsuario());
        rondas.marcarDesempate(usuario.idUsuario(), ronda.idRonda(), solicitud.idUsuario());
        avisos.liderElegido(idEquipo, solicitud.idUsuario(), OrigenLider.DESEMPATE);
        return construirVista(usuario, equipo, ahora);
    }

    private VotacionVista construirVista(UsuarioAutenticado usuario, Equipo equipo, Instant ahora) {
        long idEquipo = equipo.getIdEquipo();
        Long idLiderActual = apoyo.liderDe(idEquipo).orElse(null);
        String nombreLiderActual = idLiderActual == null ? null : nombreDe(idLiderActual);
        boolean esDocente = permisos.esDocenteDelProyecto(usuario.idUsuario(), equipo.getIdProyecto());
        List<RondasLider.CandidatoFila> candidatos = rondas.candidatos(idEquipo);
        Optional<RondaLiderFila> filaOpt = rondas.sinCerrar(idEquipo).or(() -> rondas.ultima(idEquipo));
        if (filaOpt.isEmpty()) {
            return new VotacionVista(
                    idEquipo, idLiderActual, nombreLiderActual, null, esDocente && !candidatos.isEmpty(), false);
        }
        RondaLiderFila fila = filaOpt.get();
        boolean abierta = fila.abierta(ahora);
        Map<Long, Integer> conteo = rondas.conteo(fila.idRonda(), idEquipo);
        int votosEmitidos = conteo.values().stream().mapToInt(Integer::intValue).sum();
        int votantes = candidatos.size();
        boolean mostrarVotos = fila.cerradaEn() != null && fila.resultado() != ResultadoRonda.CANCELADA;
        boolean esEmpate = fila.cerradaEn() != null && fila.resultado() == ResultadoRonda.EMPATE;
        int maximo = conteo.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        Set<Long> idsCandidatos =
                candidatos.stream().map(RondasLider.CandidatoFila::idUsuario).collect(Collectors.toSet());
        List<CandidatoVista> listaCandidatos = candidatos.stream()
                .map(c -> {
                    int votos = conteo.getOrDefault(c.idUsuario(), 0);
                    return new CandidatoVista(
                            c.idUsuario(),
                            c.nombre(),
                            mostrarVotos ? votos : null,
                            esEmpate && maximo > 0 && votos == maximo);
                })
                .toList();
        Long miVoto = jdbc.sql("SELECT id_candidato FROM voto_lider WHERE id_ronda = :r AND id_votante = :v")
                .param("r", fila.idRonda())
                .param("v", usuario.idUsuario())
                .query(Long.class)
                .optional()
                .filter(idsCandidatos::contains)
                .orElse(null);
        boolean puedoVotar = abierta && idsCandidatos.contains(usuario.idUsuario());
        String nombreLiderElegido = fila.idLiderElegido() == null ? null : nombreDe(fila.idLiderElegido());
        RondaVista rondaVista = new RondaVista(
                fila.idRonda(),
                fila.origen(),
                fila.abreEn(),
                fila.cierraEn(),
                fila.cerradaEn(),
                abierta,
                fila.resultado(),
                fila.idLiderElegido(),
                nombreLiderElegido,
                votosEmitidos,
                votantes,
                miVoto,
                puedoVotar,
                listaCandidatos);
        boolean puedeAbrir = esDocente && !candidatos.isEmpty() && !abierta;
        boolean puedeDesempatar = esDocente && esEmpate && fila.idLiderElegido() == null;
        return new VotacionVista(idEquipo, idLiderActual, nombreLiderActual, rondaVista, puedeAbrir, puedeDesempatar);
    }

    private String nombreDe(long idUsuario) {
        return jdbc.sql("SELECT nombre FROM usuario WHERE id_usuario = :id")
                .param("id", idUsuario)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    /** 403 si el equipo no existe o no es de un proyecto del docente. */
    private Equipo equipoDelDocente(UsuarioAutenticado usuario, long idEquipo) {
        Optional<Equipo> equipo = equipos.findById(idEquipo);
        permisos.exigir(equipo.isPresent()
                && permisos.esDocenteDelProyecto(
                        usuario.idUsuario(), equipo.get().getIdProyecto()));
        return equipo.get();
    }
}
