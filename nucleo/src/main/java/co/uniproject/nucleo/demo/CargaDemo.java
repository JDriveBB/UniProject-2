package co.uniproject.nucleo.demo;

import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.configuracionacademica.cursos.ServicioCursos;
import co.uniproject.nucleo.configuracionacademica.cursos.SolicitudCorreosCurso;
import co.uniproject.nucleo.configuracionacademica.cursos.SolicitudCurso;
import co.uniproject.nucleo.configuracionacademica.servicio.EquipoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.HitoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioCorreosProyecto;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioEquipos;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioHitos;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioProyectos;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudCorreos;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudCriterio;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudCursosProyecto;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudEquipo;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudHito;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudLider;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudMiembro;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudProyecto;
import co.uniproject.nucleo.ejecucion.servicio.EvidenciaVista;
import co.uniproject.nucleo.ejecucion.servicio.PeticionCorreccion;
import co.uniproject.nucleo.ejecucion.servicio.ServicioApoyo;
import co.uniproject.nucleo.ejecucion.servicio.ServicioAvance;
import co.uniproject.nucleo.ejecucion.servicio.ServicioComentarios;
import co.uniproject.nucleo.ejecucion.servicio.ServicioCorrecciones;
import co.uniproject.nucleo.ejecucion.servicio.ServicioDependencias;
import co.uniproject.nucleo.ejecucion.servicio.ServicioEvidencias;
import co.uniproject.nucleo.ejecucion.servicio.ServicioReasignacion;
import co.uniproject.nucleo.ejecucion.servicio.ServicioTareas;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudApoyo;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudAvance;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudComentario;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudDependencias;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudEnlace;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudReasignacion;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudTarea;
import co.uniproject.nucleo.ejecucion.servicio.TipoReasignacion;
import co.uniproject.nucleo.evaluacion.servicio.CalificacionSolicitada;
import co.uniproject.nucleo.evaluacion.servicio.ServicioEnvio;
import co.uniproject.nucleo.evaluacion.servicio.ServicioEvaluacion;
import co.uniproject.nucleo.evaluacion.servicio.SolicitudEvaluacion;
import co.uniproject.nucleo.seguridad.UsuarioAutenticado;
import co.uniproject.nucleo.seguridad.cuentas.ServicioCuentas;
import co.uniproject.nucleo.seguridad.cuentas.SolicitudRegistro;
import co.uniproject.nucleo.seguridad.cuentas.UsuarioSesion;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escenario demo (decisión 22-7) cargado con los servicios reales, actuando como cada usuario. Todo en una transacción:
 * si algo falla no queda nada a medias (22-4). Correos ficticios del dominio reservado .test (Ley 1581).
 */
@Component
@Profile("demo")
public class CargaDemo {

    static final String DOMINIO = "demo.uniproject.test";
    static final String DOCENTE = "docente@" + DOMINIO;
    static final String PROYECTO = "Biblioteca digital (demo)";
    static final String PROYECTO_2 = "App de turnos (demo)";
    static final String CURSO = "Diseño de Software I (demo)";

    private final JdbcClient jdbc;
    private final PasswordEncoder codificador;
    private final ServicioCuentas cuentas;
    private final ServicioCursos cursos;
    private final ServicioProyectos proyectos;
    private final ServicioHitos hitos;
    private final ServicioCorreosProyecto correos;
    private final ServicioEquipos equipos;
    private final ServicioTareas tareas;
    private final ServicioDependencias dependencias;
    private final ServicioAvance avance;
    private final ServicioEvidencias evidencias;
    private final ServicioComentarios comentarios;
    private final ServicioApoyo apoyo;
    private final ServicioReasignacion reasignacion;
    private final ServicioCorrecciones correcciones;
    private final ServicioEnvio envio;
    private final ServicioEvaluacion evaluacion;

    public CargaDemo(
            JdbcClient jdbc,
            PasswordEncoder codificador,
            ServicioCuentas cuentas,
            ServicioCursos cursos,
            ServicioProyectos proyectos,
            ServicioHitos hitos,
            ServicioCorreosProyecto correos,
            ServicioEquipos equipos,
            ServicioTareas tareas,
            ServicioDependencias dependencias,
            ServicioAvance avance,
            ServicioEvidencias evidencias,
            ServicioComentarios comentarios,
            ServicioApoyo apoyo,
            ServicioReasignacion reasignacion,
            ServicioCorrecciones correcciones,
            ServicioEnvio envio,
            ServicioEvaluacion evaluacion) {
        this.jdbc = jdbc;
        this.codificador = codificador;
        this.cuentas = cuentas;
        this.cursos = cursos;
        this.proyectos = proyectos;
        this.hitos = hitos;
        this.correos = correos;
        this.equipos = equipos;
        this.tareas = tareas;
        this.dependencias = dependencias;
        this.avance = avance;
        this.evidencias = evidencias;
        this.comentarios = comentarios;
        this.apoyo = apoyo;
        this.reasignacion = reasignacion;
        this.correcciones = correcciones;
        this.envio = envio;
        this.evaluacion = evaluacion;
    }

