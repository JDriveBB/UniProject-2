package co.uniproject.nucleo.comun.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Decisión 25-2: el SPA vive en el mismo origen. Las rutas del frontend (web/src/rutas.ts, incluidas las de
 * RutasWeb) devuelven index.html para que React las resuelva al recargar o al abrir un enlace de Telegram.
 * Lista explícita: /api/**, /interno/** y /actuator/** nunca caen aquí.
 * Las rutas desconocidas fuera de /api, /interno y /actuator las atiende ResolutorErrorSpa (404 con index.html, solo
 * si el navegador pide HTML; bloque 43).
 * Bloque 51: cursos del docente y Mis cursos del estudiante; /equipos/{id}/votacion y /proyectos/{id}/inscripcion ya caen en /equipos/** y /proyectos/**.
 */
@Controller
public class ControladorSpa {

    @GetMapping({
        "/ingresar",
        "/registro",
        "/olvide-contrasena",
        "/restablecer-contrasena",
        "/mis-tareas",
        "/mis-cursos",
        "/notificaciones",
        "/perfil",
        "/panel",
        "/reportes",
        "/tablero",
        "/ruta-critica",
        "/cursos/**",
        "/proyectos/**",
        "/equipos/**",
        "/hitos/**",
        "/tareas/**",
        "/admin/**"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
