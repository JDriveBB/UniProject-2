package co.uniproject.nucleo.demo;

import co.uniproject.nucleo.comun.FechasDominio;
import co.uniproject.nucleo.configuracionacademica.servicio.EquipoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.HitoVista;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioCorreosProyecto;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioEquipos;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioHitos;
import co.uniproject.nucleo.configuracionacademica.servicio.ServicioProyectos;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudCorreos;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudCriterio;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudEquipo;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudHito;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudLider;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudMiembro;
import co.uniproject.nucleo.configuracionacademica.servicio.SolicitudProyecto;
import co.uniproject.nucleo.ejecucion.servicio.ServicioApoyo;
import co.uniproject.nucleo.ejecucion.servicio.ServicioAvance;
import co.uniproject.nucleo.ejecucion.servicio.ServicioComentarios;
import co.uniproject.nucleo.ejecucion.servicio.ServicioDependencias;
import co.uniproject.nucleo.ejecucion.servicio.ServicioEvidencias;
import co.uniproject.nucleo.ejecucion.servicio.ServicioTareas;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudApoyo;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudAvance;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudComentario;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudDependencias;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudEnlace;
import co.uniproject.nucleo.ejecucion.servicio.SolicitudTarea;
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

    private final JdbcClient jdbc;
    private final PasswordEncoder codificador;
    private final ServicioCuentas cuentas;
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
    private final ServicioEnvio envio;
    private final ServicioEvaluacion evaluacion;

    public CargaDemo(
            JdbcClient jdbc,
            PasswordEncoder codificador,
            ServicioCuentas cuentas,
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
            ServicioEnvio envio,
            ServicioEvaluacion evaluacion) {
        this.jdbc = jdbc;
        this.codificador = codificador;
        this.cuentas = cuentas;
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
                                true))
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

        correos.habilitar(
                docente,
                idProyecto,
                new SolicitudCorreos(
                        List.of(
                                correo("ana"),
                                correo("bruno"),
                                correo("camila"),
                                correo("diego"),
                                correo("elena"),
                                correo("felipe"),
                                correo("hugo"),
                                correo("ines"),
                                correo("jorge")),
                        RolPlataforma.ESTUDIANTE));
        correos.habilitar(docente, idProyecto, new SolicitudCorreos(List.of(correo("monitor")), RolPlataforma.MONITOR));
        UsuarioAutenticado ana = registrar(docente, idProyecto, "Ana Rojas", "ana", contrasena);
        UsuarioAutenticado bruno = registrar(docente, idProyecto, "Bruno Díaz", "bruno", contrasena);
        UsuarioAutenticado camila = registrar(docente, idProyecto, "Camila Torres", "camila", contrasena);
        UsuarioAutenticado diego = registrar(docente, idProyecto, "Diego Pérez", "diego", contrasena);
        UsuarioAutenticado elena = registrar(docente, idProyecto, "Elena Gómez", "elena", contrasena);
        UsuarioAutenticado felipe = registrar(docente, idProyecto, "Felipe Ruiz", "felipe", contrasena);
        UsuarioAutenticado monitor = registrar(docente, idProyecto, "Gabriela Soto", "monitor", contrasena);
        // hugo = cuenta de sacrificio de la sesión de pruebas (límites, recuperación, supresión);
        // ines/jorge = correos pendientes para probar registro y «Retirar».
        registrar(docente, idProyecto, "Hugo Lara", "hugo", contrasena);

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

        // Segundo proyecto: muestra la lista de proyectos, la elección de panel y un hito por calificar.
        long idProyecto2 = proyectos
                .crear(
                        docente,
                        new SolicitudProyecto(
                                PROYECTO_2,
                                "Proyecto de demostración: turnos de atención de un consultorio universitario.",
                                hoy.minusDays(14),
                                hoy.plusDays(45),
                                true))
                .idProyecto();
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

        correos.habilitar(
                docente,
                idProyecto2,
                new SolicitudCorreos(
                        List.of(correo("ana"), correo("diego"), correo("bruno"), correo("elena")),
                        RolPlataforma.ESTUDIANTE));

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

    private UsuarioAutenticado registrar(
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
        EquipoVista equipo = equipos.crear(docente, idProyecto, new SolicitudEquipo(nombre));
        for (UsuarioAutenticado m : miembros) {
            equipos.agregarMiembro(docente, equipo.idEquipo(), new SolicitudMiembro(m.email()));
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
        return tareas.crear(
                        lider,
                        idHitoEquipo,
                        new SolicitudTarea(titulo, null, encargado.idUsuario(), new BigDecimal(horas), null, critica))
                .idTarea();
    }
}
