package co.uniproject.nucleo.seguridad;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.datos.CaracterNota;
import co.uniproject.eventos.datos.EquipoInactividad;
import co.uniproject.eventos.datos.HitoEvaluado;
import co.uniproject.eventos.datos.Persona;
import co.uniproject.eventos.datos.Referencia;
import co.uniproject.eventos.datos.ReferenciaTarea;
import co.uniproject.eventos.datos.TareaReasignada;
import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.notificaciones.RegistroEventos;
import co.uniproject.nucleo.seguridad.cuentas.ServicioSupresion;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Bloque 27: derechos del titular (Ley 1581) desde el perfil: conocer, rectificar el nombre y suprimir la cuenta. */
@ExtendWith(OutputCaptureExtension.class)
class DerechosTitularIntegracionTest extends ApiIntegracionBase {

    private static final String ANONIMO = ServicioSupresion.NOMBRE_ANONIMO;

    @Autowired
    private RegistroEventos eventos;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private CodecEventos codec;

    @Autowired
    private ServicioSupresion supresion;

    @Value("${uniproject.recuperacion.maximo-cambios-por-cuenta:10}")
    private int maximoCambios;

    @BeforeEach
    void sinLimites() {
        jdbc.sql("DELETE FROM limite_recuperacion").update();
    }

    private record Escenario(long idProyecto, long idEquipo, long idHitoEquipo, long idDocente, String docente,
                             long idLider, String lider, long idEstudiante, String estudiante, String nombre,
                             long idTarea) {}

