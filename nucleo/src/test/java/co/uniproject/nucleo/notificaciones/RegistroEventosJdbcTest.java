package co.uniproject.nucleo.notificaciones;

import co.uniproject.eventos.CodecEventos;
import co.uniproject.eventos.EventoCloud;
import co.uniproject.eventos.EventoInvalidoException;
import co.uniproject.eventos.TipoEvento;
import co.uniproject.eventos.datos.TareaCorreccion;
import co.uniproject.eventos.ejemplos.EjemplosEventos;
import co.uniproject.nucleo.PostgresPrueba;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistroEventosJdbcTest {

    private DataSource ds;
    private JdbcClient jdbc;
    private TransactionTemplate tx;
    private Clock reloj;
    private CodecEventos codec;
    private RegistroEventosJdbc registro;

    @BeforeEach
    void setUp() {
        ds = PostgresPrueba.baseLimpia("1");
        jdbc = JdbcClient.create(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        reloj = Clock.fixed(Instant.parse("2026-10-24T19:05:00Z"), ZoneId.of("America/Bogota"));
        codec = new CodecEventos();
        registro = new RegistroEventosJdbc(jdbc, codec, reloj);
    }

    @Test
    void insertaElEventoDentroDeLaTransaccion() {
        UUID id = tx.execute(s -> registro.registrar(EjemplosEventos.tareaReasignada()));
        assertThat(id).isNotNull();

        Integer totalFilas = jdbc.sql("SELECT count(*) FROM evento_saliente").query(Integer.class).single();
        assertThat(totalFilas).isEqualTo(1);

        var fila = jdbc.sql("""
                SELECT id, tipo, publicado_en, intentos, carga->>'type' AS carga_type, carga->>'id' AS carga_id
                  FROM evento_saliente
                 WHERE id = :id
                """)
                .param("id", id)
                .query((rs, rowNum) -> Map.of(
                        "id", rs.getObject("id", UUID.class),
                        "tipo", rs.getString("tipo"),
                        "publicado_en_null", rs.getObject("publicado_en") == null,
                        "intentos", rs.getInt("intentos"),
                        "carga_type", rs.getString("carga_type"),
                        "carga_id", rs.getString("carga_id")
                ))
                .single();

        assertThat(fila.get("id")).isEqualTo(id);
        assertThat(fila.get("tipo")).isEqualTo("tarea.reasignada");
        assertThat(fila.get("publicado_en_null")).isEqualTo(true);
        assertThat(fila.get("intentos")).isEqualTo(0);
        assertThat(fila.get("carga_type")).isEqualTo("tarea.reasignada");
        assertThat(fila.get("carga_id")).isEqualTo(id.toString());
    }

    @Test
    void sinTransaccionLanzaIllegalState() {
        assertThatThrownBy(() -> registro.registrar(EjemplosEventos.hitoEnviado()))
                .isInstanceOf(IllegalStateException.class);

        Integer total = jdbc.sql("SELECT count(*) FROM evento_saliente").query(Integer.class).single();
        assertThat(total).isEqualTo(0);
    }

    @Test
    void rollbackDelNegocioNoDejaEvento() {
        tx.executeWithoutResult(s -> {
            registro.registrar(EjemplosEventos.hitoEnviado());
            s.setRollbackOnly();
        });

        Integer total = jdbc.sql("SELECT count(*) FROM evento_saliente").query(Integer.class).single();
        assertThat(total).isEqualTo(0);
    }

    @Test
    void datosInvalidosNoSeInsertan() {
        var ej = EjemplosEventos.tareaCorreccion();
        var invalido = new TareaCorreccion(ej.proyecto(), ej.equipo(), ej.tarea(), ej.responsable(), ej.lider(), "");

        assertThatThrownBy(() -> tx.execute(s -> registro.registrar(invalido)))
                .isInstanceOf(EventoInvalidoException.class);

        Integer total = jdbc.sql("SELECT count(*) FROM evento_saliente").query(Integer.class).single();
        assertThat(total).isEqualTo(0);
    }

    @Test
    void cargaGuardadaSeLeeConElCodec() {
        tx.executeWithoutResult(s -> {
            for (TipoEvento tipo : TipoEvento.values()) {
                registro.registrar(EjemplosEventos.de(tipo));
            }
        });

        List<String> cargas = jdbc.sql("SELECT carga::text FROM evento_saliente ORDER BY creado_en")
                .query(String.class)
                .list();

        assertThat(cargas).hasSize(TipoEvento.values().length);
        for (String carga : cargas) {
            EventoCloud evento = codec.leer(carga.getBytes(StandardCharsets.UTF_8));
            TipoEvento tipo = TipoEvento.desdeClave(evento.type()).orElseThrow();
            assertThat(evento.data()).isEqualTo(EjemplosEventos.de(tipo));
        }
    }
}
