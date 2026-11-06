/**
 * Rutas del SPA. Las cinco primeras son contrato con el backend (eventos-contrato RutasWeb.java: avisos de
 * Telegram y bandeja): no se cambian sin cambiar RutasWeb.java y ControladorSpa.java.
 */
export const rutas = {
  tarea: (idTarea: number) => `/tareas/${idTarea}`,
  misTareas: () => '/mis-tareas',
  tableroEquipo: (idEquipo: number) => `/equipos/${idEquipo}/tablero`,
  evaluarHito: (idHitoEquipo: number) => `/hitos/${idHitoEquipo}/evaluar`,
  evaluacionHito: (idHitoEquipo: number) => `/hitos/${idHitoEquipo}/evaluacion`,
  ingresar: () => '/ingresar',
  registro: () => '/registro',
  /* Bloque 26a-bis. restablecerContrasena es contrato con RutasWeb.restablecerContrasena (el núcleo le agrega #codigo=…); la app nunca construye la URL con el código. */
  olvideContrasena: () => '/olvide-contrasena',
  restablecerContrasena: () => '/restablecer-contrasena',
  proyectos: () => '/proyectos',
  proyecto: (idProyecto: number) => `/proyectos/${idProyecto}`,
  panelProyecto: (idProyecto: number) => `/proyectos/${idProyecto}/panel`,
  historialEquipo: (idEquipo: number) => `/equipos/${idEquipo}/historial`,
  rutaCritica: (idHitoEquipo: number) => `/hitos/${idHitoEquipo}/ruta-critica`,
  notificaciones: () => '/notificaciones',
  perfil: () => '/perfil',
  adminDocentes: () => '/admin/docentes',
  /* Bloque 26b. Las rutas sin id son de la barra: 26c y 26f deciden a qué equipo o proyecto llevan (decisión 26b-2). */
  proyectoNuevo: () => '/proyectos/nuevo',
  panel: () => '/panel',
  reportes: () => '/reportes',
  tablero: () => '/tablero',
  rutaCriticaActual: () => '/ruta-critica',
  /* Bloque 26c: tablero en un hito concreto (pestañas de P04 y «Volver al tablero» de P06). */
  tableroEnHito: (idEquipo: number, idHitoEquipo: number) => `/equipos/${idEquipo}/tablero?hito=${idHitoEquipo}`,
} as const;
