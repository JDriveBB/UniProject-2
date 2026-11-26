package co.uniproject.nucleo.configuracionacademica.cursos;

import static org.assertj.core.api.Assertions.assertThat;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Integración de cursos académicos, membresía, registro por curso, archivado, borrado y compañeros. */
class CursosIntegracionTest extends ApiIntegracionBase {

    private Navegador docente() {
        String email = correoUnico("doccurso");
        crearUsuario("Docente Curso", email, "DOCENTE");
        return sesion(email);
    }

    private long crearProyecto(Navegador docente, String nombre) {
        HttpResponse<String> r = docente.post(
                "/api/proyectos",
                "{\"nombre\":\"" + nombre + "\",\"fechaInicio\":\"2026-08-03\","
                        + "\"fechaFin\":\"2026-11-27\",\"usaMonitores\":false}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return numero(r.body(), "idProyecto");
    }

    private HttpResponse<String> registrar(String nombre, String email, String codigo) {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        return n.post(
                "/api/cuentas/registro",
                "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email + "\",\"contrasena\":\"" + CLAVE
                        + "\",\"autorizaTratamientoDatos\":true,\"codigo\":\"" + codigo + "\"}");
    }

    @Test
    void elDocenteCreaEditaYListaSusCursos() {
        Navegador docente = docente();
        HttpResponse<String> creado = docente.post(
                "/api/cursos",
                "{\"nombre\":\"  Diseño de Software  \",\"periodo\":\" 2026-2 \",\"descripcion\":\"  Grupo 1  \"}");
        assertThat(creado.statusCode()).as(creado.body()).isEqualTo(201);
        long id = numero(creado.body(), "idCurso");
        assertThat(creado.body())
                .contains("\"nombre\":\"Diseño de Software\"")
                .contains("\"periodo\":\"2026-2\"")
                .contains("\"descripcion\":\"Grupo 1\"")
                .contains("\"archivado\":false")
                .contains("\"nombreDocente\":\"Docente Curso\"")
                .contains("\"estudiantes\":0")
                .contains("\"proyectos\":[]");

        HttpResponse<String> editado = docente.put(
                "/api/cursos/" + id,
                "{\"nombre\":\"Diseño de Software I\",\"periodo\":\"2026-2\",\"descripcion\":\"Actualizado\"}");
        assertThat(editado.statusCode()).as(editado.body()).isEqualTo(200);
        assertThat(editado.body())
                .contains("\"nombre\":\"Diseño de Software I\"")
                .contains("\"descripcion\":\"Actualizado\"");

        HttpResponse<String> lista = docente.get("/api/cursos");
        assertThat(lista.statusCode()).as(lista.body()).isEqualTo(200);
        assertThat(tieneValor(lista.body(), "idCurso", id)).isTrue();

        HttpResponse<String> repetido =
                docente.post("/api/cursos", "{\"nombre\":\"DISEÑO DE SOFTWARE I\",\"periodo\":\"2026-2\"}");
        assertThat(repetido.statusCode()).as(repetido.body()).isEqualTo(409);
        assertThat(repetido.body()).contains("\"codigo\":\"CURSO_REPETIDO\"");

        HttpResponse<String> sinNombre = docente.post("/api/cursos", "{\"nombre\":\"   \",\"periodo\":\"2026-2\"}");
        assertThat(sinNombre.statusCode()).as(sinNombre.body()).isEqualTo(400);
        assertThat(sinNombre.body()).contains("\"codigo\":\"VALIDACION\"");

        HttpResponse<String> sinPeriodo = docente.post("/api/cursos", "{\"nombre\":\"Valido\"}");
        assertThat(sinPeriodo.statusCode()).as(sinPeriodo.body()).isEqualTo(400);
        assertThat(sinPeriodo.body()).contains("\"codigo\":\"VALIDACION\"");
    }

    @Test
    void soloElDocenteDuenoVeYCambiaSuCurso() {
        Navegador docenteA = docente();
        Navegador docenteB = docente();
        long idCurso = crearCurso(docenteA, "Curso de A");

        assertThat(tieneValor(docenteB.get("/api/cursos").body(), "idCurso", idCurso))
                .isFalse();
        assertThat(docenteB.get("/api/cursos/" + idCurso).statusCode()).isEqualTo(403);
        assertThat(docenteB.put("/api/cursos/" + idCurso, "{\"nombre\":\"X\",\"periodo\":\"2026-2\"}")
                        .statusCode())
                .isEqualTo(403);
        assertThat(docenteB.delete("/api/cursos/" + idCurso).statusCode()).isEqualTo(403);
        assertThat(docenteB.post("/api/cursos/" + idCurso + "/correos", "{\"emails\":[\"x@u.edu.co\"]}")
                        .statusCode())
                .isEqualTo(403);
        assertThat(docenteB.put("/api/cursos/" + idCurso + "/archivado", "{\"archivado\":true}")
                        .statusCode())
                .isEqualTo(403);

        assertThat(docenteA.get("/api/cursos/999999999").statusCode()).isEqualTo(403);

        String est = correoUnico("est");
        crearUsuario("Est", est, "ESTUDIANTE");
        String mon = correoUnico("mon");
        crearUsuario("Mon", mon, "MONITOR");
        assertThat(sesion(est)
                        .post("/api/cursos", "{\"nombre\":\"Pirata\",\"periodo\":\"2026-2\"}")
                        .statusCode())
                .isEqualTo(403);
        assertThat(sesion(mon)
                        .post("/api/cursos", "{\"nombre\":\"Pirata\",\"periodo\":\"2026-2\"}")
                        .statusCode())
                .isEqualTo(403);

        Navegador anonimo = new Navegador(puerto);
        assertThat(anonimo.get("/api/cursos").statusCode()).isEqualTo(401);
        assertThat(anonimo.get("/api/cursos/" + idCurso).statusCode()).isEqualTo(401);
    }

    @Test
    void agregarCorreosCuentasExistentesEntranYLasDemasQuedanPendientes() {
        Navegador docente = docente();
        long idCurso = crearCurso(docente, "Curso Correos");

        String existente = correoUnico("estex");
        crearUsuario("Ana Existente", existente, "ESTUDIANTE");
        String nuevo = correoUnico("estnew");
        String otroDoc = correoUnico("otrodoc");
        crearUsuario("Otro Docente", otroDoc, "DOCENTE");

        HttpResponse<String> primera = docente.post(
                "/api/cursos/" + idCurso + "/correos",
                "{\"emails\":[\"" + existente.toUpperCase(Locale.ROOT) + "\",\"" + existente + "\",\"" + nuevo + "\",\""
                        + nuevo.toUpperCase(Locale.ROOT) + "\",\"" + otroDoc + "\"]}");
        assertThat(primera.statusCode()).as(primera.body()).isEqualTo(201);
        assertThat(primera.body())
                .contains("\"agregados\":[\"" + existente + "\"]")
                .contains("\"email\":\"" + nuevo + "\"")
                .contains("\"yaEnCurso\":[]")
                .contains("\"conOtroRol\":[\"" + otroDoc + "\"]");

        HttpResponse<String> detalle = docente.get("/api/cursos/" + idCurso);
        assertThat(detalle.statusCode()).as(detalle.body()).isEqualTo(200);
        assertThat(detalle.body())
                .contains("\"estudiantes\":1")
                .contains("\"nombre\":\"Ana Existente\"")
                .contains("\"email\":\"" + nuevo + "\"");

        HttpResponse<String> segunda = docente.post(
                "/api/cursos/" + idCurso + "/correos", "{\"emails\":[\"" + existente + "\",\"" + nuevo + "\"]}");
        assertThat(segunda.statusCode()).as(segunda.body()).isEqualTo(201);
        assertThat(segunda.body())
                .contains("\"agregados\":[]")
                .contains("\"habilitados\":[]")
                .contains(existente)
                .contains(nuevo);

        HttpResponse<String> reservado =
                docente.post("/api/cursos/" + idCurso + "/correos", "{\"emails\":[\"anonimo-1@anonimo.invalid\"]}");
        assertThat(reservado.statusCode()).as(reservado.body()).isEqualTo(400);
        assertThat(reservado.body()).contains("\"codigo\":\"CORREO_RESERVADO\"");
    }

    @Test
    void elEnlaceDelCursoCreaLaCuentaYLaMeteAlCursoYASusProyectos() {
        Navegador docente = docente();
        long curso1 = crearCurso(docente, "Curso Registro 1");
        long curso2 = crearCurso(docente, "Curso Registro 2");
        long p1 = crearProyecto(docente, "Proyecto 1");
        long p2 = crearProyecto(docente, "Proyecto 2");
        asignarCursos(docente, p1, curso1);
        asignarCursos(docente, p2, curso1);

        String email = correoUnico("regcurso");
        agregarAlCurso(docente, curso1, email);
        agregarAlCurso(docente, curso2, email);
        long idCorreo1 = jdbc.sql(
                        "SELECT id_correo_habilitado FROM correo_habilitado WHERE id_curso = :c AND email = :e")
                .param("c", curso1)
                .param("e", email)
                .query(Long.class)
                .single();

        HttpResponse<String> rEnlace =
                docente.post("/api/cursos/" + curso1 + "/correos/" + idCorreo1 + "/enlace", null);
        assertThat(rEnlace.statusCode()).as(rEnlace.body()).isEqualTo(200);
        assertThat(rEnlace.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        String codigo = campoTexto(rEnlace.body(), "codigo");

        HttpResponse<String> alta = registrar("Camila Registro", email, codigo);
        assertThat(alta.statusCode()).as(alta.body()).isEqualTo(201);
        assertThat(alta.body()).contains("\"rol\":\"ESTUDIANTE\"");
        long idUsuario = numero(alta.body(), "idUsuario");

        assertThat(jdbc.sql("SELECT id_curso FROM miembro_curso WHERE id_usuario = :u ORDER BY id_curso")
                        .param("u", idUsuario)
                        .query(Long.class)
                        .list())
                .containsExactly(curso1, curso2);
        assertThat(jdbc.sql("SELECT id_proyecto FROM participante_proyecto WHERE id_usuario = :u AND id_curso = :c "
                                + "ORDER BY id_proyecto")
                        .param("u", idUsuario)
                        .param("c", curso1)
                        .query(Long.class)
                        .list())
                .containsExactly(p1, p2);

        HttpResponse<String> enlaceOtraVez =
                docente.post("/api/cursos/" + curso1 + "/correos/" + idCorreo1 + "/enlace", null);
        assertThat(enlaceOtraVez.statusCode()).as(enlaceOtraVez.body()).isEqualTo(409);
        assertThat(enlaceOtraVez.body()).contains("\"codigo\":\"HABILITACION_USADA\"");
    }

    @Test
    void retirarUnCorreoPendienteDelCurso() {
        Navegador docente = docente();
        Navegador otro = docente();
        long idCurso = crearCurso(docente, "Curso Retiro");
        String pend = correoUnico("pend");
        String usado = correoUnico("usado");
        agregarAlCurso(docente, idCurso, pend, usado);

        long idPend = jdbc.sql("SELECT id_correo_habilitado FROM correo_habilitado WHERE id_curso = :c AND email = :e")
                .param("c", idCurso)
                .param("e", pend)
                .query(Long.class)
                .single();
        long idUsado = jdbc.sql("SELECT id_correo_habilitado FROM correo_habilitado WHERE id_curso = :c AND email = :e")
                .param("c", idCurso)
                .param("e", usado)
                .query(Long.class)
                .single();

        assertThat(otro.delete("/api/cursos/" + idCurso + "/correos/" + idPend).statusCode())
                .isEqualTo(403);
        assertThat(docente.delete("/api/cursos/" + idCurso + "/correos/" + idPend)
                        .statusCode())
                .isEqualTo(204);
        assertThat(docente.get("/api/cursos/" + idCurso).body()).doesNotContain(pend);
        assertThat(docente.delete("/api/cursos/" + idCurso + "/correos/" + idPend)
                        .statusCode())
                .isEqualTo(404);

        String codigo = enlaceCurso(docente, idCurso, usado);
        assertThat(registrar("Usado", usado, codigo).statusCode()).isEqualTo(201);
        HttpResponse<String> delUsado = docente.delete("/api/cursos/" + idCurso + "/correos/" + idUsado);
        assertThat(delUsado.statusCode()).as(delUsado.body()).isEqualTo(409);
        assertThat(delUsado.body()).contains("\"codigo\":\"HABILITACION_USADA\"");
    }

    @Test
    void archivarYDesarchivar() {
        Navegador docente = docente();
        long idCurso = crearCurso(docente, "Curso Archivable");
        long pExistente = crearProyecto(docente, "Proyecto Existente");
        long pNuevo = crearProyecto(docente, "Proyecto Nuevo");
        String estEmail = correoUnico("estarch");
        long idEst = crearUsuario("Est Arch", estEmail, "ESTUDIANTE");
        String pendEmail = correoUnico("pendarch");
        agregarAlCurso(docente, idCurso, estEmail, pendEmail);
        asignarCursos(docente, pExistente, idCurso);
        long idPend = jdbc.sql("SELECT id_correo_habilitado FROM correo_habilitado WHERE id_curso = :c AND email = :e")
                .param("c", idCurso)
                .param("e", pendEmail)
                .query(Long.class)
                .single();

        HttpResponse<String> arch = docente.put("/api/cursos/" + idCurso + "/archivado", "{\"archivado\":true}");
        assertThat(arch.statusCode()).as(arch.body()).isEqualTo(200);
        assertThat(arch.body()).contains("\"archivado\":true");

        HttpResponse<String> add =
                docente.post("/api/cursos/" + idCurso + "/correos", "{\"emails\":[\"nuevo@u.edu.co\"]}");
        assertThat(add.statusCode()).isEqualTo(409);
        assertThat(add.body()).contains("\"codigo\":\"CURSO_ARCHIVADO\"");

        HttpResponse<String> edit =
                docente.put("/api/cursos/" + idCurso, "{\"nombre\":\"Otro\",\"periodo\":\"2026-2\"}");
        assertThat(edit.statusCode()).isEqualTo(409);
        assertThat(edit.body()).contains("\"codigo\":\"CURSO_ARCHIVADO\"");

        HttpResponse<String> enl = docente.post("/api/cursos/" + idCurso + "/correos/" + idPend + "/enlace", null);
        assertThat(enl.statusCode()).isEqualTo(409);
        assertThat(enl.body()).contains("\"codigo\":\"CURSO_ARCHIVADO\"");

        HttpResponse<String> asignNuevo = docente.put(
                "/api/proyectos/" + pNuevo + "/cursos", "{\"idsCursos\":[" + idCurso + "],\"confirmar\":true}");
        assertThat(asignNuevo.statusCode()).isEqualTo(409);
        assertThat(asignNuevo.body()).contains("\"codigo\":\"CURSO_ARCHIVADO\"");

        // El proyecto que ya lo tenía lo conserva y sus participantes no cambian.
        assertThat(tieneValor(
                        docente.get("/api/proyectos/" + pExistente + "/cursos").body(), "idCurso", idCurso))
                .isTrue();
        assertThat(jdbc.sql("SELECT count(*) FROM participante_proyecto WHERE id_proyecto = :p AND id_usuario = :u")
                        .param("p", pExistente)
                        .param("u", idEst)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);

        // Quitar un estudiante sí funciona con el curso archivado.
        assertThat(docente.delete("/api/cursos/" + idCurso + "/estudiantes/" + idEst)
                        .statusCode())
                .isEqualTo(204);

        HttpResponse<String> desarch = docente.put("/api/cursos/" + idCurso + "/archivado", "{\"archivado\":false}");
        assertThat(desarch.statusCode()).isEqualTo(200);
        assertThat(desarch.body()).contains("\"archivado\":false");
        assertThat(docente.put("/api/cursos/" + idCurso, "{\"nombre\":\"Curso Reactivado\",\"periodo\":\"2026-2\"}")
                        .statusCode())
                .isEqualTo(200);
    }

    @Test
    void soloSeBorraUnCursoQueNoSeUso() {
        Navegador docente = docente();
        long sinUso = crearCurso(docente, "Curso Sin Uso");
        String est = correoUnico("estdel");
        crearUsuario("Est Del", est, "ESTUDIANTE");
        agregarAlCurso(docente, sinUso, est, correoUnico("penddel"));

        assertThat(docente.delete("/api/cursos/" + sinUso).statusCode()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT count(*) FROM curso WHERE id_curso = :c")
                        .param("c", sinUso)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM miembro_curso WHERE id_curso = :c")
                        .param("c", sinUso)
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_curso = :c")
                        .param("c", sinUso)
                        .query(Integer.class)
                        .single())
                .isZero();

        long usado = crearCurso(docente, "Curso En Uso");
        long p = crearProyecto(docente, "Proyecto Uso");
        agregarAlCurso(docente, usado, est, correoUnico("penduso"));
        asignarCursos(docente, p, usado);

        HttpResponse<String> borrarUsado = docente.delete("/api/cursos/" + usado);
        assertThat(borrarUsado.statusCode()).as(borrarUsado.body()).isEqualTo(409);
        assertThat(borrarUsado.body()).contains("\"codigo\":\"CURSO_EN_USO\"");
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE id_curso = :c")
                        .param("c", usado)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void elEstudianteVeSusCursosYLosNombresDeSusCompaneros() {
        Navegador docente = docente();
        long cursoA = crearCurso(docente, "Curso Propio");
        long cursoAjeno = crearCurso(docente, "Curso Ajeno");
        long p = crearProyecto(docente, "Proyecto Visible");
        asignarCursos(docente, p, cursoA);

        String est1 = correoUnico("comp1");
        String est2 = correoUnico("comp2");
        String estSup = correoUnico("compsup");
        crearUsuario("Ana Compañera", est1, "ESTUDIANTE");
        crearUsuario("Bruno Compañero", est2, "ESTUDIANTE");
        crearUsuario("Carlos Suprimido", estSup, "ESTUDIANTE");
        agregarAlCurso(docente, cursoA, est1, est2, estSup);

        assertThat(sesion(estSup)
                        .post("/api/perfil/supresion", "{\"contrasena\":\"" + CLAVE + "\"}")
                        .statusCode())
                .isEqualTo(204);

        Navegador ana = sesion(est1);
        HttpResponse<String> lista = ana.get("/api/cursos");
        assertThat(lista.statusCode()).as(lista.body()).isEqualTo(200);
        assertThat(tieneValor(lista.body(), "idCurso", cursoA)).isTrue();
        assertThat(tieneValor(lista.body(), "idCurso", cursoAjeno)).isFalse();
        assertThat(lista.body()).contains("Proyecto Visible").contains("\"nombreDocente\":\"Docente Curso\"");

        HttpResponse<String> comp = ana.get("/api/cursos/" + cursoA + "/companeros");
        assertThat(comp.statusCode()).as(comp.body()).isEqualTo(200);
        assertThat(comp.body())
                .contains("Ana Compañera")
                .contains("Bruno Compañero")
                .doesNotContain("Carlos Suprimido")
                .doesNotContain("@");

        assertThat(ana.get("/api/cursos/" + cursoA).statusCode()).isEqualTo(403);
        assertThat(ana.get("/api/cursos/" + cursoAjeno + "/companeros").statusCode())
                .isEqualTo(403);
    }

    @Test
    void laConfiguracionDelCursoQuedaAuditadaSinCorreos() {
        Navegador docente = docente();
        long idCurso = crearCurso(docente, "Curso Auditado");
        String est = correoUnico("estaud");
        crearUsuario("Est Aud", est, "ESTUDIANTE");
        agregarAlCurso(docente, idCurso, est, correoUnico("pendaud"));

        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'curso' "
                                + "AND id_registro_afectado = :c AND accion = 'CREAR'")
                        .param("c", idCurso)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'miembro_curso' "
                                + "AND id_registro_afectado = :c AND accion = 'CREAR'")
                        .param("c", idCurso)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'correo_habilitado' "
                                + "AND accion = 'HABILITAR_CORREO' AND id_registro_afectado IN "
                                + "(SELECT id_correo_habilitado FROM correo_habilitado WHERE id_curso = :c)")
                        .param("c", idCurso)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE entidad_afectada IN "
                                + "('curso', 'miembro_curso', 'correo_habilitado') "
                                + "AND coalesce(valor_anterior, '') || coalesce(valor_nuevo, '') LIKE '%@%'")
                        .query(Integer.class)
                        .single())
                .isZero();
    }
}
