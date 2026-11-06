import { api } from './cliente';
import type {
  EnvioVista,
  ResultadoDependencias,
  RutaCriticaVista,
  SolicitudDependencias,
  SolicitudTarea,
  TableroHito,
  TareaVista,
} from './tipos';

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
