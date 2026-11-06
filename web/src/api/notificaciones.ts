import { api } from './cliente';
import type { Bandeja } from './tipos';

/** bandeja: GET /api/notificaciones (P10). La barra solo usa noLeidas (decisión 26b-3). */
export function bandeja(): Promise<Bandeja> {
  return api.get<Bandeja>('/api/notificaciones');
}
