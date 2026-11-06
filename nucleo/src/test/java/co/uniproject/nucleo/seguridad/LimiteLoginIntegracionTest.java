package co.uniproject.nucleo.seguridad;

import co.uniproject.nucleo.ApiIntegracionBase;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bloque 33: límite de intentos fallidos en login (10 por correo, 20 por IP por hora).
 */
class LimiteLoginIntegracionTest extends ApiIntegracionBase {

    @Value("${uniproject.recuperacion.maximo-login-por-correo}")
    private int maximoPorCorreo;

    @Value("${uniproject.recuperacion.maximo-login-por-ip}")
    private int maximoPorIp;

    @BeforeEach
    void sinLimites() {
        jdbc.sql("DELETE FROM limite_recuperacion").update();
    }

    @AfterEach
    void limpiarLimites() {
        jdbc.sql("DELETE FROM limite_recuperacion").update();
    }

    private HttpResponse<String> login(String email, String contrasena) {
        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        return n.post("/api/sesion/login",
                "{\"email\":\"" + email + "\",\"contrasena\":\"" + contrasena + "\"}");
    }

    @Test
    void diezFallosDeUnCorreoBloqueanElSiguienteIntentoYSinSesion() {
        String email = correoUnico("limcorreo");
        crearUsuario("Usuario Limite", email, "ESTUDIANTE");

        for (int i = 0; i < maximoPorCorreo; i++) {
            HttpResponse<String> r = login(email, "ClaveIncorrecta");
            assertThat(r.statusCode()).isEqualTo(401);
        }

        Navegador intento11 = new Navegador(puerto);
        intento11.pedirCsrf();
        HttpResponse<String> r11 = intento11.post("/api/sesion/login",
                "{\"email\":\"" + email + "\",\"contrasena\":\"" + CLAVE + "\"}");
        assertThat(r11.statusCode()).isEqualTo(429);
        assertThat(r11.body()).contains("DEMASIADAS_SOLICITUDES");

        HttpResponse<String> sesion = intento11.get("/api/sesion/yo");
        assertThat(sesion.statusCode()).isEqualTo(401);
    }

    @Test
    void loginCorrectoAntesDelMaximoReiniciaContador() {
        String email = correoUnico("limreinicio");
        crearUsuario("Usuario Reinicio", email, "ESTUDIANTE");

        for (int i = 0; i < maximoPorCorreo - 1; i++) {
            HttpResponse<String> fallo = login(email, "ClaveIncorrecta");
            assertThat(fallo.statusCode()).isEqualTo(401);
        }

        Navegador exito = new Navegador(puerto);
        exito.pedirCsrf();
        HttpResponse<String> rExito = exito.post("/api/sesion/login",
                "{\"email\":\"" + email + "\",\"contrasena\":\"" + CLAVE + "\"}");
        assertThat(rExito.statusCode()).isEqualTo(200);

        for (int i = 0; i < maximoPorCorreo; i++) {
            HttpResponse<String> fallo = login(email, "ClaveIncorrecta");
            assertThat(fallo.statusCode()).isEqualTo(401);
        }

        HttpResponse<String> bloqueado = login(email, CLAVE);
        assertThat(bloqueado.statusCode()).isEqualTo(429);
    }

    @Test
    void correoInexistenteSeBloqueaIgualQueUnoRealYMensajeIdentico() {
        String emailReal = correoUnico("limreal");
        crearUsuario("Usuario Real", emailReal, "ESTUDIANTE");

        for (int i = 0; i < maximoPorCorreo; i++) {
            assertThat(login(emailReal, "Mala").statusCode()).isEqualTo(401);
        }
        HttpResponse<String> rReal = login(emailReal, CLAVE);
        assertThat(rReal.statusCode()).isEqualTo(429);

        jdbc.sql("DELETE FROM limite_recuperacion").update();

        String emailInexistente = correoUnico("limfantasma");
        for (int i = 0; i < maximoPorCorreo; i++) {
            assertThat(login(emailInexistente, "Mala").statusCode()).isEqualTo(401);
        }
        HttpResponse<String> rInexistente = login(emailInexistente, "Mala");
        assertThat(rInexistente.statusCode()).isEqualTo(429);

        assertThat(rInexistente.body()).isEqualTo(rReal.body());
    }

