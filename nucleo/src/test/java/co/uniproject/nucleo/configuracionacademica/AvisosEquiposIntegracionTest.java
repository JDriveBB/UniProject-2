package co.uniproject.nucleo.configuracionacademica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import co.uniproject.eventos.CodecEventos;
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
import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.configuracionacademica.votacion.RevisionRondas;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integración de eventos y avisos de bandeja de cursos, equipos, reparto, recordatorios y votación (bloque 50). */
class AvisosEquiposIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private RevisionRondas revisionRondas;

    @Autowired
    private CodecEventos codec;

    private record Docente(long id, String nombre, Navegador nav) {}

    private Docente nuevoDocente(String nombre) {
        String email = correoUnico("docav");
        long id = crearUsuario(nombre, email, "DOCENTE");
        return new Docente(id, nombre, sesion(email));
    }

    private long crearProyecto(Navegador docente, String nombre) {
        HttpResponse<String> r = docente.post(
                "/api/proyectos",
                "{\"nombre\":\"" + nombre + "\",\"fechaInicio\":\"2026-08-03\","
                        + "\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false,\"equiposMixtos\":false}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idProyecto");
    }

    private List<String> cargas(String tipo, String ruta, long id) {
        List<String> lista = jdbc.sql("SELECT carga::text FROM evento_saliente WHERE tipo = :t "
                        + "AND carga #>> CAST(:ruta AS text[]) = :id ORDER BY creado_en")
                .param("t", tipo)
                .param("ruta", ruta)
                .param("id", String.valueOf(id))
                .query(String.class)
                .list();
        for (String json : lista) {
            assertThat(json).doesNotContain("@");
        }
        return lista;
    }

    private <T extends DatosEvento> List<T> eventosDe(String tipo, String ruta, long id, Class<T> clase) {
        return cargas(tipo, ruta, id).stream()
                .map(json -> clase.cast(
                        codec.leer(json.getBytes(StandardCharsets.UTF_8)).data()))
                .toList();
    }

    private List<String> mensajesBandeja(long idUsuario, String tipo, String entidad, long id) {
        return jdbc.sql("SELECT mensaje FROM notificacion WHERE id_destinatario = :u AND tipo = :t "
                        + "AND entidad_referencia = :ent AND id_referencia = :id ORDER BY id_notificacion")
                .param("u", idUsuario)
                .param("t", tipo)
                .param("ent", entidad)
                .param("id", id)
                .query(String.class)
                .list();
    }

    @Test
    void agregarAlCursoAvisaAlEstudiante() {
        Docente doc = nuevoDocente("Profe Curso");
        long curso = crearCurso(doc.nav(), "Arquitectura");
        String emailEst = correoUnico("estcur");
        long idEst = crearUsuario("Sofía Curso", emailEst, "ESTUDIANTE");
        String emailNuevo = correoUnico("sincuenta");

        agregarAlCurso(doc.nav(), curso, emailEst, emailNuevo);

        List<CursoEstudianteAgregado> evs =
                eventosDe("curso.estudiante_agregado", "{data,curso,id}", curso, CursoEstudianteAgregado.class);
        assertThat(evs).hasSize(1);
        assertThat(evs.getFirst().estudiante().id()).isEqualTo(idEst);
        assertThat(mensajesBandeja(idEst, "curso.estudiante_agregado", "curso", curso))
                .containsExactly("Profe Curso te agregó al curso \"Arquitectura\" (2026-2).");

        // Volver a agregar la misma cuenta no duplica el evento.
        agregarAlCurso(doc.nav(), curso, emailEst);
        assertThat(eventosDe("curso.estudiante_agregado", "{data,curso,id}", curso, CursoEstudianteAgregado.class))
                .hasSize(1);
    }

    @Test
    void abrirLaVotacionAvisaALosIntegrantes() {
        Docente doc = nuevoDocente("Profe Vot");
        long c = crearCurso(doc.nav(), "Curso Vot");
        long p = crearProyecto(doc.nav(), "Proyecto Vot");
        asignarCursos(doc.nav(), p, c);
        long e1 = crearEquipoApi(doc.nav(), p, "Equipo V1", c, null);
        String emailA = correoUnico("avota");
        String emailB = correoUnico("avotb");
        String emailC = correoUnico("avotc");
        long idA = crearUsuario("Ana Vot", emailA, "ESTUDIANTE");
        long idB = crearUsuario("Beto Vot", emailB, "ESTUDIANTE");
        long idC = crearUsuario("Carla Vot", emailC, "ESTUDIANTE");
        agregarAlCurso(doc.nav(), c, emailA, emailB, emailC);
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :a), (:e, :b), (:e, :c)")
                .param("e", e1)
                .param("a", idA)
                .param("b", idB)
                .param("c", idC)
                .update();

        long idRonda = abrirRondaApi(doc.nav(), e1);
        Instant cierraBd = jdbc.sql("SELECT cierra_en FROM ronda_lider WHERE id_ronda = :r")
                .param("r", idRonda)
                .query((rs, f) ->
                        rs.getObject("cierra_en", OffsetDateTime.class).toInstant())
                .single();

        List<EquipoVotacionAbierta> evs =
                eventosDe("equipo.votacion_abierta", "{data,equipo,id}", e1, EquipoVotacionAbierta.class);
        assertThat(evs).hasSize(1);
        assertThat(evs.getFirst().integrantes()).extracting(Persona::id).containsExactlyInAnyOrder(idA, idB, idC);
        assertThat(evs.getFirst().cierraEn()).isCloseTo(cierraBd, within(1, ChronoUnit.MICROS));
        for (long id : List.of(idA, idB, idC)) {
            assertThat(mensajesBandeja(id, "equipo.votacion_abierta", "equipo", e1))
                    .containsExactly("Se abrió la votación de líder de Equipo V1. Ya puedes votar.");
        }
        assertThat(mensajesBandeja(doc.id(), "equipo.votacion_abierta", "equipo", e1))
                .isEmpty();

        // Apertura programada tras cierre de inscripción también emite 1 evento.
        long p2 = crearProyecto(doc.nav(), "Proyecto Prog");
        asignarCursos(doc.nav(), p2, c);
        long e2 = crearEquipoApi(doc.nav(), p2, "Equipo Prog", c, 3);
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :a)")
                .param("e", e2)
                .param("a", idA)
                .update();
        abrirInscripcion(doc.nav(), p2);
        cerrarInscripcionSql(p2);
        revisionRondas.abrirProgramadas();

        assertThat(eventosDe("equipo.votacion_abierta", "{data,equipo,id}", e2, EquipoVotacionAbierta.class))
                .hasSize(1);
    }

    @Test
    void elResultadoAvisaSegunElCaso() {
        Docente doc = nuevoDocente("Profe Res");
        long c = crearCurso(doc.nav(), "Curso Res");
        long p = crearProyecto(doc.nav(), "Proyecto Res");
        asignarCursos(doc.nav(), p, c);
        long e = crearEquipoApi(doc.nav(), p, "Equipo Res", c, null);
        String emailA = correoUnico("resa");
        String emailB = correoUnico("resb");
        String emailC = correoUnico("resc");
        long idA = crearUsuario("Ana Res", emailA, "ESTUDIANTE");
        long idB = crearUsuario("Beto Res", emailB, "ESTUDIANTE");
        long idC = crearUsuario("Carla Res", emailC, "ESTUDIANTE");
        agregarAlCurso(doc.nav(), c, emailA, emailB, emailC);
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :a), (:e, :b), (:e, :c)")
                .param("e", e)
                .param("a", idA)
                .param("b", idB)
                .param("c", idC)
                .update();
        Navegador navA = sesion(emailA);
        Navegador navB = sesion(emailB);
        Navegador navC = sesion(emailC);

        // (a) Ganador por votos.
        abrirRondaApi(doc.nav(), e);
        assertThat(votarApi(navA, e, idB).statusCode()).isEqualTo(200);
        assertThat(votarApi(navB, e, idB).statusCode()).isEqualTo(200);
        assertThat(votarApi(navC, e, idA).statusCode()).isEqualTo(200);
        vencerRondaSql(e);
        revisionRondas.cerrarVencidas();

        List<EquipoLiderElegido> elegidos =
                eventosDe("equipo.lider_elegido", "{data,equipo,id}", e, EquipoLiderElegido.class);
        assertThat(elegidos).hasSize(1);
        assertThat(elegidos.getFirst().origen()).isEqualTo(OrigenLider.VOTACION);
        assertThat(elegidos.getFirst().lider().id()).isEqualTo(idB);
        for (long id : List.of(idA, idB, idC, doc.id())) {
            assertThat(mensajesBandeja(id, "equipo.lider_elegido", "equipo", e))
                    .containsExactly("Beto Res es el nuevo líder de Equipo Res, elegido por votación.");
        }

        // (b) Empate y luego desempate del docente (equipo sin líder).
        jdbc.sql("UPDATE miembro_equipo SET rol_equipo = 'INTEGRANTE' WHERE id_equipo = :e")
                .param("e", e)
                .update();
        abrirRondaApi(doc.nav(), e);
        assertThat(votarApi(navA, e, idA).statusCode()).isEqualTo(200);
        assertThat(votarApi(navB, e, idC).statusCode()).isEqualTo(200);
        vencerRondaSql(e);
        revisionRondas.cerrarVencidas();

        List<EquipoSinLider> sinLider = eventosDe("equipo.sin_lider", "{data,equipo,id}", e, EquipoSinLider.class);
        assertThat(sinLider).hasSize(1);
        assertThat(sinLider.getFirst().motivo()).isEqualTo(MotivoSinLider.EMPATE);
        assertThat(mensajesBandeja(doc.id(), "equipo.sin_lider", "equipo", e))
                .containsExactly(
                        "La votación de líder de Equipo Res terminó en empate. Elige al líder entre los empatados.");
        assertThat(mensajesBandeja(idA, "equipo.sin_lider", "equipo", e)).isEmpty();

        HttpResponse<String> desempate =
                doc.nav().put("/api/equipos/" + e + "/ronda/desempate", "{\"idUsuario\":" + idC + "}");
        assertThat(desempate.statusCode()).as(desempate.body()).isEqualTo(200);

        elegidos = eventosDe("equipo.lider_elegido", "{data,equipo,id}", e, EquipoLiderElegido.class);
        assertThat(elegidos).hasSize(2);
        assertThat(elegidos.get(1).origen()).isEqualTo(OrigenLider.DESEMPATE);
        assertThat(elegidos.get(1).lider().id()).isEqualTo(idC);

        // (c) Sin votos (equipo sin líder).
        jdbc.sql("UPDATE miembro_equipo SET rol_equipo = 'INTEGRANTE' WHERE id_equipo = :e")
                .param("e", e)
                .update();
        abrirRondaApi(doc.nav(), e);
        vencerRondaSql(e);
        revisionRondas.cerrarVencidas();

        sinLider = eventosDe("equipo.sin_lider", "{data,equipo,id}", e, EquipoSinLider.class);
        assertThat(sinLider).hasSize(2);
        assertThat(sinLider.get(1).motivo()).isEqualTo(MotivoSinLider.SIN_VOTOS);
    }

    @Test
    void cerrarSinVotosConLiderNoEmiteSinLider() {
        Docente doc = nuevoDocente("Profe ConLider");
        long c = crearCurso(doc.nav(), "Curso ConLider");
        long p = crearProyecto(doc.nav(), "Proyecto ConLider");
        asignarCursos(doc.nav(), p, c);
        long e = crearEquipoApi(doc.nav(), p, "Equipo ConLider", c, null);
        String emailA = correoUnico("clida");
        String emailB = correoUnico("clidb");
        long idA = crearUsuario("Ana ConLider", emailA, "ESTUDIANTE");
        long idB = crearUsuario("Beto ConLider", emailB, "ESTUDIANTE");
        agregarAlCurso(doc.nav(), c, emailA, emailB);
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :a), (:e, :b)")
                .param("e", e)
                .param("a", idA)
                .param("b", idB)
                .update();
        assertThat(doc.nav()
                        .put("/api/equipos/" + e + "/lider", "{\"idUsuario\":" + idA + "}")
                        .statusCode())
                .isEqualTo(200);

        abrirRondaApi(doc.nav(), e);
        vencerRondaSql(e);
        revisionRondas.cerrarVencidas();

        assertThat(jdbc.sql("SELECT resultado FROM ronda_lider WHERE id_equipo = :e")
                        .param("e", e)
                        .query(String.class)
                        .single())
                .isEqualTo("SIN_VOTOS");
        assertThat(eventosDe("equipo.sin_lider", "{data,equipo,id}", e, EquipoSinLider.class))
                .isEmpty();
    }

    @Test
    void entradasYSalidasAvisanALosDemas() {
        Docente doc = nuevoDocente("Profe Miembros");
        long c = crearCurso(doc.nav(), "Curso Miembros");
        long p = crearProyecto(doc.nav(), "Proyecto Miembros");
        asignarCursos(doc.nav(), p, c);
        abrirInscripcion(doc.nav(), p);
        long e = crearEquipoApi(doc.nav(), p, "Equipo M", c, 4);
        long eVacio = crearEquipoApi(doc.nav(), p, "Equipo Vacio", c, 3);

        String emailA = correoUnico("mia");
        String emailX = correoUnico("mix");
        String emailY = correoUnico("miy");
        String emailSolo = correoUnico("misolo");
        long idA = crearUsuario("Ana M", emailA, "ESTUDIANTE");
        long idX = crearUsuario("Ximena M", emailX, "ESTUDIANTE");
        long idY = crearUsuario("Yolanda M", emailY, "ESTUDIANTE");
        crearUsuario("Solo M", emailSolo, "ESTUDIANTE");
        agregarAlCurso(doc.nav(), c, emailA, emailX, emailY, emailSolo);
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :a)")
                .param("e", e)
                .param("a", idA)
                .update();

        // Docente agrega a X.
        assertThat(doc.nav()
                        .post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + emailX + "\"}")
                        .statusCode())
                .isEqualTo(201);
        List<EquipoMiembroCambio> evs =
                eventosDe("equipo.miembro_cambio", "{data,equipo,id}", e, EquipoMiembroCambio.class);
        assertThat(evs).hasSize(1);
        assertThat(evs.getFirst().movimiento()).isEqualTo(MovimientoEquipo.ENTRO);
        assertThat(evs.getFirst().porDocente()).isTrue();
        assertThat(mensajesBandeja(idX, "equipo.miembro_cambio", "equipo", e))
                .containsExactly("El docente te agregó a Equipo M en \"Proyecto Miembros\".");
        assertThat(mensajesBandeja(idA, "equipo.miembro_cambio", "equipo", e))
                .containsExactly("Ximena M entró a Equipo M.");

        // Y se une por su cuenta.
        Navegador navY = sesion(emailY);
        assertThat(navY.post("/api/equipos/" + e + "/inscripcion", null).statusCode())
                .isEqualTo(200);
        evs = eventosDe("equipo.miembro_cambio", "{data,equipo,id}", e, EquipoMiembroCambio.class);
        assertThat(evs).hasSize(2);
        assertThat(evs.get(1).movimiento()).isEqualTo(MovimientoEquipo.ENTRO);
        assertThat(evs.get(1).porDocente()).isFalse();
        assertThat(mensajesBandeja(idY, "equipo.miembro_cambio", "equipo", e)).isEmpty();

        // Y sale por su cuenta.
        assertThat(navY.delete("/api/equipos/" + e + "/inscripcion").statusCode())
                .isEqualTo(204);
        evs = eventosDe("equipo.miembro_cambio", "{data,equipo,id}", e, EquipoMiembroCambio.class);
        assertThat(evs).hasSize(3);
        assertThat(evs.get(2).movimiento()).isEqualTo(MovimientoEquipo.SALIO);
        assertThat(evs.get(2).porDocente()).isFalse();

        // Docente quita a X; X no recibe fila de ese evento.
        int filasXAntes =
                mensajesBandeja(idX, "equipo.miembro_cambio", "equipo", e).size();
        assertThat(doc.nav().delete("/api/equipos/" + e + "/miembros/" + idX).statusCode())
                .isEqualTo(200);
        evs = eventosDe("equipo.miembro_cambio", "{data,equipo,id}", e, EquipoMiembroCambio.class);
        assertThat(evs).hasSize(4);
        assertThat(evs.get(3).movimiento()).isEqualTo(MovimientoEquipo.SALIO);
        assertThat(evs.get(3).porDocente()).isTrue();
        assertThat(mensajesBandeja(idX, "equipo.miembro_cambio", "equipo", e)).hasSize(filasXAntes);

        // Estudiante que se une solo a un equipo vacío -> 0 eventos de ese equipo.
        assertThat(sesion(emailSolo)
                        .post("/api/equipos/" + eVacio + "/inscripcion", null)
                        .statusCode())
                .isEqualTo(200);
        assertThat(eventosDe("equipo.miembro_cambio", "{data,equipo,id}", eVacio, EquipoMiembroCambio.class))
                .isEmpty();
    }

    @Test
    void elRepartoEmiteUnEventoPorEquipo() {
        Docente doc = nuevoDocente("Profe Reparto");
        long c = crearCurso(doc.nav(), "Curso Reparto");
        long p = crearProyecto(doc.nav(), "Proyecto Reparto");
        asignarCursos(doc.nav(), p, c);
        long e = crearEquipoApi(doc.nav(), p, "Equipo Rep", c, 3);

        String emailPrev = correoUnico("repprev");
        String email1 = correoUnico("rep1");
        String email2 = correoUnico("rep2");
        long idPrev = crearUsuario("Previo Rep", emailPrev, "ESTUDIANTE");
        long id1 = crearUsuario("Andrés Rep", email1, "ESTUDIANTE");
        long id2 = crearUsuario("Beatriz Rep", email2, "ESTUDIANTE");
        agregarAlCurso(doc.nav(), c, emailPrev, email1, email2);
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :u)")
                .param("e", e)
                .param("u", idPrev)
                .update();

        HttpResponse<String> resp = doc.nav().post("/api/proyectos/" + p + "/equipos/reparto", null);
        assertThat(resp.statusCode()).as(resp.body()).isEqualTo(200);

        List<EquipoMiembroCambio> evs =
                eventosDe("equipo.miembro_cambio", "{data,equipo,id}", e, EquipoMiembroCambio.class);
        assertThat(evs).hasSize(1);
        assertThat(evs.getFirst().porDocente()).isTrue();
        assertThat(evs.getFirst().personas()).extracting(Persona::id).containsExactly(id1, id2);
        assertThat(mensajesBandeja(idPrev, "equipo.miembro_cambio", "equipo", e))
                .containsExactly("Andrés Rep y Beatriz Rep entraron a Equipo Rep.");
    }

    @Test
    void elRecordatorioAvisaALosSinEquipoYAlDocente() {
        Docente doc = nuevoDocente("Profe Rec");
        long c = crearCurso(doc.nav(), "Curso Rec");
        long p = crearProyecto(doc.nav(), "Proyecto Rec");
        asignarCursos(doc.nav(), p, c);
        long e = crearEquipoApi(doc.nav(), p, "Equipo Rec", c, 3);

        String emailCon = correoUnico("reccon");
        String emailSin1 = correoUnico("recsin1");
        String emailSin2 = correoUnico("recsin2");
        long idCon = crearUsuario("Con Equipo", emailCon, "ESTUDIANTE");
        long idSin1 = crearUsuario("Sin Equipo 1", emailSin1, "ESTUDIANTE");
        long idSin2 = crearUsuario("Sin Equipo 2", emailSin2, "ESTUDIANTE");
        agregarAlCurso(doc.nav(), c, emailCon, emailSin1, emailSin2);
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :u)")
                .param("e", e)
                .param("u", idCon)
                .update();

        Instant cierre = Instant.now().plus(23, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        HttpResponse<String> cfg = doc.nav()
                .put(
                        "/api/proyectos/" + p + "/inscripcion",
                        "{\"cierreEquipos\":\"" + cierre + "\",\"horasRonda\":48}");
        assertThat(cfg.statusCode()).as(cfg.body()).isEqualTo(200);

        revisionRondas.recordarCierres();

        List<InscripcionPorCerrar> porCerrar =
                eventosDe("equipo.inscripcion_por_cerrar", "{data,proyecto,id}", p, InscripcionPorCerrar.class);
        assertThat(porCerrar).hasSize(1);
        assertThat(porCerrar.getFirst().estudiantes())
                .extracting(Persona::id)
                .containsExactlyInAnyOrder(idSin1, idSin2);

        List<InscripcionResumen> resumenes =
                eventosDe("equipo.inscripcion_resumen", "{data,proyecto,id}", p, InscripcionResumen.class);
        assertThat(resumenes).hasSize(1);
        assertThat(resumenes.getFirst().sinEquipo()).isEqualTo(2);
        assertThat(resumenes.getFirst().cuposLibres()).isEqualTo(2);

        // Segunda llamada no duplica.
        revisionRondas.recordarCierres();
        assertThat(eventosDe("equipo.inscripcion_por_cerrar", "{data,proyecto,id}", p, InscripcionPorCerrar.class))
                .hasSize(1);
        assertThat(eventosDe("equipo.inscripcion_resumen", "{data,proyecto,id}", p, InscripcionResumen.class))
                .hasSize(1);

        // Otro proyecto donde todos tienen equipo -> 0 por cerrar y 1 resumen con sinEquipo 0.
        long c2 = crearCurso(doc.nav(), "Curso Rec Completo");
        long p2 = crearProyecto(doc.nav(), "Proyecto Rec Completo");
        asignarCursos(doc.nav(), p2, c2);
        long e2 = crearEquipoApi(doc.nav(), p2, "Equipo Rec 2", c2, 2);
        agregarAlCurso(doc.nav(), c2, emailCon);
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :u)")
                .param("e", e2)
                .param("u", idCon)
                .update();
        HttpResponse<String> cfg2 = doc.nav()
                .put(
                        "/api/proyectos/" + p2 + "/inscripcion",
                        "{\"cierreEquipos\":\"" + cierre + "\",\"horasRonda\":48}");
        assertThat(cfg2.statusCode()).as(cfg2.body()).isEqualTo(200);

        revisionRondas.recordarCierres();

        assertThat(eventosDe("equipo.inscripcion_por_cerrar", "{data,proyecto,id}", p2, InscripcionPorCerrar.class))
                .isEmpty();
        List<InscripcionResumen> resumenes2 =
                eventosDe("equipo.inscripcion_resumen", "{data,proyecto,id}", p2, InscripcionResumen.class);
        assertThat(resumenes2).hasSize(1);
        assertThat(resumenes2.getFirst().sinEquipo()).isZero();
    }
}
