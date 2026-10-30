package co.uniproject.nucleo.trazabilidad.auditoria;

import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Inserta con JdbcClient: comparte la transacción del servicio que llama (JpaTransactionManager), como la outbox. */
@Component
public class RegistroAuditoriaJdbc implements RegistroAuditoria {

    static final int MAXIMO_VALOR = 1000;

    private final JdbcClient jdbc;
    private final Clock reloj;

    public RegistroAuditoriaJdbc(JdbcClient jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Override
    public void registrar(EntradaAuditoria e) {
        jdbc.sql("INSERT INTO log_auditoria (id_usuario_accion, entidad_afectada, id_registro_afectado, accion, "
                        + "campo_modificado, valor_anterior, valor_nuevo, fecha_hora) "
                        + "VALUES (:usuario, :entidad, :registro, :accion, :campo, :anterior, :nuevo, :fechaHora)")
                .param("usuario", e.idUsuarioAccion(), Types.BIGINT)
                .param("entidad", e.entidadAfectada())
                .param("registro", e.idRegistroAfectado(), Types.BIGINT)
                .param("accion", e.accion().name())
                .param("campo", e.campoModificado(), Types.VARCHAR)
                .param("anterior", recortar(e.valorAnterior()), Types.VARCHAR)
                .param("nuevo", recortar(e.valorNuevo()), Types.VARCHAR)
                .param("fechaHora", OffsetDateTime.ofInstant(Instant.now(reloj), ZoneOffset.UTC))
                .update();
    }

    static String recortar(String valor) {
        if (valor == null || valor.length() <= MAXIMO_VALOR) {
            return valor;
        }
        return valor.substring(0, MAXIMO_VALOR - 3) + "...";
    }
}