    /** true si cargó; false si ya existían (22-5). */
    @Transactional
    public boolean cargar(String contrasena) {
        boolean existe = jdbc.sql("SELECT EXISTS (SELECT 1 FROM usuario WHERE email = :email)")
                .param("email", DOCENTE)
                .query(Boolean.class)
                .single();
        if (existe) {
            return false;
        }
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        UsuarioAutenticado docente = docente(contrasena);

        long idProyecto = proyectos
                .crear(
                        docente,
                        new SolicitudProyecto(
                                PROYECTO,
                                "Proyecto de demostración: préstamos y devoluciones de una biblioteca universitaria.",
                                hoy.minusDays(21),
                                hoy.plusDays(60),
                                true,
                                false))
                .idProyecto();
        HitoVista hito1 = hitos.crear(
                docente,
                idProyecto,
                new SolicitudHito(
                        "Requisitos y diagramas",
                        "Casos de uso, diagrama de clases y documento de requisitos.",
                        1,
                        hoy.minusDays(21),
                        hoy.plusDays(3),
                        3,
                        true));
        HitoVista hito2 = hitos.crear(
                docente,
                idProyecto,
                new SolicitudHito(
                        "Prototipo funcional",
                        "Primera versión navegable con préstamos y devoluciones.",
                        2,
                        hoy.plusDays(4),
                        hoy.plusDays(30),
                        5,
                        true));
        long clases = criterio(docente, hito1, "Diagrama de clases", "40");
        long casos = criterio(docente, hito1, "Casos de uso", "30");
        long requisitos = criterio(docente, hito1, "Documento de requisitos", "30");
        criterio(docente, hito2, "Funcionalidad", "60");
        criterio(docente, hito2, "Calidad del código", "40");

        long idCurso =
                cursos.crear(docente, new SolicitudCurso(CURSO, "2026-2", null)).idCurso();
        cursos.agregarCorreos(
                docente,
                idCurso,
                new SolicitudCorreosCurso(List.of(
                        correo("ana"),
                        correo("bruno"),
                        correo("camila"),
                        correo("diego"),
                        correo("elena"),
                        correo("felipe"),
                        correo("hugo"),
                        correo("ines"),
                        correo("jorge"))));
        proyectos.asignarCursos(docente, idProyecto, new SolicitudCursosProyecto(List.of(idCurso), false));
        correos.habilitar(docente, idProyecto, new SolicitudCorreos(List.of(correo("monitor")), RolPlataforma.MONITOR));
        UsuarioAutenticado ana = registrarEstudiante(docente, idCurso, "Ana Rojas", "ana", contrasena);
        UsuarioAutenticado bruno = registrarEstudiante(docente, idCurso, "Bruno Díaz", "bruno", contrasena);
        UsuarioAutenticado camila = registrarEstudiante(docente, idCurso, "Camila Torres", "camila", contrasena);
        UsuarioAutenticado diego = registrarEstudiante(docente, idCurso, "Diego Pérez", "diego", contrasena);
        UsuarioAutenticado elena = registrarEstudiante(docente, idCurso, "Elena Gómez", "elena", contrasena);
        UsuarioAutenticado felipe = registrarEstudiante(docente, idCurso, "Felipe Ruiz", "felipe", contrasena);
        UsuarioAutenticado monitor = registrarMonitor(docente, idProyecto, "Gabriela Soto", "monitor", contrasena);
        // hugo = cuenta de sacrificio de la sesión de pruebas (límites, recuperación, supresión);
        // ines/jorge = correos pendientes del curso para probar registro y «Retirar».
        registrarEstudiante(docente, idCurso, "Hugo Lara", "hugo", contrasena);

        EquipoVista alfa = equipo(docente, idProyecto, "Equipo Alfa", ana, List.of(ana, bruno, camila));
        EquipoVista beta = equipo(docente, idProyecto, "Equipo Beta", diego, List.of(diego, elena, felipe));
        long alfa1 = hitoEquipo(hito1.idHito(), alfa.idEquipo());
        long beta1 = hitoEquipo(hito1.idHito(), beta.idEquipo());

        // Equipo Alfa: hito 1 completo, enviado y evaluado (nota provisional, sin confirmar).
        long a1 = tarea(ana, alfa1, "Levantar requisitos", ana, "6", true);
        long a2 = tarea(ana, alfa1, "Diagrama de casos de uso", bruno, "5", false);
        long a3 = tarea(ana, alfa1, "Diagrama de clases", camila, "8", true);
        dependencias.agregar(ana, a2, new SolicitudDependencias(List.of(a1)));
        dependencias.agregar(ana, a3, new SolicitudDependencias(List.of(a1)));
        comentarios.comentar(
                docente, a3, new SolicitudComentario("Revisen la multiplicidad entre Préstamo y Ejemplar."));
        avance.actualizar(ana, a1, new SolicitudAvance(100, "Requisitos validados con la docente"));
        avance.actualizar(bruno, a2, new SolicitudAvance(100, "Casos de uso completos"));
        evidencias.adjuntarEnlace(camila, a3, new SolicitudEnlace("https://example.org/demo/diagrama-de-clases"));
        avance.actualizar(camila, a3, new SolicitudAvance(100, "Diagrama corregido"));
        envio.enviar(bruno, alfa1);
        evaluacion.evaluar(
                docente,
                alfa1,
                new SolicitudEvaluacion(
                        List.of(
                                new CalificacionSolicitada(clases, new BigDecimal("4.50"), "Clases bien identificadas"),
                                new CalificacionSolicitada(casos, new BigDecimal("4.00"), null),
                                new CalificacionSolicitada(
                                        requisitos, new BigDecimal("3.50"), "Faltan requisitos no funcionales")),
                        null,
                        "Buen avance; completen los requisitos no funcionales."));

        // Equipo Beta: hito 1 en curso con bloqueo, apoyo y una integrante inactiva.
        long b1 = tarea(diego, beta1, "Levantar requisitos", diego, "6", true);
        long b2 = tarea(diego, beta1, "Diagrama de casos de uso", elena, "5", false);
        long b3 = tarea(diego, beta1, "Diagrama de clases", felipe, "10", true);
        long b4 = tarea(diego, beta1, "Documento de requisitos", elena, "4", false);
        dependencias.agregar(diego, b2, new SolicitudDependencias(List.of(b1)));
        dependencias.agregar(diego, b3, new SolicitudDependencias(List.of(b1)));
        dependencias.agregar(diego, b4, new SolicitudDependencias(List.of(b2)));
        avance.actualizar(diego, b1, new SolicitudAvance(40, "Faltan dos entrevistas"));
        apoyo.solicitar(felipe, b3, new SolicitudApoyo("¿Cómo modelamos la herencia entre Libro y Revista?"));
        comentarios.comentar(
                monitor, b2, new SolicitudComentario("Pueden empezar el borrador aunque falten entrevistas."));

        // Equipo Alfa: hito 2 en curso con ruta crítica de 5 tareas, 2 ramas cortas, reasignación y comentario.
        long alfa2 = hitoEquipo(hito2.idHito(), alfa.idEquipo());
        long repo = tarea(ana, alfa2, "Configurar repositorio", ana, "4", hoy.plusDays(6), true);
        long modelo = tarea(ana, alfa2, "Modelo de datos", bruno, "8", hoy.plusDays(12), true);
        long diseno = tarea(ana, alfa2, "Diseño de pantallas", camila, "6", hoy.plusDays(14), false);
        long api = tarea(ana, alfa2, "API de préstamos", ana, "10", hoy.plusDays(18), true);
        long datosPrueba = tarea(ana, alfa2, "Datos de prueba", bruno, "4", hoy.plusDays(20), false);
        long pantalla = tarea(ana, alfa2, "Pantalla de préstamos", bruno, "8", hoy.plusDays(24), true);
        long pruebas = tarea(ana, alfa2, "Pruebas de integración", ana, "6", hoy.plusDays(28), true);
        dependencias.agregar(ana, modelo, new SolicitudDependencias(List.of(repo)));
        dependencias.agregar(ana, diseno, new SolicitudDependencias(List.of(repo)));
        dependencias.agregar(ana, api, new SolicitudDependencias(List.of(modelo)));
        dependencias.agregar(ana, datosPrueba, new SolicitudDependencias(List.of(modelo)));
        dependencias.agregar(ana, pantalla, new SolicitudDependencias(List.of(api, diseno)));
        dependencias.agregar(ana, pruebas, new SolicitudDependencias(List.of(pantalla, datosPrueba)));
        avance.actualizar(ana, repo, new SolicitudAvance(100, "Repositorio y estructura base creados"));
        avance.actualizar(bruno, modelo, new SolicitudAvance(50, "Entidades de catálogo y préstamo definidas"));
        avance.actualizar(camila, diseno, new SolicitudAvance(40, "Bocetos iniciales de préstamo y devolución"));

        // Reasignación (RF11) en Alfa hito 2: pasa de Bruno a Camila con motivo.
        reasignacion.reasignar(
                ana,
                pantalla,
                new SolicitudReasignacion(
                        camila.idUsuario(),
                        TipoReasignacion.TOTAL,
                        null,
                        null,
                        "Equilibrar la carga entre API y pantallas del prototipo"));

        // Equipo Beta: evidencia adjunta de Elena y corrección pendiente pedida por Diego (RF5).
        EvidenciaVista evidenciaElena = evidencias.adjuntarEnlace(
                elena, b2, new SolicitudEnlace("https://example.org/demo/diagrama-casos-uso"));
        correcciones.solicitar(
                diego,
                b2,
                new PeticionCorreccion(
                        evidenciaElena.idEvidencia(),
                        "Falta el actor Administrador y el flujo alterno de préstamo vencido."));

        // Comentario adicional del docente en Alfa hito 2.
        comentarios.comentar(
                docente,
                modelo,
                new SolicitudComentario("Verifiquen las restricciones de préstamo activo antes de cerrar el modelo."));

        // Decisión 22-8: solo datos demo, para que el panel muestre el rojo recién cargado.
        jdbc.sql("UPDATE tarea SET fecha_bloqueo = now() - interval '3 days' "
                        + "WHERE id_tarea = :tarea AND bloqueada_por_dependencia")
                .param("tarea", b3)
                .update();
        jdbc.sql("UPDATE miembro_equipo SET fecha_ingreso = now() - interval '6 days' "
                        + "WHERE id_equipo = :equipo AND id_usuario = :usuario")
                .param("equipo", beta.idEquipo())
                .param("usuario", elena.idUsuario())
                .update();
        // Solo datos demo: conserva la inactividad de 6 días de Elena pese al enlace de muestra.
        jdbc.sql("UPDATE evidencia SET fecha_subida = now() - interval '6 days' WHERE id_evidencia = :evidencia")
                .param("evidencia", evidenciaElena.idEvidencia())
                .update();

        // Segundo proyecto: muestra la lista de proyectos, la elección de panel y un hito por calificar.
        long idProyecto2 = proyectos
                .crear(
                        docente,
                        new SolicitudProyecto(
                                PROYECTO_2,
                                "Proyecto de demostración: turnos de atención de un consultorio universitario.",
                                hoy.minusDays(14),
                                hoy.plusDays(45),
                                true,
                                false))
                .idProyecto();
        proyectos.asignarCursos(docente, idProyecto2, new SolicitudCursosProyecto(List.of(idCurso), false));
        HitoVista p2Hito1 = hitos.crear(
                docente,
                idProyecto2,
                new SolicitudHito("Análisis y diseño", null, 1, hoy.minusDays(14), hoy.plusDays(1), 3, true));
        HitoVista p2Hito2 = hitos.crear(
                docente,
                idProyecto2,
                new SolicitudHito("Versión entregable", null, 2, hoy.plusDays(2), hoy.plusDays(40), 5, true));
        criterio(docente, p2Hito1, "Modelo de datos", "50");
        criterio(docente, p2Hito1, "Prototipo de pantallas", "50");
        criterio(docente, p2Hito2, "Funcionalidad", "100");

        EquipoVista gamma = equipo(docente, idProyecto2, "Equipo Gamma", ana, List.of(ana, diego));
        EquipoVista delta = equipo(docente, idProyecto2, "Equipo Delta", bruno, List.of(bruno, elena));
        long gamma1 = hitoEquipo(p2Hito1.idHito(), gamma.idEquipo());
        long delta1 = hitoEquipo(p2Hito1.idHito(), delta.idEquipo());

        // Equipo Gamma: hito 1 completo y enviado, sin evaluar.
        long g1 = tarea(ana, gamma1, "Modelo de datos", ana, "6", true);
        long g2 = tarea(ana, gamma1, "Prototipo de pantallas", diego, "4", false);
        avance.actualizar(ana, g1, new SolicitudAvance(100, "Modelo entidad-relación completo"));
        avance.actualizar(diego, g2, new SolicitudAvance(100, "Flujo de reserva y cancelación diseñado"));
        envio.enviar(ana, gamma1);

        // Equipo Delta: hito 1 en curso con dependencia.
        long d1 = tarea(bruno, delta1, "Modelo de datos", bruno, "6", true);
        long d2 = tarea(bruno, delta1, "Prototipo de pantallas", elena, "4", false);
        dependencias.agregar(bruno, d2, new SolicitudDependencias(List.of(d1)));
        avance.actualizar(bruno, d1, new SolicitudAvance(50, "Tablas de citas y horarios definidas"));
        return true;
    }

