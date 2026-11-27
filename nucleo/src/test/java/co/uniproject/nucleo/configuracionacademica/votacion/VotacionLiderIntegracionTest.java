package co.uniproject.nucleo.configuracionacademica.votacion;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Integración de votación de líder: apertura, voto secreto, cierre, empate, desempate, destitución y salidas. */
class VotacionLiderIntegracionTest extends ApiIntegracionBase {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private RevisionRondas revisionRondas;

    private record Escenario(
            Navegador docente,
            long idProyecto,
            long idCurso,
            long idEquipo,
            long idA,
            long idB,
            long idC,
            Navegador navA,
            Navegador navB,
            Navegador navC) {}

    private Navegador nuevoDocente() {
        String email = correoUnico("docvot");
        crearUsuario("Docente Votacion", email, "DOCENTE");
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

    private Escenario escenarioConTres(String sufijo) {
        Navegador docente = nuevoDocente();
        long c = crearCurso(docente, "Curso Vot " + sufijo);
        long p = crearProyecto(docente, "Proyecto Vot " + sufijo);
        asignarCursos(docente, p, c);
        long e = crearEquipoApi(docente, p, "Equipo " + sufijo, c, null);
        String emailA = correoUnico("vota");
        String emailB = correoUnico("votb");
        String emailC = correoUnico("votc");
        long idA = crearUsuario("Ana " + sufijo, emailA, "ESTUDIANTE");
        long idB = crearUsuario("Beto " + sufijo, emailB, "ESTUDIANTE");
        long idC = crearUsuario("Carla " + sufijo, emailC, "ESTUDIANTE");
        agregarAlCurso(docente, c, emailA, emailB, emailC);
        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + emailA + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + emailB + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertThat(docente.post("/api/equipos/" + e + "/miembros", "{\"email\":\"" + emailC + "\"}")
                        .statusCode())
                .isEqualTo(201);
        return new Escenario(docente, p, c, e, idA, idB, idC, sesion(emailA), sesion(emailB), sesion(emailC));
    }

    @Test
    void laMayoriaEligeAlLider() {
        Escenario esc = escenarioConTres("mayoria");
        assertThat(esc.docente()
                        .put("/api/equipos/" + esc.idEquipo() + "/lider", "{\"idUsuario\":" + esc.idA() + "}")
                        .statusCode())
                .isEqualTo(200);

        HttpResponse<String> apertura = esc.docente().post("/api/equipos/" + esc.idEquipo() + "/ronda", null);
        assertThat(apertura.statusCode()).as(apertura.body()).isEqualTo(201);
        JsonNode rondaAbierta = JSON.readTree(apertura.body()).get("ronda");
        assertThat(rondaAbierta.get("abierta").asBoolean()).isTrue();
        assertThat(rondaAbierta.get("origen").asString()).isEqualTo("DEMANDA");
        Instant abre = Instant.parse(rondaAbierta.get("abreEn").asString());
        Instant cierra = Instant.parse(rondaAbierta.get("cierraEn").asString());
        assertThat(Duration.between(abre, cierra).toHours()).isEqualTo(48);

        assertThat(votarApi(esc.navA(), esc.idEquipo(), esc.idB()).statusCode()).isEqualTo(200);
        assertThat(votarApi(esc.navB(), esc.idEquipo(), esc.idB()).statusCode()).isEqualTo(200);
        assertThat(votarApi(esc.navC(), esc.idEquipo(), esc.idA()).statusCode()).isEqualTo(200);

        HttpResponse<String> vistaA = esc.navA().get("/api/equipos/" + esc.idEquipo() + "/ronda");
        assertThat(vistaA.statusCode()).as(vistaA.body()).isEqualTo(200);
        assertThat(vistaA.body()).doesNotContain("@");
        JsonNode rondaA = JSON.readTree(vistaA.body()).get("ronda");
        assertThat(rondaA.get("votosEmitidos").asInt()).isEqualTo(3);
        assertThat(rondaA.get("votantes").asInt()).isEqualTo(3);
        assertThat(rondaA.get("miVoto").asLong()).isEqualTo(esc.idB());
        assertThat(rondaA.get("puedoVotar").asBoolean()).isTrue();
        for (JsonNode cand : rondaA.get("candidatos")) {
            assertThat(cand.get("votos").isNull()).isTrue();
        }

        vencerRondaSql(esc.idEquipo());
        assertThat(revisionRondas.cerrarVencidas()).isGreaterThanOrEqualTo(1);

        assertThat(jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", esc.idEquipo())
                        .query(Long.class)
                        .list())
                .containsExactly(esc.idB());
        assertThat(jdbc.sql("SELECT rol_equipo FROM miembro_equipo WHERE id_equipo = :e AND id_usuario = :u")
                        .param("e", esc.idEquipo())
                        .param("u", esc.idA())
                        .query(String.class)
                        .single())
                .isEqualTo("INTEGRANTE");
        String resultadoSql = jdbc.sql(
                        "SELECT resultado || ':' || id_lider_elegido FROM ronda_lider WHERE id_equipo = :e")
                .param("e", esc.idEquipo())
                .query(String.class)
                .single();
        assertThat(resultadoSql).isEqualTo("LIDER_ELEGIDO:" + esc.idB());

        HttpResponse<String> vistaCerrada = esc.navA().get("/api/equipos/" + esc.idEquipo() + "/ronda");
        assertThat(vistaCerrada.statusCode()).isEqualTo(200);
        Map<Long, Integer> votosPorId = new HashMap<>();
        for (JsonNode cand : JSON.readTree(vistaCerrada.body()).get("ronda").get("candidatos")) {
            votosPorId.put(cand.get("idUsuario").asLong(), cand.get("votos").asInt());
        }
        assertThat(votosPorId.get(esc.idB())).isEqualTo(2);
        assertThat(votosPorId.get(esc.idA())).isEqualTo(1);
        assertThat(votosPorId.get(esc.idC())).isEqualTo(0);

        int auditoriaSistema = jdbc.sql("SELECT count(*) FROM log_auditoria "
                        + "WHERE entidad_afectada = 'equipo' AND id_registro_afectado = :e "
                        + "AND campo_modificado = 'lider' AND valor_anterior = :ant AND valor_nuevo = :nue "
                        + "AND id_usuario_accion IS NULL")
                .param("e", esc.idEquipo())
                .param("ant", String.valueOf(esc.idA()))
                .param("nue", String.valueOf(esc.idB()))
                .query(Integer.class)
                .single();
        assertThat(auditoriaSistema).isEqualTo(1);
    }

    @Test
    void cambiarElVoto() {
        Escenario esc = escenarioConTres("cambio");
        long idRonda = abrirRondaApi(esc.docente(), esc.idEquipo());

        assertThat(votarApi(esc.navA(), esc.idEquipo(), esc.idB()).statusCode()).isEqualTo(200);
        assertThat(votarApi(esc.navA(), esc.idEquipo(), esc.idC()).statusCode()).isEqualTo(200);

        assertThat(jdbc.sql("SELECT id_candidato FROM voto_lider WHERE id_ronda = :r AND id_votante = :v")
                        .param("r", idRonda)
                        .param("v", esc.idA())
                        .query(Long.class)
                        .list())
                .containsExactly(esc.idC());

        assertThat(votarApi(esc.navB(), esc.idEquipo(), esc.idC()).statusCode()).isEqualTo(200);

        vencerRondaSql(esc.idEquipo());
        revisionRondas.cerrarVencidas();

        assertThat(jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", esc.idEquipo())
                        .query(Long.class)
                        .list())
                .containsExactly(esc.idC());
    }

    @Test
    void empateYDesempate() {
        Escenario esc = escenarioConTres("empate");
        abrirRondaApi(esc.docente(), esc.idEquipo());
        assertThat(votarApi(esc.navA(), esc.idEquipo(), esc.idB()).statusCode()).isEqualTo(200);
        assertThat(votarApi(esc.navB(), esc.idEquipo(), esc.idA()).statusCode()).isEqualTo(200);

        vencerRondaSql(esc.idEquipo());
        revisionRondas.cerrarVencidas();

        assertThat(jdbc.sql("SELECT resultado FROM ronda_lider WHERE id_equipo = :e")
                        .param("e", esc.idEquipo())
                        .query(String.class)
                        .single())
                .isEqualTo("EMPATE");
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", esc.idEquipo())
                        .query(Integer.class)
                        .single())
                .isZero();

        HttpResponse<String> vistaDoc = esc.docente().get("/api/equipos/" + esc.idEquipo() + "/ronda");
        assertThat(vistaDoc.statusCode()).isEqualTo(200);
        JsonNode raiz = JSON.readTree(vistaDoc.body());
        assertThat(raiz.get("puedeDesempatar").asBoolean()).isTrue();
        Map<Long, Boolean> empatados = new HashMap<>();
        for (JsonNode cand : raiz.get("ronda").get("candidatos")) {
            empatados.put(cand.get("idUsuario").asLong(), cand.get("empatado").asBoolean());
        }
        assertThat(empatados.get(esc.idA())).isTrue();
        assertThat(empatados.get(esc.idB())).isTrue();
        assertThat(empatados.get(esc.idC())).isFalse();

        HttpResponse<String> noEmpatado = esc.docente()
                .put("/api/equipos/" + esc.idEquipo() + "/ronda/desempate", "{\"idUsuario\":" + esc.idC() + "}");
        assertThat(noEmpatado.statusCode()).as(noEmpatado.body()).isEqualTo(409);
        assertThat(noEmpatado.body()).contains("\"codigo\":\"NO_ESTA_EMPATADO\"");

        assertThat(esc.navA()
                        .put("/api/equipos/" + esc.idEquipo() + "/ronda/desempate", "{\"idUsuario\":" + esc.idA() + "}")
                        .statusCode())
                .isEqualTo(403);

        HttpResponse<String> ok = esc.docente()
                .put("/api/equipos/" + esc.idEquipo() + "/ronda/desempate", "{\"idUsuario\":" + esc.idA() + "}");
        assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);
        assertThat(jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", esc.idEquipo())
                        .query(Long.class)
                        .list())
                .containsExactly(esc.idA());
        String estadoRonda = jdbc.sql(
                        "SELECT resultado || ':' || id_lider_elegido FROM ronda_lider WHERE id_equipo = :e")
                .param("e", esc.idEquipo())
                .query(String.class)
                .single();
        assertThat(estadoRonda).isEqualTo("EMPATE:" + esc.idA());

