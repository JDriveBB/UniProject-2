package co.uniproject.nucleo.configuracionacademica.votacion;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integración de RevisionRondas: apertura programada, cierre de vencidas y recordatorio de cierre. */
class ProgramadorRondasIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private RevisionRondas revisionRondas;

    private Navegador nuevoDocente() {
        String email = correoUnico("docprog");
        crearUsuario("Docente Programador", email, "DOCENTE");
        return sesion(email);
    }

    private long crearProyecto(Navegador docente, String nombre) {
        HttpResponse<String> r = docente.post(
                "/api/proyectos",
                "{\"nombre\":\"" + nombre + "\",\"fechaInicio\":\"2026-08-03\","
                        + "\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false,\"equiposMixtos\":false}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idProyecto");
    }

    @Test
    void alCierreSeAbrenLasRondasDeTodosLosEquipos() {
        Navegador docente = nuevoDocente();
        long c = crearCurso(docente, "Curso Prog");
        long p = crearProyecto(docente, "Proyecto Prog");
        asignarCursos(docente, p, c);

        Instant cierreFuturo = Instant.now().plus(3, ChronoUnit.DAYS);
        HttpResponse<String> cfg = docente.put(
                "/api/proyectos/" + p + "/inscripcion",
                "{\"cierreEquipos\":\"" + cierreFuturo + "\",\"horasRonda\":24}");
        assertThat(cfg.statusCode()).as(cfg.body()).isEqualTo(200);

        long eSinRonda = crearEquipoApi(docente, p, "Eq Sin Ronda", c, 3);
        long eConDemanda = crearEquipoApi(docente, p, "Eq Con Demanda", c, 3);
        long eVacio = crearEquipoApi(docente, p, "Eq Vacio", c, 3);

        String email1 = correoUnico("prog1");
        String email2 = correoUnico("prog2");
        crearUsuario("Est Prog 1", email1, "ESTUDIANTE");
        crearUsuario("Est Prog 2", email2, "ESTUDIANTE");
        agregarAlCurso(docente, c, email1, email2);
        assertThat(docente.post("/api/equipos/" + eSinRonda + "/miembros", "{\"email\":\"" + email1 + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertThat(docente.post("/api/equipos/" + eConDemanda + "/miembros", "{\"email\":\"" + email2 + "\"}")
                        .statusCode())
                .isEqualTo(201);

        long idDemanda = abrirRondaApi(docente, eConDemanda);
        cerrarInscripcionSql(p);

        assertThat(revisionRondas.abrirProgramadas()).isGreaterThanOrEqualTo(1);

        String origenSinRonda = jdbc.sql("SELECT origen FROM ronda_lider WHERE id_equipo = :e")
                .param("e", eSinRonda)
                .query(String.class)
                .single();
        assertThat(origenSinRonda).isEqualTo("PROGRAMADA");
        long horas = jdbc.sql("SELECT extract(epoch FROM (cierra_en - abre_en))::bigint / 3600 "
                        + "FROM ronda_lider WHERE id_equipo = :e")
                .param("e", eSinRonda)
                .query(Long.class)
                .single();
        assertThat(horas).isEqualTo(24);

        assertThat(jdbc.sql("SELECT id_ronda FROM ronda_lider WHERE id_equipo = :e")
                        .param("e", eConDemanda)
                        .query(Long.class)
                        .list())
                .containsExactly(idDemanda);
        assertThat(jdbc.sql("SELECT count(*) FROM ronda_lider WHERE id_equipo = :e")
                        .param("e", eVacio)
                        .query(Integer.class)
                        .single())
                .isZero();

        OffsetDateTime marca1 = jdbc.sql("SELECT rondas_programadas_en FROM proyecto WHERE id_proyecto = :p")
                .param("p", p)
                .query(OffsetDateTime.class)
                .single();
        assertThat(marca1).isNotNull();

        revisionRondas.abrirProgramadas();
        assertThat(jdbc.sql("SELECT count(*) FROM ronda_lider WHERE id_equipo = :e")
                        .param("e", eSinRonda)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM ronda_lider WHERE id_equipo = :e")
                        .param("e", eConDemanda)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        OffsetDateTime marca2 = jdbc.sql("SELECT rondas_programadas_en FROM proyecto WHERE id_proyecto = :p")
                .param("p", p)
                .query(OffsetDateTime.class)
                .single();
        assertThat(marca2).isEqualTo(marca1);

        HttpResponse<String> yaCerrada = docente.put(
                "/api/proyectos/" + p + "/inscripcion",
                "{\"cierreEquipos\":\"" + Instant.now().plus(5, ChronoUnit.DAYS) + "\",\"horasRonda\":24}");
        assertThat(yaCerrada.statusCode()).as(yaCerrada.body()).isEqualTo(409);
        assertThat(yaCerrada.body()).contains("\"codigo\":\"INSCRIPCION_YA_CERRADA\"");
    }

    @Test
    void noAbreAntesDelCierreNiSinFecha() {
        Navegador docente = nuevoDocente();
        long c = crearCurso(docente, "Curso Futuro");
        long pFuturo = crearProyecto(docente, "Proyecto Futuro");
        long pSinFecha = crearProyecto(docente, "Proyecto Sin Fecha");
        asignarCursos(docente, pFuturo, c);
        asignarCursos(docente, pSinFecha, c);
        abrirInscripcion(docente, pFuturo);

        long e1 = crearEquipoApi(docente, pFuturo, "Eq Futuro", c, 3);
        long e2 = crearEquipoApi(docente, pSinFecha, "Eq Sin Fecha", c, 3);
        String email = correoUnico("fut");
        crearUsuario("Est Futuro", email, "ESTUDIANTE");
        agregarAlCurso(docente, c, email);
        assertThat(docente.post("/api/equipos/" + e1 + "/miembros", "{\"email\":\"" + email + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertThat(docente.post("/api/equipos/" + e2 + "/miembros", "{\"email\":\"" + email + "\"}")
                        .statusCode())
                .isEqualTo(201);

        revisionRondas.abrirProgramadas();

        assertThat(jdbc.sql("SELECT count(*) FROM ronda_lider WHERE id_equipo IN (:e1, :e2)")
                        .param("e1", e1)
                        .param("e2", e2)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM proyecto WHERE id_proyecto IN (:p1, :p2) "
                                + "AND rondas_programadas_en IS NOT NULL")
                        .param("p1", pFuturo)
                        .param("p2", pSinFecha)
                        .query(Integer.class)
                        .single())
                .isZero();
    }

    @Test
    void cierraLasVencidasUnaSolaVez() {
        Navegador docente = nuevoDocente();
        long c = crearCurso(docente, "Curso Vencida");
        long p = crearProyecto(docente, "Proyecto Vencida");
        asignarCursos(docente, p, c);
        long e = crearEquipoApi(docente, p, "Eq Vencida", c, null);
        String email = correoUnico("venc");
        long idEst = crearUsuario("Est Vencida", email, "ESTUDIANTE");
        agregarAlCurso(docente, c, email);
        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + email + "\"}")
                        .statusCode())
                .isEqualTo(201);
        Navegador navEst = sesion(email);

        long idRonda = abrirRondaApi(docente, e);
        assertThat(votarApi(navEst, e, idEst).statusCode()).isEqualTo(200);
        vencerRondaSql(e);

        assertThat(revisionRondas.cerrarVencidas()).isGreaterThanOrEqualTo(1);
        OffsetDateTime cerrada1 = jdbc.sql("SELECT cerrada_en FROM ronda_lider WHERE id_ronda = :r")
                .param("r", idRonda)
                .query(OffsetDateTime.class)
                .single();
        assertThat(cerrada1).isNotNull();
        assertThat(jdbc.sql("SELECT resultado FROM ronda_lider WHERE id_ronda = :r")
                        .param("r", idRonda)
                        .query(String.class)
                        .single())
                .isEqualTo("LIDER_ELEGIDO");

        revisionRondas.cerrarVencidas();
        OffsetDateTime cerrada2 = jdbc.sql("SELECT cerrada_en FROM ronda_lider WHERE id_ronda = :r")
                .param("r", idRonda)
                .query(OffsetDateTime.class)
                .single();
        assertThat(cerrada2).isEqualTo(cerrada1);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria "
                                + "WHERE entidad_afectada = 'ronda_lider' AND id_registro_afectado = :r "
                                + "AND campo_modificado = 'resultado'")
                        .param("r", idRonda)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void elRecordatorioSeMarcaUnaVezPorFecha() {
        Navegador docente = nuevoDocente();
        long p23h = crearProyecto(docente, "Proyecto 23h");
        long p3d = crearProyecto(docente, "Proyecto 3d");
        long pPasado = crearProyecto(docente, "Proyecto Pasado");

        Instant en23h = Instant.now().plus(Duration.ofHours(23));
        Instant en3d = Instant.now().plus(Duration.ofDays(3));
        assertThat(docente.put(
                                "/api/proyectos/" + p23h + "/inscripcion",
                                "{\"cierreEquipos\":\"" + en23h + "\",\"horasRonda\":48}")
                        .statusCode())
                .isEqualTo(200);
        assertThat(docente.put(
                                "/api/proyectos/" + p3d + "/inscripcion",
                                "{\"cierreEquipos\":\"" + en3d + "\",\"horasRonda\":48}")
                        .statusCode())
                .isEqualTo(200);
        cerrarInscripcionSql(pPasado);

        assertThat(revisionRondas.recordarCierres()).isGreaterThanOrEqualTo(1);
        OffsetDateTime marca1 = jdbc.sql("SELECT recordatorio_cierre_en FROM proyecto WHERE id_proyecto = :p")
                .param("p", p23h)
                .query(OffsetDateTime.class)
                .single();
        assertThat(marca1).isNotNull();

        revisionRondas.recordarCierres();
        OffsetDateTime marca2 = jdbc.sql("SELECT recordatorio_cierre_en FROM proyecto WHERE id_proyecto = :p")
                .param("p", p23h)
                .query(OffsetDateTime.class)
                .single();
        assertThat(marca2).isEqualTo(marca1);

        assertThat(jdbc.sql(
                                "SELECT count(*) FROM proyecto WHERE id_proyecto = :p AND recordatorio_cierre_en IS NOT NULL")
                        .param("p", p3d)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbc.sql(
                                "SELECT count(*) FROM proyecto WHERE id_proyecto = :p AND recordatorio_cierre_en IS NOT NULL")
                        .param("p", pPasado)
                        .query(Integer.class)
                        .single())
                .isZero();

        Instant en20h = Instant.now().plus(Duration.ofHours(20));
        assertThat(docente.put(
                                "/api/proyectos/" + p23h + "/inscripcion",
                                "{\"cierreEquipos\":\"" + en20h + "\",\"horasRonda\":48}")
                        .statusCode())
                .isEqualTo(200);
        assertThat(jdbc.sql(
                                "SELECT count(*) FROM proyecto WHERE id_proyecto = :p AND recordatorio_cierre_en IS NOT NULL")
                        .param("p", p23h)
                        .query(Integer.class)
                        .single())
                .isZero();

        assertThat(revisionRondas.recordarCierres()).isGreaterThanOrEqualTo(1);
        OffsetDateTime marca3 = jdbc.sql("SELECT recordatorio_cierre_en FROM proyecto WHERE id_proyecto = :p")
                .param("p", p23h)
                .query(OffsetDateTime.class)
                .single();
        assertThat(marca3).isNotNull();
    }
}
