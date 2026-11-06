package co.uniproject.nucleo.trazabilidad.auditoria;

import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Inserta con JdbcClient: comparte la transacción del servicio que llama (JpaTransactionManager), como la outbox.
 * El texto libre va a texto_auditoria (bloque 29); en log_auditoria queda solo el hecho.
 */
@Component
public class RegistroAuditoriaJdbc implements RegistroAuditoria {

    static final int MAXIMO_VALOR = 1000;

    private static final String INSERTAR = "INSERT INTO log_auditoria (id_usuario_accion, entidad_afectada, "
            + "id_registro_afectado, accion, campo_modificado, valor_anterior, valor_nuevo, fecha_hora) "
            + "VALUES (:usuario, :entidad, :registro, :accion, :campo, :anterior, :nuevo, :fechaHora)";

    private final JdbcClient jdbc;
    private final Clock reloj;

    public RegistroAuditoriaJdbc(JdbcClient jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Override
    public void registrar(EntradaAuditoria e) {
        insertar(INSERTAR, e, recortar(e.valorAnterior()), recortar(e.valorNuevo())).update();
    }

    @Override
    public void registrarTexto(EntradaAuditoria e) {
        long idLog = insertar(INSERTAR + " RETURNING id_log", e, null, null).query(Long.class).single();
        String anterior = recortar(e.valorAnterior());
        String nuevo = recortar(e.valorNuevo());
        if (anterior == null && nuevo == null) {
            return;
        }
        jdbc.sql("INSERT INTO texto_auditoria (id_log, valor_anterior, valor_nuevo) VALUES (:log, :anterior, :nuevo)")
                .param("log", idLog)
                .param("anterior", anterior, Types.VARCHAR)
                .param("nuevo", nuevo, Types.VARCHAR)
                .update();
    }

    private JdbcClient.StatementSpec insertar(String sql, EntradaAuditoria e, String anterior, String nuevo) {
        return jdbc.sql(sql)
                .param("usuario", e.idUsuarioAccion(), Types.BIGINT)
                .param("entidad", e.entidadAfectada())
                .param("registro", e.idRegistroAfectado(), Types.BIGINT)
                .param("accion", e.accion().name())
                .param("campo", e.campoModificado(), Types.VARCHAR)
                .param("anterior", anterior, Types.VARCHAR)
                .param("nuevo", nuevo, Types.VARCHAR)
                .param("fechaHora", OffsetDateTime.ofInstant(Instant.now(reloj), ZoneOffset.UTC));
    }

    static String recortar(String valor) {
        if (valor == null || valor.length() <= MAXIMO_VALOR) {
            return valor;
        }
        return valor.substring(0, MAXIMO_VALOR - 3) + "...";
    }
}
