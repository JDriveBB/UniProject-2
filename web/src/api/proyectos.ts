import { api } from './cliente';
import type {
  CorreoHabilitadoVista,
  CriterioVista,
  CursoResumen,
  EnlaceRegistro,
  EquipoVista,
  EstudianteDisponibleVista,
  EstudianteProyectoVista,
  HitoVista,
  InscripcionVista,
  MonitorVista,
  ProyectoDetalle,
  ProyectoVista,
  ResultadoCursosProyecto,
  ResultadoHabilitacion,
  ResultadoReparto,
  SolicitudCorreos,
  SolicitudCriterio,
  SolicitudCupo,
  SolicitudCursoEstudiante,
  SolicitudCursosProyecto,
  SolicitudEquipo,
  SolicitudEquiposLote,
  SolicitudHito,
  SolicitudInscripcion,
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

/** crearEquipo: 400 CURSO_EN_PROYECTO_MIXTO o CURSO_REQUERIDO; 409 PROYECTO_SIN_CURSOS, CURSO_NO_ASIGNADO o EQUIPO_REPETIDO. */
export function crearEquipo(idProyecto: number, solicitud: SolicitudEquipo): Promise<EquipoVista> {
  return api.post<EquipoVista>(`/api/proyectos/${idProyecto}/equipos`, solicitud);
}

/** agregarMiembro: 409 ESTUDIANTE_SIN_CURSO, YA_TIENE_EQUIPO, CURSO_DEL_EQUIPO o CUPO_LLENO (se repite con pasarCupo para confirmar). */
export function agregarMiembro(idEquipo: number, email: string, pasarCupo = false): Promise<EquipoVista> {
  const solicitud: SolicitudMiembro = pasarCupo ? { email, pasarCupo: true } : { email };
  return api.post<EquipoVista>(`/api/equipos/${idEquipo}/miembros`, solicitud);
}

/** listarEstudiantes: participantes del proyecto con su curso y equipo. Solo el docente dueño (403). */
export function listarEstudiantes(idProyecto: number): Promise<EstudianteProyectoVista[]> {
  return api.get<EstudianteProyectoVista[]>(`/api/proyectos/${idProyecto}/estudiantes`);
}

/** listarEstudiantesDisponibles: participantes sin equipo en el proyecto, con su curso. Solo el docente dueño (403). */
export function listarEstudiantesDisponibles(idProyecto: number): Promise<EstudianteDisponibleVista[]> {
  return api.get<EstudianteDisponibleVista[]>(`/api/proyectos/${idProyecto}/estudiantes-disponibles`);
}

/** cambiarLider: 409 NO_ES_MIEMBRO. */
export function cambiarLider(idEquipo: number, idUsuario: number): Promise<EquipoVista> {
  const solicitud: SolicitudLider = { idUsuario };
  return api.put<EquipoVista>(`/api/equipos/${idEquipo}/lider`, solicitud);
}

/** habilitarCorreos: solo monitores. 409 PROYECTO_SIN_MONITORES si no tiene usaMonitores guardado. */
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

/** crearEquiposEnLote: 400 CURSO_EN_PROYECTO_MIXTO o CURSO_REQUERIDO; 409 PROYECTO_SIN_CURSOS, CURSO_NO_ASIGNADO, EQUIPO_REPETIDO, SIN_ESTUDIANTES_SIN_EQUIPO o DEMASIADOS_EQUIPOS. */
export function crearEquiposEnLote(idProyecto: number, solicitud: SolicitudEquiposLote): Promise<EquipoVista[]> {
  return api.post<EquipoVista[]>(`/api/proyectos/${idProyecto}/equipos/lote`, solicitud);
}

/** repartirEquipos: asigna al azar a los estudiantes sin equipo según curso y cupo. */
export function repartirEquipos(idProyecto: number): Promise<ResultadoReparto> {
  return api.post<ResultadoReparto>(`/api/proyectos/${idProyecto}/equipos/reparto`);
}

/** vistaInscripcion: estado de inscripción a equipos del proyecto. */
export function vistaInscripcion(idProyecto: number): Promise<InscripcionVista> {
  return api.get<InscripcionVista>(`/api/proyectos/${idProyecto}/inscripcion`);
}

/** configurarInscripcion: configura cierreEquipos y horasRonda del proyecto. 400 CIERRE_EN_EL_PASADO o VALIDACION; 409 INSCRIPCION_YA_CERRADA. */
export function configurarInscripcion(
  idProyecto: number,
  solicitud: SolicitudInscripcion,
): Promise<InscripcionVista> {
  return api.put<InscripcionVista>(`/api/proyectos/${idProyecto}/inscripcion`, solicitud);
}

/** listarCursosProyecto: cursos asignados al proyecto. */
export function listarCursosProyecto(idProyecto: number): Promise<CursoResumen[]> {
  return api.get<CursoResumen[]>(`/api/proyectos/${idProyecto}/cursos`);
}

/** asignarCursosProyecto: reemplaza los cursos del proyecto (confirmar=false devuelve vista previa si hay cambios). 409 CURSO_ARCHIVADO. */
export function asignarCursosProyecto(
  idProyecto: number,
  solicitud: SolicitudCursosProyecto,
): Promise<ResultadoCursosProyecto> {
  return api.put<ResultadoCursosProyecto>(`/api/proyectos/${idProyecto}/cursos`, solicitud);
}

/** elegirCursoEstudiante: 409 EQUIPO_DE_OTRO_CURSO o CURSO_NO_VALIDO. */
export function elegirCursoEstudiante(
  idProyecto: number,
  idUsuario: number,
  idCurso: number,
): Promise<EstudianteProyectoVista> {
  const solicitud: SolicitudCursoEstudiante = { idCurso };
  return api.put<EstudianteProyectoVista>(`/api/proyectos/${idProyecto}/estudiantes/${idUsuario}/curso`, solicitud);
}

/** destituirLider: deja el equipo sin líder y abre una votación si no hay una abierta. 409 SIN_LIDER. */
export function destituirLider(idEquipo: number): Promise<EquipoVista> {
  return api.borrar<EquipoVista>(`/api/equipos/${idEquipo}/lider`);
}

/** cambiarCupo: cambia el cupo; puede quedar por debajo de los integrantes actuales sin sacar a nadie. */
export function cambiarCupo(idEquipo: number, cupo: number | null): Promise<EquipoVista> {
  const solicitud: SolicitudCupo = { cupo };
  return api.put<EquipoVista>(`/api/equipos/${idEquipo}/cupo`, solicitud);
}

/** unirseEquipo: 409 EQUIPO_SIN_INSCRIPCION, INSCRIPCION_CERRADA, YA_TIENE_EQUIPO, CURSO_DEL_EQUIPO o EQUIPO_LLENO. */
export function unirseEquipo(idEquipo: number): Promise<EquipoVista> {
  return api.post<EquipoVista>(`/api/equipos/${idEquipo}/inscripcion`);
}

/** salirseEquipo: 204. 404 si no está en el equipo; 409 EQUIPO_SIN_INSCRIPCION, INSCRIPCION_CERRADA o TIENE_TAREAS. */
export function salirseEquipo(idEquipo: number): Promise<void> {
  return api.borrar<void>(`/api/equipos/${idEquipo}/inscripcion`);
}
