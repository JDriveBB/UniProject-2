package co.uniproject.nucleo.trazabilidad.auditoria;

import co.uniproject.nucleo.PostgresPrueba;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistroAuditoriaJdbcTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T13:00:00Z");

    private DataSource ds;
    private JdbcClient jdbc;
    private RegistroAuditoriaJdbc registro;

    @BeforeEach
    void setUp() {
        ds = PostgresPrueba.baseLimpia("3");
        jdbc = JdbcClient.create(ds);
        registro = new RegistroAuditoriaJdbc(jdbc, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private int filas() {
        return jdbc.sql("SELECT count(*) FROM log_auditoria").query(Integer.class).single();
    }

    @Test
    void insertaLaEntradaConLaHoraDelReloj() {
        registro.registrar(EntradaAuditoria.cambio(null, "tarea", 50L, AccionAuditoria.CAMBIAR_ESTADO, "estado",
                "PENDIENTE", "EN_PROGRESO"));
        String fila = jdbc.sql("SELECT coalesce(id_usuario_accion::text, '-') || '|' || entidad_afectada || '|' "
                + "|| id_registro_afectado || '|' || accion || '|' || campo_modificado || '|' || valor_anterior "
                + "|| '|' || valor_nuevo FROM log_auditoria").query(String.class).single();
        assertThat(fila).isEqualTo("-|tarea|50|CAMBIAR_ESTADO|estado|PENDIENTE|EN_PROGRESO");
        Instant fecha = jdbc.sql("SELECT fecha_hora FROM log_auditoria")
                .query((rs, n) -> rs.getObject(1, OffsetDateTime.class).toInstant()).single();
        assertThat(fecha).isEqualTo(AHORA);
    }

    @Test
    void recortaLosValoresLargos() {
        registro.registrar(EntradaAuditoria.cambio(null, "tarea", 50L, AccionAuditoria.ACTUALIZAR, "descripcion",
                null, "x".repeat(1500)));
        String valor = jdbc.sql("SELECT valor_nuevo FROM log_auditoria").query(String.class).single();
        assertThat(valor).hasSize(1000).endsWith("...");
        assertThat(jdbc.sql("SELECT count(*) FROM log_auditoria WHERE valor_anterior IS NULL").query(Integer.class).single())
                .isEqualTo(1);
    }

    @Test
    void participaEnLaTransaccionDelNegocio() {
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.executeWithoutResult(estado -> {
            registro.registrar(EntradaAuditoria.de(null, "tarea", 50L, AccionAuditoria.CREAR));
            estado.setRollbackOnly();
        });
        assertThat(filas()).isZero();
        registro.registrar(EntradaAuditoria.de(null, "tarea", 50L, AccionAuditoria.CREAR));
        assertThat(filas()).isEqualTo(1);
    }

    @Test
    void entidadYAccionSonObligatorias() {
        assertThatThrownBy(() -> EntradaAuditoria.de(null, null, 1L, AccionAuditoria.CREAR))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> EntradaAuditoria.de(null, "tarea", 1L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void accesoDenegadoGuardaMetodoYRuta() {
        assertThat(EntradaAuditoria.accesoDenegado(7L, "POST", "/api/tareas/5/avance"))
                .isEqualTo(new EntradaAuditoria(7L, "http", null, AccionAuditoria.ACCESO_DENEGADO, null, null,
                        "POST /api/tareas/5/avance"));
    }
}
