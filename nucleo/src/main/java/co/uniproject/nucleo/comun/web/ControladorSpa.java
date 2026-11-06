package co.uniproject.nucleo.comun.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Decisión 25-2: el SPA vive en el mismo origen. Las rutas del frontend (web/src/rutas.ts, incluidas las de
 * RutasWeb) devuelven index.html para que React las resuelva al recargar o al abrir un enlace de Telegram.
 * Lista explícita: /api/**, /interno/** y /actuator/** nunca caen aquí.
 */
@Controller
public class ControladorSpa {

    @GetMapping({"/ingresar", "/registro", "/olvide-contrasena", "/restablecer-contrasena", "/mis-tareas",
            "/notificaciones", "/perfil", "/panel", "/reportes", "/tablero", "/ruta-critica", "/proyectos/**",
            "/equipos/**", "/hitos/**", "/tareas/**", "/admin/**"})
    public String spa() {
        return "forward:/index.html";
    }
}
