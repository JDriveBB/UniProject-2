import { api } from './cliente';
import type { EntradaHistorial } from './tipos';

/** GET /api/equipos/{idEquipo}/historial?limite={limite}. Quien ve el equipo: docente dueño, monitor o integrantes; 403 si no o si no existe; más reciente primero. */
export function historialEquipo(
  idEquipo: number,
  limite: number
): Promise<EntradaHistorial[]> {
  return api.get<EntradaHistorial[]>(`/api/equipos/${idEquipo}/historial?limite=${limite}`);
}

/** GET /api/equipos/{idEquipo}/reporte.pdf. RF15: solo el docente dueño; 403 también si el equipo no existe; se genera al pedirlo y no se guarda. */
export function reporteEquipo(idEquipo: number): Promise<Blob> {
  return api.descargar(`/api/equipos/${idEquipo}/reporte.pdf`);
}
