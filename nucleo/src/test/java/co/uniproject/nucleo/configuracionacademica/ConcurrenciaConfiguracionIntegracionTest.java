package co.uniproject.nucleo.configuracionacademica;

import co.uniproject.nucleo.ApiIntegracionBase;
import co.uniproject.nucleo.comun.FechasDominio;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Bloque 44: cambios de líder y altas de criterios simultáneos se serializan con un candado de fila. */
class ConcurrenciaConfiguracionIntegracionTest extends ApiIntegracionBase {

    @Autowired
    private DataSource fuente;

    @Test
    void dosCambiosDeLiderSimultaneosDejanUnSoloLider() throws Exception {
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Lider", docente, "DOCENTE");
        long proyecto = proyecto(idDocente);
        long equipo = jdbc.sql("INSERT INTO equipo (id_proyecto, nombre_equipo) VALUES (:p, 'Equipo L') "
                + "RETURNING id_equipo").param("p", proyecto).query(Long.class).single();
        long a = miembro(equipo, "LIDER", "A");
        long b = miembro(equipo, "INTEGRANTE", "B");
        long c = miembro(equipo, "INTEGRANTE", "C");
        Navegador n1 = sesion(docente);
        Navegador n2 = sesion(docente);
        String ruta = "/api/equipos/" + equipo + "/lider";
        try (Connection otra = fuente.getConnection()) {
            bloquear(otra, "SELECT 1 FROM equipo WHERE id_equipo = ? FOR NO KEY UPDATE", equipo);
            CompletableFuture<HttpResponse<String>> p1 = CompletableFuture.supplyAsync(
                    () -> n1.put(ruta, "{\"idUsuario\":" + b + "}"));
            esperarEsperas(1);
            CompletableFuture<HttpResponse<String>> p2 = CompletableFuture.supplyAsync(
                    () -> n2.put(ruta, "{\"idUsuario\":" + c + "}"));
            esperarEsperas(2);
            otra.commit();
            assertThat(p1.get(30, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            assertThat(p2.get(30, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
        }
        assertThat(jdbc.sql("SELECT id_usuario FROM miembro_equipo WHERE id_equipo = :e AND rol_equipo = 'LIDER'")
                .param("e", equipo).query(Long.class).list()).hasSize(1).doesNotContain(a);
    }

    @Test
    void dosAltasDeCriterioQueSumanMasDeCienSoloUnaEntra() throws Exception {
        String docente = correoUnico("doc");
        long idDocente = crearUsuario("Docente Rubrica", docente, "DOCENTE");
        long proyecto = proyecto(idDocente);
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        long hito = jdbc.sql("INSERT INTO hito (id_proyecto, nombre_hito, orden, fecha_inicio, fecha_limite) "
                        + "VALUES (:p, 'H', 1, :i, :f) RETURNING id_hito")
                .param("p", proyecto).param("i", hoy).param("f", hoy.plusDays(10)).query(Long.class).single();
        jdbc.sql("INSERT INTO criterio_rubrica (id_hito, descripcion, peso_porcentaje, puntaje_maximo) "
                + "VALUES (:h, 'Base', 60, 5)").param("h", hito).update();
        Navegador n1 = sesion(docente);
        Navegador n2 = sesion(docente);
        String ruta = "/api/hitos/" + hito + "/criterios";
        String cuerpo = "{\"descripcion\":\"Nuevo\",\"pesoPorcentaje\":30,\"puntajeMaximo\":5}";
        try (Connection otra = fuente.getConnection()) {
            bloquear(otra, "SELECT 1 FROM hito WHERE id_hito = ? FOR NO KEY UPDATE", hito);
            CompletableFuture<HttpResponse<String>> p1 = CompletableFuture.supplyAsync(() -> n1.post(ruta, cuerpo));
            esperarEsperas(1);
            CompletableFuture<HttpResponse<String>> p2 = CompletableFuture.supplyAsync(() -> n2.post(ruta, cuerpo));
            esperarEsperas(2);
            otra.commit();
            HttpResponse<String> r1 = p1.get(30, TimeUnit.SECONDS);
            HttpResponse<String> r2 = p2.get(30, TimeUnit.SECONDS);
            assertThat(r1.statusCode()).as(r1.body()).isEqualTo(201);
            assertThat(r2.statusCode()).as(r2.body()).isEqualTo(409);
            assertThat(r2.body()).contains("\"codigo\":\"PESOS_EXCEDEN_100\"");
        }
        assertThat(jdbc.sql("SELECT sum(peso_porcentaje) FROM criterio_rubrica WHERE id_hito = :h")
                .param("h", hito).query(BigDecimal.class).single()).isEqualByComparingTo("90");
    }

    private long proyecto(long idDocente) {
        LocalDate hoy = LocalDate.now(FechasDominio.ZONA);
        return jdbc.sql("INSERT INTO proyecto (id_docente, nombre, fecha_inicio, fecha_fin) "
                        + "VALUES (:d, 'Proyecto Concurrencia', :i, :f) RETURNING id_proyecto")
                .param("d", idDocente).param("i", hoy.minusDays(1)).param("f", hoy.plusDays(60))
                .query(Long.class).single();
    }

    private long miembro(long equipo, String rol, String nombre) {
        long usuario = crearUsuario("Estudiante " + nombre, correoUnico("est"), "ESTUDIANTE");
        jdbc.sql("INSERT INTO miembro_equipo (id_equipo, id_usuario, rol_equipo) VALUES (:e, :u, :r)")
                .param("e", equipo).param("u", usuario).param("r", rol).update();
        return usuario;
    }

    private static void bloquear(Connection conexion, String sql, long id) throws Exception {
        conexion.setAutoCommit(false);
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeQuery();
        }
    }

    /** Espera acotada a que haya al menos n sesiones detenidas por un candado de fila (sin sleeps de orden). */
    private void esperarEsperas(int n) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < limite) {
            int esperando = jdbc.sql("SELECT count(*) FROM pg_locks WHERE NOT granted "
                            + "AND locktype IN ('transactionid','tuple')")
                    .query(Integer.class).single();
            if (esperando >= n) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("La petición " + n + " no esperó el candado");
    }
}
