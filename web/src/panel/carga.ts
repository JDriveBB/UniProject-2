import type { PanelProyecto, ProyectoDetalle } from '../api/tipos';

export interface DatosPanel {
  panel: PanelProyecto;
  detalle: ProyectoDetalle;
}

export interface ApiCargaPanel {
  panelProyecto(idProyecto: number): Promise<PanelProyecto>;
  detalleProyecto(idProyecto: number): Promise<ProyectoDetalle>;
}

export async function cargarPanel(api: ApiCargaPanel, idProyecto: number): Promise<DatosPanel> {
  const [panel, detalle] = await Promise.all([
    api.panelProyecto(idProyecto),
    api.detalleProyecto(idProyecto),
  ]);
  return { panel, detalle };
}
