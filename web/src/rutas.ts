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
  proyectos: () => '/proyectos',
  proyecto: (idProyecto: number) => `/proyectos/${idProyecto}`,
  panelProyecto: (idProyecto: number) => `/proyectos/${idProyecto}/panel`,
  historialEquipo: (idEquipo: number) => `/equipos/${idEquipo}/historial`,
  rutaCritica: (idHitoEquipo: number) => `/hitos/${idHitoEquipo}/ruta-critica`,
  notificaciones: () => '/notificaciones',
  perfil: () => '/perfil',
  adminDocentes: () => '/admin/docentes',
} as const;
