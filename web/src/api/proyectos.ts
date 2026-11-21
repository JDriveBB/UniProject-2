import { api } from './cliente';
import type {
  CorreoHabilitadoVista,
  CriterioVista,
  EnlaceRegistro,
  EquipoVista,
  EstudianteDisponibleVista,
  EstudianteProyectoVista,
  HitoVista,
  MonitorVista,
  ProyectoDetalle,
  ProyectoVista,
  ResultadoHabilitacion,
  SolicitudCorreos,
  SolicitudCriterio,
  SolicitudEquipo,
  SolicitudHito,
  SolicitudInvitacion,
  SolicitudLider,
  SolicitudMiembro,
  SolicitudProyecto,
} from './tipos';

/** listarProyectos: los que el usuario puede ver (dueño, integrante o monitor). */
export function listarProyectos(): Promise<ProyectoVista[]> {
  return api.get<ProyectoVista[]>('/api/proyectos');
}

/** detalleProyecto: 403 si no lo puede ver o no existe (decisión 15-2). */
export function detalleProyecto(idProyecto: number): Promise<ProyectoDetalle> {
  return api.get<ProyectoDetalle>(`/api/proyectos/${idProyecto}`);
}

/** crearProyecto: solo DOCENTE. */
export function crearProyecto(solicitud: SolicitudProyecto): Promise<ProyectoVista> {
  return api.post<ProyectoVista>('/api/proyectos', solicitud);
}

/** actualizarProyecto: 409 HITOS_FUERA_DE_FECHAS si un hito queda fuera de las fechas nuevas. */
export function actualizarProyecto(idProyecto: number, solicitud: SolicitudProyecto): Promise<ProyectoVista> {
  return api.put<ProyectoVista>(`/api/proyectos/${idProyecto}`, solicitud);
}

/** crearHito: 400 FECHAS_FUERA_DEL_PROYECTO; 409 ORDEN_REPETIDO. */
export function crearHito(idProyecto: number, solicitud: SolicitudHito): Promise<HitoVista> {
  return api.post<HitoVista>(`/api/proyectos/${idProyecto}/hitos`, solicitud);
}

/** actualizarHito: PUT, reemplaza todo el hito. */
export function actualizarHito(idHito: number, solicitud: SolicitudHito): Promise<HitoVista> {
  return api.put<HitoVista>(`/api/hitos/${idHito}`, solicitud);
}

/** crearCriterio: 409 PESOS_EXCEDEN_100 o RUBRICA_EN_USO. */
export function crearCriterio(idHito: number, solicitud: SolicitudCriterio): Promise<CriterioVista> {
  return api.post<CriterioVista>(`/api/hitos/${idHito}/criterios`, solicitud);
}

export function actualizarCriterio(
  idHito: number,
  idCriterio: number,
  solicitud: SolicitudCriterio,
): Promise<CriterioVista> {
  return api.put<CriterioVista>(`/api/hitos/${idHito}/criterios/${idCriterio}`, solicitud);
}

export function eliminarCriterio(idHito: number, idCriterio: number): Promise<void> {
  return api.borrar<void>(`/api/hitos/${idHito}/criterios/${idCriterio}`);
}

/** crearEquipo: 409 EQUIPO_REPETIDO. */
export function crearEquipo(idProyecto: number, nombreEquipo: string): Promise<EquipoVista> {
  const solicitud: SolicitudEquipo = { nombreEquipo };
  return api.post<EquipoVista>(`/api/proyectos/${idProyecto}/equipos`, solicitud);
}

/** agregarMiembro: 409 CORREO_NO_HABILITADO, ESTUDIANTE_NO_REGISTRADO o YA_TIENE_EQUIPO. */
export function agregarMiembro(idEquipo: number, email: string): Promise<EquipoVista> {
  const solicitud: SolicitudMiembro = { email };
  return api.post<EquipoVista>(`/api/equipos/${idEquipo}/miembros`, solicitud);
}

