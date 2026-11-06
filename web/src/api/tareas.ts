import { api } from './cliente';
import type {
  ApoyoVista,
  ComentarioVista,
  CorreccionVista,
  EnvioVista,
  EvidenciaVista,
  MiTareaVista,
  PeticionCorreccion,
  ResultadoDependencias,
  ResultadoReasignacion,
  RutaCriticaVista,
  SolicitudApoyo,
  SolicitudAvance,
  SolicitudComentario,
  SolicitudDependencias,
  SolicitudEnlace,
  SolicitudReasignacion,
  SolicitudTarea,
  TableroHito,
  TareaDetalle,
  TareaVista,
} from './tipos';

/** misTareas: tareas a cargo del usuario autenticado en todos sus proyectos. */
export function misTareas(): Promise<MiTareaVista[]> {
  return api.get<MiTareaVista[]>('/api/tareas/mias');
}

/** detalleTarea: quien ve el equipo (integrantes, docente, monitor). 403 si no tiene acceso o no existe. */
export function detalleTarea(idTarea: number): Promise<TareaDetalle> {
  return api.get<TareaDetalle>(`/api/tareas/${idTarea}`);
}

/** editarTarea: solo el líder. 409 USAR_REASIGNACION si la tarea ya tiene un encargado asignado. */
export function editarTarea(idTarea: number, solicitud: SolicitudTarea): Promise<TareaVista> {
  return api.put<TareaVista>(`/api/tareas/${idTarea}`, solicitud);
}

/** quitarDependencia: solo el líder. 403 si no es líder; quita el prerrequisito y recalcula holgura. */
export function quitarDependencia(idTarea: number, idPrerrequisito: number): Promise<TareaVista> {
  return api.borrar<TareaVista>(`/api/tareas/${idTarea}/dependencias/${idPrerrequisito}`);
}

/** actualizarAvance: encargado o líder. 400 porcentaje 0..100; 409 TAREA_BLOQUEADA o CORRECCION_PENDIENTE. */
export function actualizarAvance(idTarea: number, solicitud: SolicitudAvance): Promise<TareaVista> {
  return api.put<TareaVista>(`/api/tareas/${idTarea}/avance`, solicitud);
}

/** listarEvidencias: quien ve el equipo. Lista evidencias de la tarea (archivos y enlaces). */
export function listarEvidencias(idTarea: number): Promise<EvidenciaVista[]> {
  return api.get<EvidenciaVista[]>(`/api/tareas/${idTarea}/evidencias`);
}

/** adjuntarArchivo: encargado o líder. Sube un archivo PDF, PNG o JPEG de hasta 10 MB. */
export function adjuntarArchivo(idTarea: number, archivo: File): Promise<EvidenciaVista> {
  return api.subirArchivo(`/api/tareas/${idTarea}/evidencias`, 'archivo', archivo, archivo.name);
}

/** adjuntarEnlace: encargado o líder. Registra un enlace web como evidencia. */
export function adjuntarEnlace(idTarea: number, url: string): Promise<EvidenciaVista> {
  const solicitud: SolicitudEnlace = { url };
  return api.post<EvidenciaVista>(`/api/tareas/${idTarea}/evidencias`, solicitud);
}

/** listarComentarios: quien ve el equipo. Lista los comentarios en orden de llegada. */
export function listarComentarios(idTarea: number): Promise<ComentarioVista[]> {
  return api.get<ComentarioVista[]>(`/api/tareas/${idTarea}/comentarios`);
}

/** comentarTarea: integrantes, monitor o docente. 409 TAREA_NO_EN_CURSO si el hito no está abierto. */
export function comentarTarea(idTarea: number, contenido: string): Promise<ComentarioVista> {
  const solicitud: SolicitudComentario = { contenido };
  return api.post<ComentarioVista>(`/api/tareas/${idTarea}/comentarios`, solicitud);
}

/** listarCorrecciones: quien ve el equipo. Historial de solicitudes de corrección del líder. */
export function listarCorrecciones(idTarea: number): Promise<CorreccionVista[]> {
  return api.get<CorreccionVista[]>(`/api/tareas/${idTarea}/correcciones`);
}

/** solicitarCorreccion: solo líder. 409 SIN_ENCARGADO, CORRECCION_PENDIENTE o evidencia no pertenece a la tarea. */
export function solicitarCorreccion(idTarea: number, peticion: PeticionCorreccion): Promise<CorreccionVista> {
  return api.post<CorreccionVista>(`/api/tareas/${idTarea}/correcciones`, peticion);
}

/** reasignarTarea: solo líder. Reasigna totalmente o crea una nueva tarea con alcance parcial (RF11). */
export function reasignarTarea(idTarea: number, solicitud: SolicitudReasignacion): Promise<ResultadoReasignacion> {
  return api.post<ResultadoReasignacion>(`/api/tareas/${idTarea}/reasignacion`, solicitud);
}

/** solicitarApoyo: solo el encargado de la tarea. Registra una solicitud de ayuda en la auditoría. */
export function solicitarApoyo(idTarea: number, mensaje: string): Promise<ApoyoVista> {
  const solicitud: SolicitudApoyo = { mensaje };
  return api.post<ApoyoVista>(`/api/tareas/${idTarea}/apoyo`, solicitud);
}

/** tableroHito: quien ve el equipo (integrantes, docente, monitor). 403 si no lo puede ver o no existe. */
export function tableroHito(idHitoEquipo: number): Promise<TableroHito> {
  return api.get<TableroHito>(`/api/hitos-equipo/${idHitoEquipo}/tareas`);
}

/** crearTarea: solo el líder. 400 FECHA_FUERA_DEL_HITO; 409 HITO_NO_EDITABLE o ENCARGADO_NO_ES_MIEMBRO. */
export function crearTarea(idHitoEquipo: number, solicitud: SolicitudTarea): Promise<TareaVista> {
  return api.post<TareaVista>(`/api/hitos-equipo/${idHitoEquipo}/tareas`, solicitud);
}

/** agregarDependencias: 400 PRERREQUISITO_DE_OTRO_HITO; los que formarían ciclo vuelven en "ciclicos". */
export function agregarDependencias(idTarea: number, prerrequisitos: number[]): Promise<ResultadoDependencias> {
  const solicitud: SolicitudDependencias = { prerrequisitos };
  return api.post<ResultadoDependencias>(`/api/tareas/${idTarea}/dependencias`, solicitud);
}

/** rutaCritica: CPM del núcleo (RF3); nodos en orden topológico. */
export function rutaCritica(idHitoEquipo: number): Promise<RutaCriticaVista> {
  return api.get<RutaCriticaVista>(`/api/hitos-equipo/${idHitoEquipo}/ruta-critica`);
}

/** enviarHito: cualquier integrante. 409 HITO_NO_ENVIABLE o PRERREQUISITOS_INCOMPLETOS (títulos en detalles). */
export function enviarHito(idHitoEquipo: number): Promise<EnvioVista> {
  return api.post<EnvioVista>(`/api/hitos-equipo/${idHitoEquipo}/envio`);
}
