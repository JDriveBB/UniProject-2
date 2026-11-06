import { api } from './cliente';
import type { Bandeja } from './tipos';

/** Evento emitido cuando cambia el estado de la bandeja de notificaciones. */
export const EVENTO_BANDEJA_CAMBIO = 'uniproject:bandeja-cambio';

/** Avisa en la ventana actual que hubo cambios en la bandeja de notificaciones. */
export function avisarCambioBandeja(): void {
  window.dispatchEvent(new Event(EVENTO_BANDEJA_CAMBIO));
}

/** bandeja: GET /api/notificaciones (P10). La barra solo usa noLeidas (decisión 26b-3). */
export function bandeja(): Promise<Bandeja> {
  return api.get<Bandeja>('/api/notificaciones');
}

/** marcarLeida: POST /api/notificaciones/{id}/leida (204; 403 si no existe o es de otro). */
export function marcarLeida(idNotificacion: number): Promise<void> {
  return api.post<void>(`/api/notificaciones/${idNotificacion}/leida`);
}

/** marcarTodasLeidas: POST /api/notificaciones/leidas (204). */
export function marcarTodasLeidas(): Promise<void> {
  return api.post<void>('/api/notificaciones/leidas');
}
