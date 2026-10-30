package co.uniproject.nucleo;

import co.uniproject.nucleo.configuracionacademica.dominio.Equipo;
import co.uniproject.nucleo.configuracionacademica.dominio.Hito;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipo;
import co.uniproject.nucleo.configuracionacademica.dominio.MiembroEquipoId;
import co.uniproject.nucleo.configuracionacademica.dominio.Proyecto;
import co.uniproject.nucleo.configuracionacademica.dominio.RolEquipo;
import co.uniproject.nucleo.configuracionacademica.repositorio.EquipoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.HitoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.MiembroEquipoRepositorio;
import co.uniproject.nucleo.configuracionacademica.repositorio.ProyectoRepositorio;
import co.uniproject.nucleo.ejecucion.dominio.DependenciaTarea;
import co.uniproject.nucleo.ejecucion.dominio.EstadoHitoEquipo;
import co.uniproject.nucleo.ejecucion.dominio.EstadoTarea;
import co.uniproject.nucleo.ejecucion.dominio.HitoEquipo;
import co.uniproject.nucleo.ejecucion.dominio.Tarea;
import co.uniproject.nucleo.ejecucion.repositorio.DependenciaTareaRepositorio;
import co.uniproject.nucleo.ejecucion.repositorio.HitoEquipoRepositorio;
import co.uniproject.nucleo.ejecucion.repositorio.TareaRepositorio;
import co.uniproject.nucleo.notificaciones.dominio.EstadoEnvioNotificacion;
import co.uniproject.nucleo.notificaciones.dominio.Notificacion;
import co.uniproject.nucleo.notificaciones.repositorio.NotificacionRepositorio;
import co.uniproject.nucleo.seguridad.dominio.CorreoHabilitado;
import co.uniproject.nucleo.seguridad.dominio.RolPlataforma;
import co.uniproject.nucleo.seguridad.dominio.Usuario;
import co.uniproject.nucleo.seguridad.repositorio.CorreoHabilitadoRepositorio;
import co.uniproject.nucleo.seguridad.repositorio.UsuarioRepositorio;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import co.uniproject.nucleo.trazabilidad.dominio.LogAuditoria;
import co.uniproject.nucleo.trazabilidad.repositorio.LogAuditoriaRepositorio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class MapeoDominioTest {

    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6");
    private static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.3.7-management");
    private static final Instant AHORA = Instant.parse("2026-11-02T13:00:00Z");

    static {
        postgres.start();
        rabbit.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("uniproject.token-interno", () -> "token-de-prueba");
        registry.add("uniproject.outbox.intervalo-ms", () -> 600000L);
    }

    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private CorreoHabilitadoRepositorio correos;
    @Autowired private ProyectoRepositorio proyectos;
    @Autowired private EquipoRepositorio equipos;
    @Autowired private MiembroEquipoRepositorio miembros;
    @Autowired private HitoRepositorio hitos;
    @Autowired private HitoEquipoRepositorio hitosEquipo;
    @Autowired private TareaRepositorio tareas;
    @Autowired private DependenciaTareaRepositorio dependencias;
    @Autowired private NotificacionRepositorio notificaciones;
    @Autowired private LogAuditoriaRepositorio logs;
    @Autowired private RegistroAuditoria auditoria;
    @Autowired private JdbcClient jdbc;

    private Usuario usuario(String email, RolPlataforma rol) {
        Usuario u = new Usuario();
        u.setNombre("Nombre " + email);
        u.setEmail(email);
        u.setPasswordHash("!");
        u.setRolPlataforma(rol);
        u.setFechaRegistro(AHORA);
        return usuarios.save(u);
    }

    private Tarea tarea(HitoEquipo he, String titulo, String horas, Usuario encargado) {
        Tarea t = new Tarea();
        t.setIdHitoEquipo(he.getIdHitoEquipo());
        t.setIdEncargado(encargado.getIdUsuario());
        t.setTitulo(titulo);
        t.setHorasEstimadas(new BigDecimal(horas));
        t.setFechaCambioEstado(AHORA);
        return tareas.save(t);
    }

    @Test
    void guardaYLeeElGrafoBasico() {
        Usuario docente = usuario("docente.mapeo@u.edu.co", RolPlataforma.DOCENTE);
        Usuario lider = usuario("lider.mapeo@u.edu.co", RolPlataforma.ESTUDIANTE);
        Proyecto p = new Proyecto();
        p.setIdDocente(docente.getIdUsuario());
        p.setNombre("Proyecto mapeo");
        p.setFechaInicio(LocalDate.of(2026, 8, 3));
        p.setFechaFin(LocalDate.of(2026, 11, 27));
        p = proyectos.save(p);
        Equipo e = new Equipo();
        e.setIdProyecto(p.getIdProyecto());
        e.setNombreEquipo("Equipo mapeo");
        e = equipos.save(e);
        MiembroEquipo m = new MiembroEquipo();
        m.setIdEquipo(e.getIdEquipo());
        m.setIdUsuario(lider.getIdUsuario());
        m.setRolEquipo(RolEquipo.LIDER);
        m.setFechaIngreso(AHORA);
        miembros.save(m);
        Hito h = new Hito();
        h.setIdProyecto(p.getIdProyecto());
        h.setNombreHito("Hito 1");
        h.setOrden(1);
        h.setFechaInicio(LocalDate.of(2026, 8, 3));
        h.setFechaLimite(LocalDate.of(2026, 9, 4));
        h = hitos.save(h);
        HitoEquipo he = new HitoEquipo();
        he.setIdHito(h.getIdHito());
        he.setIdEquipo(e.getIdEquipo());
        he = hitosEquipo.save(he);
        Tarea a = tarea(he, "Diseño", "4.50", lider);
        Tarea b = tarea(he, "Implementación", "8.00", lider);
        DependenciaTarea d = new DependenciaTarea();
        d.setIdTareaPredecesora(a.getIdTarea());
        d.setIdTareaDependiente(b.getIdTarea());
        dependencias.save(d);

        assertThat(proyectos.findByIdDocenteOrderByFechaInicioDesc(docente.getIdUsuario()))
                .extracting(Proyecto::getUsaMonitores).containsExactly(false);
        assertThat(miembros.findById(new MiembroEquipoId(e.getIdEquipo(), lider.getIdUsuario()))).isPresent();
        assertThat(miembros.findByIdEquipo(e.getIdEquipo())).extracting(MiembroEquipo::getRolEquipo)
                .containsExactly(RolEquipo.LIDER);
        assertThat(hitos.findByIdProyectoOrderByOrden(p.getIdProyecto()).get(0).getDiasInactividadAlerta()).isEqualTo(3);
        assertThat(hitosEquipo.findByIdHitoAndIdEquipo(h.getIdHito(), e.getIdEquipo()).orElseThrow().getEstado())
                .isEqualTo(EstadoHitoEquipo.EN_CURSO);
        assertThat(tareas.findByIdHitoEquipo(he.getIdHitoEquipo())).extracting(Tarea::getTitulo)
                .containsExactlyInAnyOrder("Diseño", "Implementación");
        Tarea leida = tareas.findById(a.getIdTarea()).orElseThrow();
        assertThat(leida.getHorasEstimadas()).isEqualByComparingTo("4.50");
        assertThat(leida.getEstado()).isEqualTo(EstadoTarea.PENDIENTE);
        assertThat(leida.getPorcentajeAvance()).isZero();
        assertThat(leida.getEdicionHabilitada()).isTrue();
        assertThat(dependencias.findByIdTareaDependiente(b.getIdTarea()))
                .extracting(DependenciaTarea::getIdTareaPredecesora).containsExactly(a.getIdTarea());
    }

    @Test
    void jpaNoPisaElChatDeTelegram() {
        Usuario u = usuario("chat.mapeo@u.edu.co", RolPlataforma.ESTUDIANTE);
        jdbc.sql("UPDATE usuario SET telegram_chat_id = 777000111 WHERE id_usuario = :id")
                .param("id", u.getIdUsuario()).update();
        Usuario cargado = usuarios.findByEmail("chat.mapeo@u.edu.co").orElseThrow();
        assertThat(cargado.getTelegramChatId()).isEqualTo(777000111L);
        cargado.setNombre("Nombre cambiado");
        usuarios.save(cargado);
        assertThat(jdbc.sql("SELECT telegram_chat_id FROM usuario WHERE id_usuario = :id")
                .param("id", u.getIdUsuario()).query(Long.class).single()).isEqualTo(777000111L);
        assertThat(jdbc.sql("SELECT nombre FROM usuario WHERE id_usuario = :id")
                .param("id", u.getIdUsuario()).query(String.class).single()).isEqualTo("Nombre cambiado");
    }

    @Test
    void bandejaYCorreosHabilitados() {
        Usuario docente = usuario("docente.bandeja@u.edu.co", RolPlataforma.DOCENTE);
        Notificacion n = new Notificacion();
        n.setIdDestinatario(docente.getIdUsuario());
        n.setTipo("hito.enviado");
        n.setMensaje("El equipo A envió el Hito 1");
        n.setEntidadReferencia("hito_equipo");
        n.setIdReferencia(99L);
        n.setFechaCreacion(AHORA);
        n.setEstadoEnvio(EstadoEnvioNotificacion.SIN_TELEGRAM);
        notificaciones.save(n);
        assertThat(notificaciones.countByIdDestinatarioAndLeidaFalse(docente.getIdUsuario())).isEqualTo(1L);
        assertThat(notificaciones.findTop50ByIdDestinatarioOrderByFechaCreacionDesc(docente.getIdUsuario()))
                .extracting(Notificacion::getMensaje).containsExactly("El equipo A envió el Hito 1");

        CorreoHabilitado c = new CorreoHabilitado();
        c.setEmail("nuevo.docente@u.edu.co");
        c.setRolPlataforma(RolPlataforma.DOCENTE);
        c.setIdHabilitadoPor(docente.getIdUsuario());
        c.setFechaHabilitacion(AHORA);
        correos.save(c);
        assertThat(correos.findByEmailAndFechaUsoIsNull("nuevo.docente@u.edu.co"))
                .extracting(CorreoHabilitado::getRolPlataforma).containsExactly(RolPlataforma.DOCENTE);
        assertThat(usuarios.existsByRolPlataforma(RolPlataforma.ADMINISTRADOR)).isFalse();
    }

    @Test
    void auditoriaSeLeePeroNoSeModifica() {
        auditoria.registrar(EntradaAuditoria.cambio(null, "tarea", 4242L, AccionAuditoria.CAMBIAR_ESTADO, "estado",
                "PENDIENTE", "EN_PROGRESO"));
        List<LogAuditoria> l = logs.findByEntidadAfectadaAndIdRegistroAfectadoOrderByFechaHora("tarea", 4242L);
        assertThat(l).hasSize(1);
        assertThat(l.get(0).getAccion()).isEqualTo(AccionAuditoria.CAMBIAR_ESTADO);
        assertThat(l.get(0).getValorNuevo()).isEqualTo("EN_PROGRESO");
        assertThatThrownBy(() -> jdbc.sql("UPDATE log_auditoria SET valor_nuevo = 'x' WHERE id_registro_afectado = 4242").update())
                .isInstanceOf(DataAccessException.class).hasMessageContaining("solo inserción");
    }
}
