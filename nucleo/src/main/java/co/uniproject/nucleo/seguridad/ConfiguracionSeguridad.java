package co.uniproject.nucleo.seguridad;

import co.uniproject.nucleo.seguridad.web.ManejadorAccesoDenegado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionSeguridad {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionSeguridad.class);

    @Bean
    @Order(1)
    public SecurityFilterChain cadenaInterna(HttpSecurity http,
                                             @Value("${uniproject.token-interno}") String tokenInterno) throws Exception {
        if (tokenInterno == null || tokenInterno.isBlank()) {
            log.warn("UNIPROJECT_TOKEN_INTERNO vacío: /interno/** responderá 401");
        }
        http.securityMatcher("/interno/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new FiltroTokenInterno(tokenInterno), AuthorizationFilter.class)
            .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Sesión con cookie + CSRF para SPA (cookie XSRF-TOKEN, cabecera X-XSRF-TOKEN). El login es JSON en
     * ControladorSesion; /api/** exige sesión, /api/admin/** el rol ADMINISTRADOR y el resto es el SPA estático.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain cadenaWeb(HttpSecurity http, ManejadorAccesoDenegado accesoDenegado,
                                         SecurityContextRepository repositorioContexto) throws Exception {
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/sesion/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/sesion/login", "/api/sesion/logout", "/api/cuentas/registro")
                    .permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .csrf(csrf -> csrf.spa())
            .securityContext(c -> c.securityContextRepository(repositorioContexto))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler(accesoDenegado))
            .requestCache(c -> c.disable())
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable());
        return http.build();
    }

    /** El mismo repositorio lo usan la cadena (para leer) y ControladorSesion (para guardar al iniciar sesión). */
    @Bean
    public SecurityContextRepository repositorioContextoSeguridad() {
        return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    /** Restricción 3: bcrypt con costo 12. */
    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder(12);
    }

    /** Evita el usuario en memoria (y su contraseña en el log) que Spring Boot crea si no hay ninguno. */
    @Bean
    public UserDetailsService sinUsuariosEnMemoria() {
        return nombre -> {
            throw new UsernameNotFoundException("El inicio de sesión es por POST /api/sesion/login");
        };
    }
}
