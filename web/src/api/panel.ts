import { api } from './cliente';
import type { PanelProyecto } from './tipos';

/** GET /api/proyectos/{idProyecto}/panel. Solo el docente dueño; 403 si no o si no existe. */
export function panelProyecto(idProyecto: number): Promise<PanelProyecto> {
  return api.get<PanelProyecto>(`/api/proyectos/${idProyecto}/panel`);
}