/** invitarAlEquipo (D-12): estudiante habilitado sin cuenta; entra al equipo al registrarse. 409 CORREO_NO_HABILITADO, YA_TIENE_CUENTA o YA_INVITADO. */
export function invitarAlEquipo(idEquipo: number, email: string): Promise<EstudianteProyectoVista> {
  const solicitud: SolicitudInvitacion = { email };
  return api.post<EstudianteProyectoVista>(`/api/equipos/${idEquipo}/invitaciones`, solicitud);
}

/** listarEstudiantes (D-11, D-12): estudiantes habilitados con su equipo o invitación. Solo el docente dueño (403). */
export function listarEstudiantes(idProyecto: number): Promise<EstudianteProyectoVista[]> {
  return api.get<EstudianteProyectoVista[]>(`/api/proyectos/${idProyecto}/estudiantes`);
}

/** listarEstudiantesDisponibles (T-046): cuentas de estudiante sin equipo ni habilitación en el proyecto. Solo el docente dueño (403). */
export function listarEstudiantesDisponibles(idProyecto: number): Promise<EstudianteDisponibleVista[]> {
  return api.get<EstudianteDisponibleVista[]>(`/api/proyectos/${idProyecto}/estudiantes-disponibles`);
}

/** cambiarLider: 409 NO_ES_MIEMBRO. */
export function cambiarLider(idEquipo: number, idUsuario: number): Promise<EquipoVista> {
  const solicitud: SolicitudLider = { idUsuario };
  return api.put<EquipoVista>(`/api/equipos/${idEquipo}/lider`, solicitud);
}

/** habilitarCorreos: 409 PROYECTO_SIN_MONITORES si se habilita un monitor sin usaMonitores guardado. */
export function habilitarCorreos(idProyecto: number, solicitud: SolicitudCorreos): Promise<ResultadoHabilitacion> {
  return api.post<ResultadoHabilitacion>(`/api/proyectos/${idProyecto}/correos`, solicitud);
}

/** listarMonitores: solo el docente dueño. */
export function listarMonitores(idProyecto: number): Promise<MonitorVista[]> {
  return api.get<MonitorVista[]>(`/api/proyectos/${idProyecto}/monitores`);
}

/** quitarMiembro: sus tareas no cambian (el líder las reasigna). 404 si ya no era miembro. */
export function quitarMiembro(idEquipo: number, idUsuario: number): Promise<EquipoVista> {
  return api.borrar<EquipoVista>(`/api/equipos/${idEquipo}/miembros/${idUsuario}`);
}

/** quitarMonitor: 204. 404 si ya no estaba asignado. También retira su habilitación de monitor: se puede volver a agregar con su correo (bloque 30). */
export function quitarMonitor(idProyecto: number, idMonitor: number): Promise<void> {
  return api.borrar<void>(`/api/proyectos/${idProyecto}/monitores/${idMonitor}`);
}

/** listarCorreos: solo el docente dueño (403). Ordenados por correo. */
export function listarCorreos(idProyecto: number): Promise<CorreoHabilitadoVista[]> {
  return api.get<CorreoHabilitadoVista[]>(`/api/proyectos/${idProyecto}/correos`);
}

/** retirarCorreo: 204. Solo habilitaciones pendientes; 409 HABILITACION_USADA si ya creó su cuenta; 404 si no existe en el proyecto. */
export function retirarCorreo(idProyecto: number, idCorreoHabilitado: number): Promise<void> {
  return api.borrar<void>(`/api/proyectos/${idProyecto}/correos/${idCorreoHabilitado}`);
}

/** generarEnlaceCorreo: 200 (Cache-Control: no-store). Solo habilitaciones pendientes; 409 HABILITACION_USADA; 404 si no existe en el proyecto. */
export function generarEnlaceCorreo(idProyecto: number, idCorreoHabilitado: number): Promise<EnlaceRegistro> {
  return api.post<EnlaceRegistro>(`/api/proyectos/${idProyecto}/correos/${idCorreoHabilitado}/enlace`);
}
