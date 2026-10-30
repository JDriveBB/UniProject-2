package co.uniproject.agente.idempotencia;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class RegistroProcesadosJdbc implements RegistroProcesados {

    private final JdbcClient jdbc;

    public RegistroProcesadosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean yaProcesado(UUID eventoId, long usuarioId) {
        return Boolean.TRUE.equals(
                jdbc.sql("SELECT EXISTS (SELECT 1 FROM evento_procesado WHERE evento_id = :eventoId AND usuario_id = :usuarioId)")
                        .param("eventoId", eventoId)
                        .param("usuarioId", usuarioId)
                        .query(Boolean.class)
                        .single()
        );
    }

    @Override
    public void registrar(UUID eventoId, long usuarioId) {
        jdbc.sql("INSERT INTO evento_procesado (evento_id, usuario_id) VALUES (:eventoId, :usuarioId) ON CONFLICT DO NOTHING")
                .param("eventoId", eventoId)
                .param("usuarioId", usuarioId)
                .update();
    }
}