        HttpResponse<String> repetido = esc.docente()
                .put("/api/equipos/" + esc.idEquipo() + "/ronda/desempate", "{\"idUsuario\":" + esc.idB() + "}");
        assertThat(repetido.statusCode()).as(repetido.body()).isEqualTo(409);
        assertThat(repetido.body()).contains("\"codigo\":\"EMPATE_RESUELTO\"");

        long otroEquipo = crearEquipoApi(esc.docente(), esc.idProyecto(), "Sin Empate", esc.idCurso(), null);
        HttpResponse<String> sinEmpate = esc.docente()
                .put("/api/equipos/" + otroEquipo + "/ronda/desempate", "{\"idUsuario\":" + esc.idA() + "}");
        assertThat(sinEmpate.statusCode()).as(sinEmpate.body()).isEqualTo(409);
        assertThat(sinEmpate.body()).contains("\"codigo\":\"SIN_EMPATE\"");
    }

    @Test
    void sinVotosQuedaSinLider() {
        Escenario esc = escenarioConTres("sinvotos");
        abrirRondaApi(esc.docente(), esc.idEquipo());

        vencerRondaSql(esc.idEquipo());
        revisionRondas.cerrarVencidas();

        assertThat(jdbc.sql("SELECT resultado FROM ronda_lider WHERE id_equipo = :e")
                        .param("e", esc.idEquipo())
                        .query(String.class)
                        .single())
                .isEqualTo("SIN_VOTOS");
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", esc.idEquipo())
                        .query(Integer.class)
                        .single())
                .isZero();

        HttpResponse<String> vista = esc.docente().get("/api/equipos/" + esc.idEquipo() + "/ronda");
        assertThat(vista.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(vista.body()).get("puedeAbrir").asBoolean()).isTrue();

        abrirRondaApi(esc.docente(), esc.idEquipo());
        assertThat(jdbc.sql("SELECT count(*) FROM ronda_lider WHERE id_equipo = :e")
                        .param("e", esc.idEquipo())
                        .query(Integer.class)
                        .single())
                .isEqualTo(2);
        assertThat(jdbc.sql("SELECT count(*) FROM ronda_lider WHERE id_equipo = :e AND cerrada_en IS NULL")
                        .param("e", esc.idEquipo())
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void votoFueraDeRonda() {
        Escenario esc = escenarioConTres("fuera");

        HttpResponse<String> sinRonda = votarApi(esc.navA(), esc.idEquipo(), esc.idB());
        assertThat(sinRonda.statusCode()).as(sinRonda.body()).isEqualTo(409);
        assertThat(sinRonda.body()).contains("\"codigo\":\"SIN_RONDA_ABIERTA\"");

        abrirRondaApi(esc.docente(), esc.idEquipo());
        vencerRondaSql(esc.idEquipo());

        HttpResponse<String> vencida = votarApi(esc.navA(), esc.idEquipo(), esc.idB());
        assertThat(vencida.statusCode()).as(vencida.body()).isEqualTo(409);
        assertThat(vencida.body()).contains("\"codigo\":\"SIN_RONDA_ABIERTA\"");

        HttpResponse<String> vistaVencida = esc.navA().get("/api/equipos/" + esc.idEquipo() + "/ronda");
        assertThat(vistaVencida.statusCode()).isEqualTo(200);
        JsonNode ronda = JSON.readTree(vistaVencida.body()).get("ronda");
        assertThat(ronda.get("abierta").asBoolean()).isFalse();
        assertThat(ronda.get("resultado").isNull()).isTrue();

        assertThat(esc.docente()
                        .put("/api/equipos/" + esc.idEquipo() + "/lider", "{\"idUsuario\":" + esc.idA() + "}")
                        .statusCode())
                .isEqualTo(200);
        HttpResponse<String> cancelada = votarApi(esc.navA(), esc.idEquipo(), esc.idB());
        assertThat(cancelada.statusCode()).as(cancelada.body()).isEqualTo(409);
        assertThat(cancelada.body()).contains("\"codigo\":\"SIN_RONDA_ABIERTA\"");
    }

    @Test
    void soloIntegrantesVotanPorIntegrantes() {
        Escenario esc = escenarioConTres("permisos");
        long otroEquipo = crearEquipoApi(esc.docente(), esc.idProyecto(), "Otro Equipo", esc.idCurso(), null);
        String emailD = correoUnico("votd");
        long idD = crearUsuario("Diego Otro", emailD, "ESTUDIANTE");
        agregarAlCurso(esc.docente(), esc.idCurso(), emailD);
        assertThat(esc.docente()
                        .post("/api/equipos/" + otroEquipo + "/miembros", "{\"email\":\"" + emailD + "\"}")
                        .statusCode())
                .isEqualTo(201);
        Navegador navD = sesion(emailD);

        abrirRondaApi(esc.docente(), esc.idEquipo());

        assertThat(votarApi(esc.docente(), esc.idEquipo(), esc.idA()).statusCode())
                .isEqualTo(403);
        assertThat(votarApi(navD, esc.idEquipo(), esc.idA()).statusCode()).isEqualTo(403);
        assertThat(navD.get("/api/equipos/" + esc.idEquipo() + "/ronda").statusCode())
                .isEqualTo(403);

        HttpResponse<String> ajeno = votarApi(esc.navA(), esc.idEquipo(), idD);
        assertThat(ajeno.statusCode()).as(ajeno.body()).isEqualTo(409);
        assertThat(ajeno.body()).contains("\"codigo\":\"CANDIDATO_NO_VALIDO\"");

        HttpResponse<String> vistaDoc = esc.docente().get("/api/equipos/" + esc.idEquipo() + "/ronda");
        assertThat(vistaDoc.statusCode()).isEqualTo(200);
        JsonNode rondaDoc = JSON.readTree(vistaDoc.body()).get("ronda");
        assertThat(rondaDoc.get("miVoto").isNull()).isTrue();
        assertThat(rondaDoc.get("puedoVotar").asBoolean()).isFalse();
    }

    @Test
    void rondaDobleYPermisosDeApertura() {
        Escenario esc = escenarioConTres("doble");
        Navegador otroDocente = nuevoDocente();
        long idPrimera = abrirRondaApi(esc.docente(), esc.idEquipo());

        HttpResponse<String> segunda = esc.docente().post("/api/equipos/" + esc.idEquipo() + "/ronda", null);
        assertThat(segunda.statusCode()).as(segunda.body()).isEqualTo(409);
        assertThat(segunda.body()).contains("\"codigo\":\"RONDA_ABIERTA\"");

        assertThat(otroDocente
                        .post("/api/equipos/" + esc.idEquipo() + "/ronda", null)
                        .statusCode())
                .isEqualTo(403);
        assertThat(esc.navA()
                        .post("/api/equipos/" + esc.idEquipo() + "/ronda", null)
                        .statusCode())
                .isEqualTo(403);

        long vacio = crearEquipoApi(esc.docente(), esc.idProyecto(), "Vacio", esc.idCurso(), null);
        HttpResponse<String> sinIntegrantes = esc.docente().post("/api/equipos/" + vacio + "/ronda", null);
        assertThat(sinIntegrantes.statusCode()).as(sinIntegrantes.body()).isEqualTo(409);
        assertThat(sinIntegrantes.body()).contains("\"codigo\":\"EQUIPO_SIN_INTEGRANTES\"");

        vencerRondaSql(esc.idEquipo());
        long idNueva = abrirRondaApi(esc.docente(), esc.idEquipo());
        assertThat(idNueva).isNotEqualTo(idPrimera);
        assertThat(jdbc.sql("SELECT resultado FROM ronda_lider WHERE id_ronda = :r")
                        .param("r", idPrimera)
                        .query(String.class)
                        .single())
                .isEqualTo("SIN_VOTOS");
        assertThat(jdbc.sql("SELECT count(*) FROM ronda_lider WHERE id_equipo = :e AND cerrada_en IS NULL")
                        .param("e", esc.idEquipo())
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void destituirAbreRondaYNombrarLaCancela() {
        Escenario esc = escenarioConTres("destituir");
        assertThat(esc.docente()
                        .put("/api/equipos/" + esc.idEquipo() + "/lider", "{\"idUsuario\":" + esc.idA() + "}")
                        .statusCode())
                .isEqualTo(200);

        HttpResponse<String> dest = esc.docente().delete("/api/equipos/" + esc.idEquipo() + "/lider");
        assertThat(dest.statusCode()).as(dest.body()).isEqualTo(200);
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", esc.idEquipo())
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbc.sql("SELECT origen FROM ronda_lider WHERE id_equipo = :e AND cerrada_en IS NULL")
                        .param("e", esc.idEquipo())
                        .query(String.class)
                        .list())
                .containsExactly("DEMANDA");

        HttpResponse<String> sinLider = esc.docente().delete("/api/equipos/" + esc.idEquipo() + "/lider");
        assertThat(sinLider.statusCode()).as(sinLider.body()).isEqualTo(409);
        assertThat(sinLider.body()).contains("\"codigo\":\"SIN_LIDER\"");

        assertThat(esc.docente()
                        .put("/api/equipos/" + esc.idEquipo() + "/lider", "{\"idUsuario\":" + esc.idB() + "}")
                        .statusCode())
                .isEqualTo(200);
        assertThat(jdbc.sql("SELECT resultado FROM ronda_lider WHERE id_equipo = :e AND cerrada_en IS NOT NULL")
                        .param("e", esc.idEquipo())
                        .query(String.class)
                        .list())
                .containsExactly("CANCELADA");
        assertThat(jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", esc.idEquipo())
                        .query(Long.class)
                        .list())
                .containsExactly(esc.idB());
        HttpResponse<String> votoCancelada = votarApi(esc.navA(), esc.idEquipo(), esc.idB());
        assertThat(votoCancelada.statusCode()).isEqualTo(409);
        assertThat(votoCancelada.body()).contains("\"codigo\":\"SIN_RONDA_ABIERTA\"");

        long idRondaAbierta = abrirRondaApi(esc.docente(), esc.idEquipo());
        assertThat(esc.docente()
                        .delete("/api/equipos/" + esc.idEquipo() + "/lider")
                        .statusCode())
                .isEqualTo(200);
        assertThat(jdbc.sql("SELECT id_ronda FROM ronda_lider WHERE id_equipo = :e AND cerrada_en IS NULL")
                        .param("e", esc.idEquipo())
                        .query(Long.class)
                        .list())
                .containsExactly(idRondaAbierta);
    }

    @Test
    void quienSaleDejaDeContar() {
        Escenario esc = escenarioConTres("sale");
        abrirRondaApi(esc.docente(), esc.idEquipo());
        assertThat(votarApi(esc.navA(), esc.idEquipo(), esc.idB()).statusCode()).isEqualTo(200);
        assertThat(votarApi(esc.navB(), esc.idEquipo(), esc.idB()).statusCode()).isEqualTo(200);
        assertThat(votarApi(esc.navC(), esc.idEquipo(), esc.idA()).statusCode()).isEqualTo(200);

        assertThat(esc.docente()
                        .delete("/api/equipos/" + esc.idEquipo() + "/miembros/" + esc.idB())
                        .statusCode())
                .isEqualTo(200);

        vencerRondaSql(esc.idEquipo());
        revisionRondas.cerrarVencidas();

        assertThat(jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                        .param("e", esc.idEquipo())
                        .query(Long.class)
                        .list())
                .containsExactly(esc.idA());
        HttpResponse<String> vista = esc.docente().get("/api/equipos/" + esc.idEquipo() + "/ronda");
        assertThat(vista.statusCode()).isEqualTo(200);
        JsonNode ronda = JSON.readTree(vista.body()).get("ronda");
        assertThat(ronda.get("resultado").asString()).isEqualTo("LIDER_ELEGIDO");
        assertThat(ronda.get("votosEmitidos").asInt()).isEqualTo(1);
    }
}