    /** Proyecto activo, equipo (líder + estudiante) y una tarea del estudiante. El nombre del estudiante es único. */
    private Escenario escenario() {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        String docente = correoUnico("docsup");
        long idDocente = crearUsuario("Docente Supresion", docente, "DOCENTE");
        String lider = correoUnico("lidersup");
        long idLider = crearUsuario("Lider Supresion", lider, "ESTUDIANTE");
        String estudiante = correoUnico("estsup");
        String nombre = "Paula " + UUID.randomUUID().toString().substring(0, 8);
        long idEstudiante = crearUsuario(nombre, estudiante, "ESTUDIANTE");
        long proyecto = proyecto(idDocente, hoy);
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'Hito 1', 1, :i, :f) RETURNING id_hito")
                .param("p", proyecto).param("i", hoy.minusDays(10)).param("f", hoy.plusDays(20))
                .query(Long.class).single();
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo S') "
                        + "RETURNING id_equipo")
                .param("p", proyecto).query(Long.class).single();
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :l, 'LIDER'), "
                        + "(:e, :a, 'INTEGRANTE')")
                .param("e", equipo).param("l", idLider).param("a", idEstudiante).update();
        long he = jdbc.sql("INSERT INTO hito_equipo (id_hito, id_equipo) VALUES (:h, :e) RETURNING id_hito_equipo")
                .param("h", hito).param("e", equipo).query(Long.class).single();
        long tarea = jdbc.sql("INSERT INTO tarea (id_hito_equipo, id_encargado, titulo, horas_estimadas) "
                        + "VALUES (:he, :u, 'Informe', 4) RETURNING id_tarea")
                .param("he", he).param("u", idEstudiante).query(Long.class).single();
        return new Escenario(proyecto, equipo, he, idDocente, docente, idLider, lider, idEstudiante, estudiante,
                nombre, tarea);
    }

    private long proyecto(long idDocente, LocalDate hoy) {
        return jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Supresion', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(30)).param("f", hoy.plusDays(60))
                .query(Long.class).single();
    }

    private static TareaReasignada reasignada(Escenario e, Persona anterior, Persona nuevo) {
        return new TareaReasignada(new Referencia(e.idProyecto(), "Proyecto Supresion"),
                new Referencia(e.idEquipo(), "Equipo S"), new ReferenciaTarea(e.idTarea(), "Informe"), anterior,
                nuevo, new Referencia(e.idLider(), "Lider Supresion"), Instant.now());
    }

    private static HttpResponse<String> suprimir(Navegador n, String contrasena) {
        return n.post("/api/perfil/supresion", "{\"contrasena\":\"" + contrasena + "\"}");
    }

    private static HttpResponse<String> cambiar(Navegador n, String actual, String nueva) {
        return n.post("/api/perfil/contrasena",
                "{\"contrasenaActual\":\"" + actual + "\",\"contrasenaNueva\":\"" + nueva + "\"}");
    }

    private static String login(String email) {
        return "{\"email\":\"" + email + "\",\"contrasena\":\"" + CLAVE + "\"}";
    }

    private int contar(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(Integer.class).single();
    }

    private String texto(String sql, long id) {
        return jdbc.sql(sql).param("id", id).query(String.class).single();
    }

    private String carga(UUID evento) {
        return jdbc.sql("SELECT carga::text FROM evento_saliente WHERE id = :id").param("id", evento)
                .query(String.class).single();
    }

    @Test
    void suprimeLaCuentaYNoDejaDatosPersonales(CapturedOutput salida) {
        Escenario e = escenario();
        long id = e.idEstudiante();
        long chat = 700000000L + id;
        jdbc.sql("UPDATE usuario SET telegram_chat_id = :c WHERE id_usuario = :id").param("c", chat).param("id", id)
                .update();
        jdbc.sql("INSERT INTO codigo_vinculacion (codigo, usuario_id, vence_en) "
                        + "VALUES (:c, :id, now() + interval '10 minutes')")
                .param("c", String.format("%032d", id)).param("id", id).update();
        long otroProyecto = proyecto(e.idDocente(), LocalDate.now(FechasDominio.ZONA));
        jdbc.sql("INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por, "
                        + "id_usuario_registrado, fecha_uso) VALUES (:p, :email, 'ESTUDIANTE', :d, :id, now())")
                .param("p", e.idProyecto()).param("email", e.estudiante()).param("d", e.idDocente()).param("id", id)
                .update();
        jdbc.sql("INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por) "
                        + "VALUES (:p, :email, 'ESTUDIANTE', :d)")
                .param("p", otroProyecto).param("email", e.estudiante()).param("d", e.idDocente()).update();
        String url = "evidencias/" + UUID.randomUUID() + ".pdf";
        long evidencia = jdbc.sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url, nombre_archivo) "
                        + "VALUES (:t, :id, 'PDF', :url, :nombre) RETURNING id_evidencia")
                .param("t", e.idTarea()).param("id", id).param("url", url)
                .param("nombre", "Informe " + e.nombre() + ".pdf").query(Long.class).single();
        long comentario = jdbc.sql("INSERT INTO comentario_tarea (id_tarea, id_autor, contenido) "
                        + "VALUES (:t, :id, 'Avance listo para revisar') RETURNING id_comentario")
                .param("t", e.idTarea()).param("id", id).query(Long.class).single();
        long avance = jdbc.sql("INSERT INTO registro_avance (id_tarea, id_usuario, porcentaje_avance, observacion) "
                        + "VALUES (:t, :id, 40, 'Primer borrador') RETURNING id_registro")
                .param("t", e.idTarea()).param("id", id).query(Long.class).single();
        Persona persona = new Persona(id, e.nombre(), chat);
        Persona lider = new Persona(e.idLider(), "Lider Supresion", null);
        UUID pendiente = tx.execute(s -> eventos.registrar(reasignada(e, lider, persona)));
        UUID publicado = tx.execute(s -> eventos.registrar(new HitoEvaluado(
                new Referencia(e.idProyecto(), "Proyecto Supresion"), new Referencia(e.idEquipo(), "Equipo S"),
                new Referencia(e.idHitoEquipo(), "Hito 1"), CaracterNota.PROVISIONAL, List.of(lider, persona))));
        jdbc.sql("UPDATE evento_saliente SET publicado_en = now() WHERE id = :id").param("id", publicado).update();

        HttpResponse<String> r = suprimir(sesion(e.estudiante()), CLAVE);

        assertThat(r.statusCode()).as(r.body()).isEqualTo(204);
        assertThat(texto("SELECT nombre || '|' || email || '|' || password_hash || '|' "
                + "|| coalesce(CAST(telegram_chat_id AS text), 'null') || '|' || autoriza_tratamiento_datos || '|' "
                + "|| coalesce(CAST(fecha_autorizacion AS text), 'null') || '|' || (fecha_anonimizacion IS NOT NULL) "
                + "|| '|' || rol_plataforma FROM usuario WHERE id_usuario = :id", id))
                .isEqualTo(ANONIMO + "|anonimo-" + id + "@anonimo.invalid|!|null|false|null|true|ESTUDIANTE");
        assertThat(contar("SELECT count(*) FROM codigo_vinculacion WHERE usuario_id = :id", id)).isZero();
        assertThat(contar("SELECT count(*) FROM notificacion WHERE id_destinatario = :id", id)).isZero();
        assertThat(texto("SELECT mensaje FROM notificacion WHERE id_destinatario = :id AND tipo = 'tarea.reasignada'",
                e.idLider())).isEqualTo("La tarea \"Informe\" se reasignó a " + ANONIMO + ".");
        assertThat(contar("SELECT count(*) FROM correo_habilitado WHERE id_usuario_registrado = :id "
                + "AND email = 'anonimo-' || CAST(:id AS text) || '@anonimo.invalid'", id)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM correo_habilitado WHERE id_proyecto = :id", otroProyecto)).isZero();
        assertThat(texto("SELECT nombre_archivo || '|' || url FROM evidencia WHERE id_evidencia = :id", evidencia))
                .isEqualTo("evidencia-" + evidencia + ".pdf|" + url);
        assertThat(texto("SELECT id_autor || '|' || contenido FROM comentario_tarea WHERE id_comentario = :id",
                comentario)).isEqualTo(id + "|" + ServicioSupresion.COMENTARIO_ELIMINADO);
        assertThat(texto("SELECT id_usuario || '|' || observacion FROM registro_avance WHERE id_registro = :id",
                avance)).isEqualTo(id + "|Primer borrador");
        assertThat(contar("SELECT count(*) FROM miembro_equipo WHERE id_usuario = :id", id)).isEqualTo(1);
        TareaReasignada datos = (TareaReasignada) codec.leer(carga(pendiente).getBytes(StandardCharsets.UTF_8)).data();
        assertThat(datos.integranteNuevo()).isEqualTo(new Persona(id, ANONIMO, null));
        assertThat(datos.integranteAnterior()).isEqualTo(lider);
        HitoEvaluado evaluado = (HitoEvaluado) codec.leer(carga(publicado).getBytes(StandardCharsets.UTF_8)).data();
        assertThat(evaluado.integrantes()).containsExactly(lider, new Persona(id, ANONIMO, null));
        assertThat(jdbc.sql("SELECT count(*) FROM evento_saliente WHERE id = :id AND publicado_en IS NOT NULL")
                .param("id", publicado).query(Integer.class).single()).isEqualTo(1);
        Long restos = jdbc.sql("SELECT (SELECT count(*) FROM usuario WHERE nombre = :n OR email = :c "
                        + "OR telegram_chat_id = :chat) "
                        + "+ (SELECT count(*) FROM correo_habilitado WHERE email = :c) "
                        + "+ (SELECT count(*) FROM notificacion WHERE strpos(mensaje, :n) > 0) "
                        + "+ (SELECT count(*) FROM evento_saliente WHERE strpos(carga::text, :n) > 0 "
                        + "OR strpos(carga::text, :chatTexto) > 0) "
                        + "+ (SELECT count(*) FROM evidencia WHERE strpos(coalesce(nombre_archivo, ''), :n) > 0) "
                        + "+ (SELECT count(*) FROM log_auditoria WHERE strpos(coalesce(valor_anterior, '') "
                        + "|| coalesce(valor_nuevo, ''), :n) > 0 OR strpos(coalesce(valor_anterior, '') "
                        + "|| coalesce(valor_nuevo, ''), :c) > 0)")
                .param("n", e.nombre()).param("c", e.estudiante()).param("chat", chat)
                .param("chatTexto", String.valueOf(chat))
                .query(Long.class).single();
        assertThat(restos).as("filas con datos personales").isZero();
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE accion = 'SUPRIMIR_CUENTA' "
                + "AND entidad_afectada = 'usuario' AND id_usuario_accion = :id AND id_registro_afectado = :id "
                + "AND campo_modificado IS NULL AND valor_anterior IS NULL AND valor_nuevo IS NULL", id)).isEqualTo(1);
        assertThat(salida.getAll()).doesNotContain(e.nombre()).doesNotContain(e.estudiante());
    }

    @Test
    void cierraTodasLasSesionesYNoPermiteIngresar() {
        Escenario e = escenario();
        Navegador primera = sesion(e.estudiante());
        Navegador segunda = sesion(e.estudiante());
        assertThat(suprimir(primera, CLAVE).statusCode()).isEqualTo(204);
        assertThat(primera.get("/api/sesion/yo").statusCode()).isEqualTo(401);
        assertThat(segunda.get("/api/sesion/yo").statusCode()).isEqualTo(401);
        assertThat(segunda.get("/api/notificaciones").statusCode()).isEqualTo(401);
        Navegador nuevo = new Navegador(puerto);
        nuevo.pedirCsrf();
        assertThat(nuevo.post("/api/sesion/login", login(e.estudiante())).statusCode()).isEqualTo(401);
        assertThat(nuevo.post("/api/sesion/login", login(ServicioSupresion.correoAnonimo(e.idEstudiante())))
                .statusCode()).isEqualTo(401);
    }

    @Test
    void bloqueaAlLiderDeUnEquipoActivoHastaQueTermineElProyecto() {
        Escenario e = escenario();
        Navegador lider = sesion(e.lider());
        HttpResponse<String> r = suprimir(lider, CLAVE);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(409);
        assertThat(r.body()).contains("\"codigo\":\"LIDER_DE_EQUIPO_ACTIVO\"");
        assertThat(texto("SELECT nombre FROM usuario WHERE id_usuario = :id", e.idLider())).isEqualTo("Lider Supresion");
        assertThat(lider.get("/api/sesion/yo").statusCode()).isEqualTo(200);
        jdbc.sql("UPDATE proyecto SET fecha_fin = :f WHERE id_proyecto = :p")
                .param("f", LocalDate.now(FechasDominio.ZONA).minusDays(1)).param("p", e.idProyecto()).update();
        assertThat(suprimir(lider, CLAVE).statusCode()).isEqualTo(204);
        assertThat(contar("SELECT count(*) FROM miembro_equipo WHERE id_usuario = :id AND rol_equipo = 'LIDER'",
                e.idLider())).isEqualTo(1);
        assertThat(texto("SELECT nombre FROM usuario WHERE id_usuario = :id", e.idLider())).isEqualTo(ANONIMO);
    }

    @Test
    void bloqueaAlDocenteConProyectosActivosYAlAdministrador() {
        Escenario e = escenario();
        HttpResponse<String> docente = suprimir(sesion(e.docente()), CLAVE);
        assertThat(docente.statusCode()).as(docente.body()).isEqualTo(409);
        assertThat(docente.body()).contains("\"codigo\":\"DOCENTE_CON_PROYECTOS_ACTIVOS\"");
        String admin = correoUnico("adminsup");
        long idAdmin = crearUsuario("Admin Supresion", admin, "ADMINISTRADOR");
        HttpResponse<String> administrador = suprimir(sesion(admin), CLAVE);
        assertThat(administrador.statusCode()).as(administrador.body()).isEqualTo(409);
        assertThat(administrador.body()).contains("\"codigo\":\"CUENTA_ADMINISTRADOR\"");
        assertThat(contar("SELECT count(*) FROM usuario WHERE fecha_anonimizacion IS NULL AND id_usuario IN (:id, "
                + e.idDocente() + ")", idAdmin)).isEqualTo(2);
    }

    @Test
    void rechazaLaContrasenaIncorrectaSinCambiarNada() {
        Escenario e = escenario();
        Navegador n = sesion(e.estudiante());
        HttpResponse<String> r = suprimir(n, "OtraClave2026");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(400);
        assertThat(r.body()).contains("\"codigo\":\"CONTRASENA_INCORRECTA\"");
        HttpResponse<String> vacia = n.post("/api/perfil/supresion", "{}");
        assertThat(vacia.statusCode()).isEqualTo(400);
        assertThat(vacia.body()).contains("\"codigo\":\"VALIDACION\"");
        assertThat(texto("SELECT nombre FROM usuario WHERE id_usuario = :id", e.idEstudiante())).isEqualTo(e.nombre());
        assertThat(n.get("/api/sesion/yo").statusCode()).isEqualTo(200);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE accion = 'SUPRIMIR_CUENTA' "
                + "AND id_registro_afectado = :id", e.idEstudiante())).isZero();
    }

    @Test
    void esIdempotenteYUnaNuevaHabilitacionCreaOtraCuenta() {
        Escenario e = escenario();
        assertThat(suprimir(sesion(e.estudiante()), CLAVE).statusCode()).isEqualTo(204);
        supresion.suprimir(e.idEstudiante(), CLAVE);
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE accion = 'SUPRIMIR_CUENTA' "
                + "AND id_registro_afectado = :id", e.idEstudiante())).isEqualTo(1);
        jdbc.sql("INSERT INTO correo_habilitado (id_proyecto, email, rol_plataforma, id_habilitado_por) "
                        + "VALUES (:p, :email, 'ESTUDIANTE', :d)")
                .param("p", e.idProyecto()).param("email", e.estudiante()).param("d", e.idDocente()).update();
        Navegador nuevo = new Navegador(puerto);
        nuevo.pedirCsrf();
        HttpResponse<String> registro = nuevo.post("/api/cuentas/registro", "{\"nombre\":\"Paula Nueva\",\"email\":\""
                + e.estudiante() + "\",\"contrasena\":\"" + CLAVE + "\",\"autorizaTratamientoDatos\":true}");
        assertThat(registro.statusCode()).as(registro.body()).isEqualTo(201);
        long nuevoId = numero(registro.body(), "idUsuario");
        assertThat(nuevoId).isNotEqualTo(e.idEstudiante());
        assertThat(contar("SELECT count(*) FROM miembro_equipo WHERE id_usuario = :id", nuevoId)).isZero();
        assertThat(texto("SELECT email FROM usuario WHERE id_usuario = :id", e.idEstudiante()))
                .isEqualTo(ServicioSupresion.correoAnonimo(e.idEstudiante()));
    }

    @Test
    void muestraLosDatosPersonalesSinHashNiChat() {
        String correo = correoUnico("perfil");
        long id = crearUsuario("Marta Perfil", correo, "ESTUDIANTE");
        long chat = 710000000L + id;
        jdbc.sql("UPDATE usuario SET telegram_chat_id = :c WHERE id_usuario = :id").param("c", chat).param("id", id)
                .update();
        HttpResponse<String> r = sesion(correo).get("/api/perfil/datos");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat(tieneValor(r.body(), "idUsuario", id)).isTrue();
        assertThat(r.body()).contains("\"nombre\":\"Marta Perfil\"").contains("\"email\":\"" + correo + "\"")
                .contains("\"rol\":\"ESTUDIANTE\"").contains("\"telegramVinculado\":true")
                .contains("\"autorizaTratamientoDatos\":true").contains("\"fechaAutorizacion\":")
                .contains("\"fechaRegistro\":").doesNotContain("\"fechaAutorizacion\":null")
                .doesNotContain("passwordHash").doesNotContain("telegramChatId").doesNotContain(String.valueOf(chat))
                .doesNotContain("$2a$");
    }

    @Test
    void rectificaElNombreEnLaBandejaYLosEventosSinGuardarloEnLaAuditoria() {
        Escenario e = escenario();
        Persona persona = new Persona(e.idEstudiante(), e.nombre(), null);
        Persona lider = new Persona(e.idLider(), "Lider Supresion", null);
        UUID evento = tx.execute(s -> eventos.registrar(reasignada(e, lider, persona)));
        String parecido = e.nombre() + "x lleva 3 días sin registrar actividad en Equipo S.";
        long control = jdbc.sql("INSERT INTO notificacion (id_destinatario, tipo, mensaje, entidad_referencia, "
                        + "id_referencia, estado_envio) VALUES (:d, 'equipo.inactividad', :m, 'equipo', :e, "
                        + "'SIN_TELEGRAM') RETURNING id_notificacion")
                .param("d", e.idLider()).param("m", parecido).param("e", e.idEquipo()).query(Long.class).single();
        HttpResponse<String> r = sesion(e.estudiante()).put("/api/perfil/nombre", "{\"nombre\":\"  Paula Rectificada  \"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat(r.body()).contains("\"nombre\":\"Paula Rectificada\"");
        assertThat(texto("SELECT nombre FROM usuario WHERE id_usuario = :id", e.idEstudiante()))
                .isEqualTo("Paula Rectificada");
        assertThat(texto("SELECT mensaje FROM notificacion WHERE id_destinatario = :id AND tipo = 'tarea.reasignada'",
                e.idLider())).isEqualTo("La tarea \"Informe\" se reasignó a Paula Rectificada.");
        assertThat(texto("SELECT mensaje FROM notificacion WHERE id_notificacion = :id", control)).isEqualTo(parecido);
        TareaReasignada datos = (TareaReasignada) codec.leer(carga(evento).getBytes(StandardCharsets.UTF_8)).data();
        assertThat(datos.integranteNuevo()).isEqualTo(new Persona(e.idEstudiante(), "Paula Rectificada", null));
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE entidad_afectada = 'usuario' "
                + "AND id_registro_afectado = :id AND accion = 'ACTUALIZAR' AND campo_modificado = 'nombre' "
                + "AND valor_anterior IS NULL AND valor_nuevo IS NULL", e.idEstudiante())).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE strpos(coalesce(valor_anterior, '') "
                        + "|| coalesce(valor_nuevo, ''), :n) > 0")
                .param("n", e.nombre()).query(Integer.class).single()).isZero();
    }

    @Test
    void exigeSesionYNombreValido() {
        Navegador anonimo = new Navegador(puerto);
        anonimo.pedirCsrf();
        assertThat(anonimo.get("/api/perfil/datos").statusCode()).isEqualTo(401);
        assertThat(anonimo.put("/api/perfil/nombre", "{\"nombre\":\"Otro\"}").statusCode()).isEqualTo(401);
        assertThat(suprimir(anonimo, CLAVE).statusCode()).isEqualTo(401);
        String correo = correoUnico("perfilval");
        crearUsuario("Nombre Valido", correo, "ESTUDIANTE");
        Navegador n = sesion(correo);
        HttpResponse<String> blanco = n.put("/api/perfil/nombre", "{\"nombre\":\"   \"}");
        assertThat(blanco.statusCode()).isEqualTo(400);
        assertThat(blanco.body()).contains("\"codigo\":\"VALIDACION\"");
        HttpResponse<String> largo = n.put("/api/perfil/nombre", "{\"nombre\":\"" + "a".repeat(121) + "\"}");
        assertThat(largo.statusCode()).isEqualTo(400);
        assertThat(jdbc.sql("SELECT nombre FROM usuario WHERE email = :email").param("email", correo)
                .query(String.class).single()).isEqualTo("Nombre Valido");
    }

    @Test
    void noTocaLosAvisosDeUnHomonimo() {
        Escenario e = escenario();
        String homonimo = "Luis " + UUID.randomUUID().toString().substring(0, 8);
        long idHomonimo = crearUsuario(homonimo, correoUnico("homsup"), "ESTUDIANTE");
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :u)")
                .param("e", e.idEquipo()).param("u", idHomonimo).update();
        Persona lider = new Persona(e.idLider(), "Lider Supresion", null);
        tx.execute(s -> eventos.registrar(reasignada(e, lider, new Persona(idHomonimo, homonimo, null))));
        String aviso = "La tarea \"Informe\" se reasignó a " + homonimo + ".";
        String consulta = "SELECT mensaje FROM notificacion WHERE id_destinatario = :id AND tipo = 'tarea.reasignada'";
        Navegador n = sesion(e.estudiante());
        assertThat(n.put("/api/perfil/nombre", "{\"nombre\":\"" + homonimo + "\"}").statusCode()).isEqualTo(200);
        assertThat(n.put("/api/perfil/nombre", "{\"nombre\":\"Paula Otra\"}").statusCode()).isEqualTo(200);
        assertThat(texto(consulta, e.idLider())).isEqualTo(aviso);
        assertThat(suprimir(n, CLAVE).statusCode()).isEqualTo(204);
        assertThat(texto(consulta, e.idLider())).isEqualTo(aviso);
    }

    @Test
    void quitaElNombreDeLosAvisosAunqueYaNoSeaMiembro() {
        Escenario e = escenario();
        Persona lider = new Persona(e.idLider(), "Lider Supresion", null);
        Persona docente = new Persona(e.idDocente(), "Docente Supresion", null);
        tx.execute(s -> eventos.registrar(new EquipoInactividad(new Referencia(e.idProyecto(), "Proyecto Supresion"),
                new Referencia(e.idEquipo(), "Equipo S"), new Referencia(e.idEstudiante(), e.nombre()),
                Instant.now().minusSeconds(4 * 86400L), 3, lider, docente, false)));
        String consulta = "SELECT mensaje FROM notificacion WHERE id_destinatario = :id AND tipo = 'equipo.inactividad'";
        assertThat(texto(consulta, e.idLider())).isEqualTo(e.nombre() + " lleva 3 días sin registrar actividad en Equipo S.");
        jdbc.sql("DELETE FROM miembro_equipo WHERE id_equipo = :e AND id_usuario = :u")
                .param("e", e.idEquipo()).param("u", e.idEstudiante()).update();
        assertThat(suprimir(sesion(e.estudiante()), CLAVE).statusCode()).isEqualTo(204);
        assertThat(texto(consulta, e.idLider())).isEqualTo(ANONIMO + " lleva 3 días sin registrar actividad en Equipo S.");
    }

    @Test
    void noSeHabilitanCorreosDelDominioAnonimo() {
        Escenario e = escenario();
        HttpResponse<String> proyecto = sesion(e.docente()).post("/api/proyectos/" + e.idProyecto() + "/correos",
                "{\"emails\":[\"ANONIMO-" + e.idEstudiante() + "@anonimo.invalid\"],\"rol\":\"ESTUDIANTE\"}");
        assertThat(proyecto.statusCode()).as(proyecto.body()).isEqualTo(400);
        assertThat(proyecto.body()).contains("\"codigo\":\"CORREO_RESERVADO\"");
        String admin = correoUnico("adminres");
        crearUsuario("Admin Reservado", admin, "ADMINISTRADOR");
        HttpResponse<String> docente = sesion(admin).post("/api/admin/docentes",
                "{\"email\":\"anonimo-" + e.idDocente() + "@anonimo.invalid\"}");
        assertThat(docente.statusCode()).as(docente.body()).isEqualTo(400);
        assertThat(docente.body()).contains("\"codigo\":\"CORREO_RESERVADO\"");
        assertThat(jdbc.sql("SELECT count(*) FROM correo_habilitado WHERE email LIKE '%@anonimo.invalid' "
                + "AND fecha_uso IS NULL").query(Integer.class).single()).isZero();
    }

    @Test
    void noReescribeElAvisoDeOtroEstudianteDelMismoEquipoYSegundo() {
        Escenario e = escenario();
        String otro = "Bruno " + UUID.randomUUID().toString().substring(0, 8);
        long idOtro = crearUsuario(otro, correoUnico("otrosup"), "ESTUDIANTE");
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario) VALUES (:e, :u)")
                .param("e", e.idEquipo()).param("u", idOtro).update();
        Persona lider = new Persona(e.idLider(), "Lider Supresion", null);
        Persona docente = new Persona(e.idDocente(), "Docente Supresion", null);
        Instant ultima = Instant.now().minusSeconds(4 * 86400L);
        Referencia proyecto = new Referencia(e.idProyecto(), "Proyecto Supresion");
        Referencia equipo = new Referencia(e.idEquipo(), "Equipo S");
        tx.execute(s -> {
            eventos.registrar(new EquipoInactividad(proyecto, equipo, new Referencia(e.idEstudiante(), e.nombre()),
                    ultima, 3, lider, docente, false));
            eventos.registrar(new EquipoInactividad(proyecto, equipo, new Referencia(idOtro, otro),
                    ultima, 3, lider, docente, false));
            return null;
        });
        String avisoA = e.nombre() + " lleva 3 días sin registrar actividad en Equipo S.";
        String avisoB = otro + " lleva 3 días sin registrar actividad en Equipo S.";
        String porTexto = "SELECT id_notificacion FROM notificacion WHERE id_destinatario = :l AND mensaje = :m";
        long idAvisoA = jdbc.sql(porTexto).param("l", e.idLider()).param("m", avisoA).query(Long.class).single();
        long idAvisoB = jdbc.sql(porTexto).param("l", e.idLider()).param("m", avisoB).query(Long.class).single();
        jdbc.sql("UPDATE notificacion SET fecha_creacion = (SELECT fecha_creacion FROM notificacion "
                        + "WHERE id_notificacion = :a) WHERE id_notificacion = :b")
                .param("a", idAvisoA).param("b", idAvisoB).update();
        assertThat(suprimir(sesion(e.estudiante()), CLAVE).statusCode()).isEqualTo(204);
        assertThat(texto("SELECT mensaje FROM notificacion WHERE id_notificacion = :id", idAvisoA))
                .isEqualTo(ANONIMO + " lleva 3 días sin registrar actividad en Equipo S.");
        assertThat(texto("SELECT mensaje FROM notificacion WHERE id_notificacion = :id", idAvisoB)).isEqualTo(avisoB);
    }

    @Test
    void supresionVaciaComentariosDelTitularYConservaLosAjenosYCorrecciones() {
        Escenario e = escenario();
        long titular = e.idEstudiante();
        long otro = e.idLider();
        long tarea = e.idTarea();

        long cTitular = jdbc.sql("INSERT INTO comentario_tarea (id_tarea, id_autor, contenido) "
                        + "VALUES (:t, :u, 'Comentario del titular que debe borrarse') RETURNING id_comentario")
                .param("t", tarea).param("u", titular).query(Long.class).single();
        long cOtro = jdbc.sql("INSERT INTO comentario_tarea (id_tarea, id_autor, contenido) "
                        + "VALUES (:t, :u, 'Comentario de otro que debe quedar intacto') RETURNING id_comentario")
                .param("t", tarea).param("u", otro).query(Long.class).single();

        String url = "evidencias/" + UUID.randomUUID() + ".pdf";
        long evidencia = jdbc.sql("INSERT INTO evidencia (id_tarea, id_usuario, tipo_evidencia, url, nombre_archivo) "
                        + "VALUES (:t, :u, 'PDF', :url, 'informe.pdf') RETURNING id_evidencia")
                .param("t", tarea).param("u", titular).param("url", url).query(Long.class).single();
        long correccion = jdbc.sql("INSERT INTO solicitud_correccion (id_tarea, id_evidencia, id_lider, id_estudiante, observaciones) "
                        + "VALUES (:t, :ev, :l, :i, 'Observación de corrección del titular') RETURNING id_solicitud")
                .param("t", tarea).param("ev", evidencia).param("l", titular).param("i", otro).query(Long.class).single();

        HttpResponse<String> r = suprimir(sesion(e.estudiante()), CLAVE);
        assertThat(r.statusCode()).as(r.body()).isEqualTo(204);

        assertThat(texto("SELECT id_autor || '|' || contenido FROM comentario_tarea WHERE id_comentario = :id", cTitular))
                .isEqualTo(titular + "|" + ServicioSupresion.COMENTARIO_ELIMINADO);
        assertThat(texto("SELECT id_autor || '|' || contenido FROM comentario_tarea WHERE id_comentario = :id", cOtro))
                .isEqualTo(otro + "|Comentario de otro que debe quedar intacto");
        assertThat(texto("SELECT id_lider || '|' || observaciones FROM solicitud_correccion WHERE id_solicitud = :id", correccion))
                .isEqualTo(titular + "|Observación de corrección del titular");
    }

    @Test
    void limitaIntentosDeSupresionConContrasenaIncorrectaSinBorrarCuenta() {
        Escenario e = escenario();
        Navegador n = sesion(e.estudiante());
        List<Integer> intentos = IntStream.range(0, maximoCambios)
                .mapToObj(i -> suprimir(n, "ClaveIncorrecta").statusCode())
                .toList();
        assertThat(intentos).containsOnly(400);

        HttpResponse<String> limitado = suprimir(n, CLAVE);
        assertThat(limitado.statusCode()).isEqualTo(429);
        assertThat(limitado.body()).contains("\"codigo\":\"DEMASIADAS_SOLICITUDES\"");

        assertThat(texto("SELECT nombre FROM usuario WHERE id_usuario = :id", e.idEstudiante())).isEqualTo(e.nombre());
        assertThat(texto("SELECT email FROM usuario WHERE id_usuario = :id", e.idEstudiante())).isEqualTo(e.estudiante());
        assertThat(contar("SELECT count(*) FROM usuario WHERE fecha_anonimizacion IS NOT NULL AND id_usuario = :id",
                e.idEstudiante())).isZero();
        assertThat(contar("SELECT count(*) FROM log_auditoria WHERE accion = 'SUPRIMIR_CUENTA' "
                + "AND id_registro_afectado = :id", e.idEstudiante())).isZero();
    }

    @Test
    void intentosDeSupresionYCambioContrasenaCompartenCupo() {
        Escenario e = escenario();
        Navegador n = sesion(e.estudiante());
        int mitad = maximoCambios / 2;
        int resto = maximoCambios - mitad;

        for (int i = 0; i < mitad; i++) {
            assertThat(cambiar(n, "ClaveIncorrecta", "NuevaClave2026!").statusCode()).isEqualTo(400);
        }
        for (int i = 0; i < resto; i++) {
            assertThat(suprimir(n, "ClaveIncorrecta").statusCode()).isEqualTo(400);
        }

        HttpResponse<String> supLimitado = suprimir(n, CLAVE);
        assertThat(supLimitado.statusCode()).isEqualTo(429);
        assertThat(supLimitado.body()).contains("\"codigo\":\"DEMASIADAS_SOLICITUDES\"");

        HttpResponse<String> cambioLimitado = cambiar(n, CLAVE, "OtraClave2026!");
        assertThat(cambioLimitado.statusCode()).isEqualTo(429);
        assertThat(cambioLimitado.body()).contains("\"codigo\":\"DEMASIADAS_SOLICITUDES\"");
    }
}