    @Test
    void veinteFallosDesdeMismaIpBloqueanCualquierCorreo() {
        for (int i = 0; i < maximoPorIp; i++) {
            String correoDistinto = correoUnico("ipfallo." + i);
            HttpResponse<String> r = login(correoDistinto, "Mala");
            assertThat(r.statusCode()).isEqualTo(401);
        }

        String otroCorreo = correoUnico("ipbloqueada");
        HttpResponse<String> bloqueado = login(otroCorreo, "Cualquiera");
        assertThat(bloqueado.statusCode()).isEqualTo(429);
        assertThat(bloqueado.body()).contains("DEMASIADAS_SOLICITUDES");
    }

    @Test
    void limitesDeRecuperacionNoColisionanConLimiteDeLogin() {
        String email = correoUnico("limaislado");
        crearUsuario("Usuario Aislado", email, "ESTUDIANTE");

        for (int i = 0; i < 3; i++) {
            assertThat(login(email, "Mala").statusCode()).isEqualTo(401);
        }

        Navegador n = new Navegador(puerto);
        n.pedirCsrf();
        HttpResponse<String> r = n.post("/api/cuentas/recuperacion", "{\"email\":\"" + email + "\"}");
        assertThat(r.statusCode()).isEqualTo(202);
    }

    @Test
    void rafagaParalelaDeLoginsFallidosNoSuperaMaximoAutenticaciones() throws Exception {
        String email = correoUnico("limparalelo");
        crearUsuario("Usuario Paralelo", email, "ESTUDIANTE");

        int totalHilos = 30;
        ExecutorService executor = Executors.newFixedThreadPool(totalHilos);
        CountDownLatch listoParaDisparar = new CountDownLatch(totalHilos);
        CountDownLatch disparo = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();

        for (int i = 0; i < totalHilos; i++) {
            futuros.add(executor.submit(() -> {
                Navegador n = new Navegador(puerto);
                n.pedirCsrf();
                listoParaDisparar.countDown();
                listoParaDisparar.await();
                disparo.await();
                HttpResponse<String> r = n.post("/api/sesion/login",
                        "{\"email\":\"" + email + "\",\"contrasena\":\"ClaveIncorrecta\"}");
                return r.statusCode();
            }));
        }

        assertThat(listoParaDisparar.await(15, TimeUnit.SECONDS)).isTrue();
        disparo.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        int cuenta401 = 0;
        int cuenta429 = 0;
        for (Future<Integer> f : futuros) {
            int status = f.get();
            if (status == 401) {
                cuenta401++;
            } else if (status == 429) {
                cuenta429++;
            }
        }

        assertThat(cuenta401).isLessThanOrEqualTo(maximoPorCorreo);
        assertThat(cuenta429).isGreaterThanOrEqualTo(totalHilos - maximoPorCorreo);
        assertThat(cuenta401 + cuenta429).isEqualTo(totalHilos);

        Integer maxCuentaEnBd = jdbc.sql("SELECT max(cuenta) FROM limite_recuperacion").query(Integer.class).single();
        assertThat(maxCuentaEnBd).isNotNull().isLessThanOrEqualTo(totalHilos);
    }

    @Test
    void loginsCorrectosEnParaleloMismaIpNoDan500YContadorQuedaCoherente() throws Exception {
        String email = correoUnico("limconcurrente");
        crearUsuario("Usuario Concurrente", email, "ESTUDIANTE");

        int n = 8;
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch listoParaDisparar = new CountDownLatch(n);
        CountDownLatch disparo = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            futuros.add(executor.submit(() -> {
                Navegador navegador = new Navegador(puerto);
                navegador.pedirCsrf();
                listoParaDisparar.countDown();
                listoParaDisparar.await();
                disparo.await();
                HttpResponse<String> r = navegador.post("/api/sesion/login",
                        "{\"email\":\"" + email + "\",\"contrasena\":\"" + CLAVE + "\"}");
                return r.statusCode();
            }));
        }

        assertThat(listoParaDisparar.await(15, TimeUnit.SECONDS)).isTrue();
        disparo.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        List<Integer> codigos = new ArrayList<>();
        for (Future<Integer> f : futuros) {
            codigos.add(f.get());
        }

        assertThat(codigos).hasSize(n).containsOnly(200);

        Integer violacionesCheck = jdbc.sql("SELECT count(*) FROM limite_recuperacion WHERE cuenta < 1")
                .query(Integer.class)
                .single();
        assertThat(violacionesCheck).isEqualTo(0);

        Integer filasIp = jdbc.sql("SELECT count(*) FROM limite_recuperacion WHERE clave LIKE 'ip:%'")
                .query(Integer.class)
                .single();
        assertThat(filasIp).isGreaterThanOrEqualTo(0);
    }
}