    static String correo(String usuario) {
        return usuario + "@" + DOMINIO;
    }

    private UsuarioAutenticado docente(String contrasena) {
        String nombre = "Laura Méndez";
        long id = jdbc.sql("INSERT INTO usuario (nombre, email, password_hash, rol_plataforma, "
                        + "autoriza_tratamiento_datos, fecha_autorizacion) "
                        + "VALUES (:nombre, :email, :hash, 'DOCENTE', true, now()) RETURNING id_usuario")
                .param("nombre", nombre)
                .param("email", DOCENTE)
                .param("hash", codificador.encode(contrasena))
                .query(Long.class)
                .single();
        return new UsuarioAutenticado(id, nombre, DOCENTE, RolPlataforma.DOCENTE);
    }

    private UsuarioAutenticado registrarEstudiante(
            UsuarioAutenticado docente, long idCurso, String nombre, String usuario, String contrasena) {
        String email = correo(usuario);
        long idCorreo = jdbc.sql(
                        "SELECT id_correo_habilitado FROM correo_habilitado WHERE id_curso = :c AND email = :e")
                .param("c", idCurso)
                .param("e", email)
                .query(Long.class)
                .single();
        String codigo = cursos.generarEnlace(docente, idCurso, idCorreo).codigo();
        UsuarioSesion s = cuentas.registrar(new SolicitudRegistro(nombre, email, contrasena, true, codigo));
        return new UsuarioAutenticado(s.idUsuario(), s.nombre(), s.email(), s.rol());
    }

