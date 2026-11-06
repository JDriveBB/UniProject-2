import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { PanelProyecto, ProyectoDetalle } from '../api/tipos';
import { cargarPanel, type ApiCargaPanel } from './carga';

describe('cargaPanel', () => {
  it('orquesta la carga en paralelo y propaga errores', async () => {
    const llamadas: string[] = [];

    const panel: PanelProyecto = {
      idProyecto: 3,
      nombre: 'Proyecto 3',
      generadoEn: '2026-09-23T19:05:00Z',
      equipos: [],
    };

    const detalle: ProyectoDetalle = {
      proyecto: {
        idProyecto: 3,
        idDocente: 5,
        nombre: 'Proyecto 3',
        descripcion: null,
        fechaInicio: '2026-08-01',
        fechaFin: '2026-11-30',
        usaMonitores: false,
      },
      hitos: [],
      equipos: [],
    };

    const api: ApiCargaPanel = {
      async panelProyecto(idProyecto: number): Promise<PanelProyecto> {
        llamadas.push(`panelProyecto ${idProyecto}`);
        return panel;
      },
      async detalleProyecto(idProyecto: number): Promise<ProyectoDetalle> {
        llamadas.push(`detalleProyecto ${idProyecto}`);
        return detalle;
      },
    };

    const datos = await cargarPanel(api, 3);
    expect(datos.panel).toBe(panel);
    expect(datos.detalle).toBe(detalle);
    expect(llamadas).toHaveLength(2);
    expect(llamadas).toContain('panelProyecto 3');
    expect(llamadas).toContain('detalleProyecto 3');

    const error403 = new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado');
    const apiFallo: ApiCargaPanel = {
      ...api,
      async panelProyecto(): Promise<PanelProyecto> {
        throw error403;
      },
    };
    await expect(cargarPanel(apiFallo, 3)).rejects.toBe(error403);
  });
});
