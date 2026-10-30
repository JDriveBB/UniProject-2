package co.uniproject.nucleo.evaluacion;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluacionIntegracionTest extends ApiIntegracionBase {

    private record Escenario(long idHitoEquipo, long idDocente, String docente, String lider, String integrante,
                             String otro, long idIntegrante, long idOtro, long criterio1, long criterio2,
                             long tareaOtro) {}

    private Escenario escenario() {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Evaluacion", docente, "DOCENTE");
        String lider = correoUnico("lider");
        long idLider = crearUsuario("Lider Eval", lider, "ESTUDIANTE");
        String integrante = correoUnico("int");
        long idIntegrante = crearUsuario("Integrante Uno", integrante, "ESTUDIANTE");
        String otro = correoUnico("otro");
        long idOtro = crearUsuario("Integrante Dos", otro, "ESTUDIANTE");
        long proyecto = jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Evaluacion', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(30)).param("f", hoy.plusDays(60))
                .query(Long.class).single();
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'Hito 1', 1, :i, :f) RETURNING id_hito")
                .param("p", proyecto).param("i", hoy.minusDays(10)).param("f", hoy.plusDays(20))
                .query(Long.class).single();
        long c1 = criterio(hito, "Diseño", 60, 5);
        long c2 = criterio(hito, "Código", 40, 10);
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo V') "
                        + "RETURNING id_equipo")
                .param("p", proyecto).query(Long.class).single();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :l, 'LIDER'), "
                        + "(:e, :a, 'INTEGRANTE'), (:e, :b, 'INTEGRANTE')")
                .param("e", equipo).param("l", idLider).param("a", idIntegrante).param("b", idOtro).update();
        long he = jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) VALUES (:h, :e) RETURNING id_hito_equipo")
                .param("h", hito).param("e", equipo).query(Long.class).single();
        completada(he, idLider, "Arquitectura", 6);
        completada(he, idIntegrante, "Modelo", 3);
        long tareaOtro = jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                        + "VALUES (:he, :u, 'Pruebas', 1) RETURNING id_tarea")
                .param("he", he).param("u", idOtro).query(Long.class).single();
        return new Escenario(he, idDocente, docente, lider, integrante, otro, idIntegrante, idOtro, c1, c2,
                tareaOtro);
    }

    private long criterio(long hito, String descripcion, int peso, int maximo) {
        return jdbc.sql("INSERT INTO criterio_rubrica (id_hito, descripcion, peso_porcentaje, puntaje_maximo) "
                        + "VALUES (:h, :d, :p, :m) RETURNING id_criterio")
                .param("h", hito).param("d", descripcion).param("p", peso).param("m", maximo)
                .query(Long.class).single();
    }

    private void completada(long he, long encargado, String titulo, int horas) {
        jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas, estado, porcentaje_avance, "
                        + "fecha_completada) VALUES (:he, :u, :t, :h, 'COMPLETADA', 100, now())")
                .param("he", he).param("u", encargado).param("t", titulo).param("h", horas).update();
    }

    private static String envio(Escenario e) {
        return "/api/hitos-equipo/" + e.idHitoEquipo() + "/envio";
    }

    private static String evaluacion(Escenario e) {
        return "/api/hitos-equipo/" + e.idHitoEquipo() + "/evaluacion";
    }

    private static String calificar(Escenario e, String nota1, String nota2, String extra) {
        return "{\"criterios\":[{\"idCriterio\":" + e.criterio1() + ",\"nota\":" + nota1 + "},{\"idCriterio\":"
                + e.criterio2() + ",\"nota\":" + nota2 + "}]" + extra + "}";
    }

    private void enviar(Escenario e) {
        HttpResponse<String> r = sesion(e.lider()).post(envio(e), "{}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
    }

    private int contar(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(Integer.class).single();
    }

    private String notas(long idHitoEquipo) {
        return jdbc.sql("SELECT string_agg(u.nombre || '=' || ni.factor_ajuste || '/' || ni.nota_sugerida, ';' "
                        + "ORDER BY u.nombre) FROM nota_individual ni JOIN usuario u ON u.id_usuario = ni.id_estudiante "
                        + "JOIN evaluacion_hito ev ON ev.id_evaluacion = ni.id_evaluacion "
                        + "JOIN envio_hito en ON en.id_envio = ev.id_envio WHERE en.id_hito_equipo = :he")
                .param("he", idHitoEquipo).query(String.class).single();
    }

    @Test
    void elEnvioExigePrerrequisitosCriticosYAvisaAlDocente() {
        Escenario e = escenario();
        jdbc.sql("UPDATE tarea SET es_prerrequisito_critico = true WHERE id_tarea = :t")
                .param("t", e.tareaOtro()).update();
        HttpResponse<String> bloqueado = sesion(e.otro()).post(envio(e), "{}");
        assertThat(bloqueado.statusCode()).isEqualTo(409);
        assertThat(bloqueado.body()).contains("\"codigo\":\"PRERREQUISITOS_INCOMPLETOS\"").contains("Pruebas");
        jdbc.sql("UPDATE tarea SET estado = 'COMPLETADA', porcentaje_avance = 100, fecha_completada = now() "
                + "WHERE id_tarea = :t").param("t", e.tareaOtro()).update();
        assertThat(sesion(e.docente()).post(envio(e), "{}").statusCode()).isEqualTo(403);
        HttpResponse<String> r = sesion(e.otro()).post(envio(e), "{}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        assertThat(r.body()).contains("\"enviadoPor\":\"Integrante Dos\"");
        long he = e.idHitoEquipo();
        assertThat(jdbc.sql("SELECT estado FROM hito_equipo WHERE id_hito_equipo = :id").param("id", he)
                .query(String.class).single()).isEqualTo("ENVIADO");
        assertThat(contar("SELECT count(*) FROM tarea WHERE id_hito_equipo = :id AND edicion_habilitada", he)).isZero();
        assertThat(contar("SELECT count(*) FROM evento_saliente WHERE tipo = 'hito.enviado' "
                + "AND (carga->'data'->'hito'->>'id')::bigint = :id", he)).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM notificacion WHERE tipo = 'hito.enviado' AND id_destinatario = :d "
                        + "AND id_referencia = :he")
                .param("d", e.idDocente()).param("he", he).query(Integer.class).single()).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'hito_equipo' "
                + "AND id_registro_afectado = :id AND accion = 'ENVIAR_HITO'", he)).isEqualTo(1);
        HttpResponse<String> otraVez = sesion(e.lider()).post(envio(e), "{}");
        assertThat(otraVez.statusCode()).isEqualTo(409);
        assertThat(otraVez.body()).contains("\"codigo\":\"HITO_NO_ENVIABLE\"");
        assertThat(sesion(e.lider()).put("/api/tareas/" + e.tareaOtro() + "/avance", "{\"porcentajeAvance\":50}")
                .statusCode()).isEqualTo(409);
    }

    @Test
    void laEvaluacionCalculaLaNotaGrupalYElIce() {
        Escenario e = escenario();
        enviar(e);
        HttpResponse<String> r = sesion(e.docente()).post(evaluacion(e), calificar(e, "4", "8", ""));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        assertThat(r.body()).contains("\"notaGrupal\":4.00").contains("\"notaCalculada\":4.00")
                .contains("\"caracter\":\"PROVISIONAL\"");
        long he = e.idHitoEquipo();
        assertThat(notas(he)).isEqualTo("Integrante Dos=0.0000/2.00;Integrante Uno=1.0000/4.00;Lider Eval=2.0000/5.00");
        assertThat(jdbc.sql("SELECT estado FROM hito_equipo WHERE id_hito_equipo = :id").param("id", he)
                .query(String.class).single()).isEqualTo("EVALUADO");
        assertThat(contar("SELECT count(*) FROM tarea WHERE id_hito_equipo = :id AND NOT edicion_habilitada", he))
                .isZero();
        assertThat(contar("SELECT count(*) FROM evento_saliente WHERE tipo = 'hito.evaluado' "
                + "AND (carga->'data'->'hito'->>'id')::bigint = :id", he)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM notificacion WHERE tipo = 'hito.evaluado' AND id_referencia = :id", he))
                .isEqualTo(3);
        assertThat(sesion(e.docente()).get(evaluacion(e)).body()).contains("Integrante Dos").contains("Lider Eval");
        HttpResponse<String> repetida = sesion(e.docente()).post(evaluacion(e), calificar(e, "4", "8", ""));
        assertThat(repetida.statusCode()).isEqualTo(409);
        assertThat(repetida.body()).contains("\"codigo\":\"HITO_NO_ENVIADO\"");
    }

    @Test
    void elDocenteAjustaLaNotaGrupalAntesDeConfirmar() {
        Escenario e = escenario();
        enviar(e);
        Navegador docente = sesion(e.docente());
        HttpResponse<String> r = docente.post(evaluacion(e), calificar(e, "4", "8", ",\"notaGrupal\":4.5"));
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        assertThat(r.body()).contains("\"notaGrupal\":4.50").contains("\"notaCalculada\":4.00");
        long he = e.idHitoEquipo();
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'hito_equipo' "
                + "AND id_registro_afectado = :id AND accion = 'ACTUALIZAR' AND campo_modificado = 'nota_grupal' "
                + "AND valor_anterior = '4.00' AND valor_nuevo = '4.50'", he)).isEqualTo(1);
        long ev = numero(r.body(), "idEvaluacion");
        HttpResponse<String> cambio = docente.put("/api/evaluaciones/" + ev + "/nota-grupal", "{\"notaGrupal\":3}");
        assertThat(cambio.statusCode()).as(cambio.body()).isEqualTo(200);
        assertThat(cambio.body()).contains("\"notaGrupal\":3.00");
        assertThat(notas(he)).isEqualTo("Integrante Dos=0.0000/1.50;Integrante Uno=1.0000/3.00;Lider Eval=2.0000/4.50");
        String confirmar = "/api/evaluaciones/" + ev + "/notas/" + e.idIntegrante();
        assertThat(sesion(e.lider()).put(confirmar, "{\"notaFinal\":3.2}").statusCode()).isEqualTo(403);
        HttpResponse<String> confirmada = docente.put(confirmar, "{\"notaFinal\":3.2}");
        assertThat(confirmada.statusCode()).as(confirmada.body()).isEqualTo(200);
        assertThat(confirmada.body()).contains("\"notaFinal\":3.20");
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'hito_equipo' "
                + "AND id_registro_afectado = :id AND accion = 'CONFIRMAR_NOTA' AND campo_modificado = 'nota_final:"
                + e.idIntegrante() + "'", he)).isEqualTo(1);
        HttpResponse<String> tarde = docente.put("/api/evaluaciones/" + ev + "/nota-grupal", "{\"notaGrupal\":4}");
        assertThat(tarde.statusCode()).isEqualTo(409);
        assertThat(tarde.body()).contains("\"codigo\":\"NOTAS_CONFIRMADAS\"");
        assertThat(docente.put("/api/evaluaciones/" + ev + "/notas/999999999", "{\"notaFinal\":3}").statusCode())
                .isEqualTo(404);
    }

    @Test
    void validaLaRubricaYQuienEvalua() {
        Escenario e = escenario();
        Navegador docente = sesion(e.docente());
        HttpResponse<String> sinEnvio = docente.post(evaluacion(e), calificar(e, "4", "8", ""));
        assertThat(sinEnvio.statusCode()).isEqualTo(409);
        assertThat(sinEnvio.body()).contains("\"codigo\":\"HITO_NO_ENVIADO\"");
        enviar(e);
        assertThat(sesion(e.lider()).post(evaluacion(e), calificar(e, "4", "8", "")).statusCode()).isEqualTo(403);
        String otroDocente = correoUnico("doc2");
        crearUsuario("Otro Docente", otroDocente, "DOCENTE");
        assertThat(sesion(otroDocente).post(evaluacion(e), calificar(e, "4", "8", "")).statusCode()).isEqualTo(403);
        HttpResponse<String> incompleta = docente.post(evaluacion(e),
                "{\"criterios\":[{\"idCriterio\":" + e.criterio1() + ",\"nota\":4}]}");
        assertThat(incompleta.statusCode()).isEqualTo(400);
        assertThat(incompleta.body()).contains("\"codigo\":\"RUBRICA_INCOMPLETA\"");
        HttpResponse<String> fuera = docente.post(evaluacion(e), calificar(e, "6", "8", ""));
        assertThat(fuera.statusCode()).isEqualTo(400);
        assertThat(fuera.body()).contains("\"codigo\":\"NOTA_FUERA_DE_RANGO\"");
        assertThat(docente.post(evaluacion(e), calificar(e, "4", "8", ",\"notaGrupal\":5.5")).statusCode())
                .isEqualTo(400);
        jdbc.sql("UPDATE criterio_rubrica SET peso_porcentaje = 50 WHERE id_criterio = :c")
                .param("c", e.criterio1()).update();
        HttpResponse<String> pesos = docente.post(evaluacion(e), calificar(e, "4", "8", ""));
        assertThat(pesos.statusCode()).isEqualTo(409);
        assertThat(pesos.body()).contains("\"codigo\":\"PESOS_NO_SUMAN_100\"");
        assertThat(contar("SELECT count(*) FROM evaluacion_hito ev JOIN envio_hito en ON en.id_envio = ev.id_envio "
                + "WHERE en.id_hito_equipo = :id", e.idHitoEquipo())).isZero();
    }

    @Test
    void cadaUnoVeLoQueLeCorresponde() {
        Escenario e = escenario();
        enviar(e);
        Navegador docente = sesion(e.docente());
        long ev = numero(docente.post(evaluacion(e), calificar(e, "4", "8", "")).body(), "idEvaluacion");
        assertThat(docente.put("/api/evaluaciones/" + ev + "/notas/" + e.idIntegrante(), "{\"notaFinal\":3.2}")
                .statusCode()).isEqualTo(200);
        HttpResponse<String> propio = sesion(e.integrante()).get(evaluacion(e));
        assertThat(propio.statusCode()).isEqualTo(200);
        assertThat(propio.body()).contains("\"notaGrupal\":4.00").contains("\"notaCalculada\":null")
                .contains("\"nombre\":\"Integrante Uno\",\"factorAjuste\":null,\"notaSugerida\":null,\"notaFinal\":3.20")
                .doesNotContain("Integrante Dos").doesNotContain("Lider Eval");
        assertThat(sesion(e.otro()).get(evaluacion(e)).body())
                .contains("\"nombre\":\"Integrante Dos\",\"factorAjuste\":null,\"notaSugerida\":null,\"notaFinal\":null")
                .doesNotContain("Integrante Uno");
        String ajeno = correoUnico("ajeno");
        crearUsuario("Ajeno", ajeno, "ESTUDIANTE");
        assertThat(sesion(ajeno).get(evaluacion(e)).statusCode()).isEqualTo(403);
        Escenario sinNota = escenario();
        assertThat(sesion(sinNota.docente()).get(evaluacion(sinNota)).statusCode()).isEqualTo(404);
    }
}