    private UsuarioAutenticado registrarMonitor(
            UsuarioAutenticado docente, long idProyecto, String nombre, String usuario, String contrasena) {
        String email = correo(usuario);
        long idCorreo = jdbc.sql(
                        "SELECT id_correo_habilitado FROM correo_habilitado WHERE id_proyecto = :p AND email = :e")
                .param("p", idProyecto)
                .param("e", email)
                .query(Long.class)
                .single();
        String codigo = correos.generarEnlace(docente, idProyecto, idCorreo).codigo();
        UsuarioSesion s = cuentas.registrar(new SolicitudRegistro(nombre, email, contrasena, true, codigo));
        return new UsuarioAutenticado(s.idUsuario(), s.nombre(), s.email(), s.rol());
    }

    private long criterio(UsuarioAutenticado docente, HitoVista hito, String descripcion, String peso) {
        return hitos.crearCriterio(
                        docente,
                        hito.idHito(),
                        new SolicitudCriterio(descripcion, new BigDecimal(peso), new BigDecimal("5")))
                .idCriterio();
    }

    private EquipoVista equipo(
            UsuarioAutenticado docente,
            long idProyecto,
            String nombre,
            UsuarioAutenticado lider,
            List<UsuarioAutenticado> miembros) {
        EquipoVista equipo = equipos.crear(docente, idProyecto, new SolicitudEquipo(nombre, null, null));
        for (UsuarioAutenticado m : miembros) {
            equipos.agregarMiembro(docente, equipo.idEquipo(), new SolicitudMiembro(m.email(), false));
        }
        return equipos.cambiarLider(docente, equipo.idEquipo(), new SolicitudLider(lider.idUsuario()));
    }

    private long hitoEquipo(long idHito, long idEquipo) {
        return jdbc.sql("SELECT id_hito_equipo FROM hito_equipo WHERE id_hito = :hito AND id_equipo = :equipo")
                .param("hito", idHito)
                .param("equipo", idEquipo)
                .query(Long.class)
                .single();
    }

    private long tarea(
            UsuarioAutenticado lider,
            long idHitoEquipo,
            String titulo,
            UsuarioAutenticado encargado,
            String horas,
            boolean critica) {
        return tarea(lider, idHitoEquipo, titulo, encargado, horas, null, critica);
    }

    private long tarea(
            UsuarioAutenticado lider,
            long idHitoEquipo,
            String titulo,
            UsuarioAutenticado encargado,
            String horas,
            LocalDate fechaLimite,
            boolean critica) {
        return tareas.crear(
                        lider,
                        idHitoEquipo,
                        new SolicitudTarea(
                                titulo, null, encargado.idUsuario(), new BigDecimal(horas), fechaLimite, critica))
                .idTarea();
    }
}
