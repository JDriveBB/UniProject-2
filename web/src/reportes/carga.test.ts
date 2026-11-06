import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type { EntradaHistorial, TableroHito } from '../api/tipos';
import { cargarTrazabilidad, type ApiTrazabilidad } from './carga';

function entrada(idLog: number): EntradaHistorial {
  return {
    idLog,
    fechaHora: '2026-09-23T15:00:00Z',
    idUsuario: null,
    autor: 'Ana',
    accion: 'CREAR',
    entidad: 'tarea',
    idRegistro: null,
    campo: null,
    valorAnterior: null,
    valorNuevo: null,
    detalle: '',
  };
}

describe('carga de trazabilidad', () => {
  it('carga historial solo o en paralelo con tablero y evaluacion', async () => {
    const llamadas1: string[] = [];
    const api1: ApiTrazabilidad = {
      async historialEquipo(idEquipo: number, limite: number) {
        llamadas1.push(`historialEquipo ${idEquipo} ${limite}`);
        return [entrada(1), entrada(2)];
      },
      async tableroHito(idHitoEquipo: number) {
        llamadas1.push(`tableroHito ${idHitoEquipo}`);
        return {
          idHitoEquipo,
          idHito: 1,
          nombreHito: 'Hito 1',
          fechaLimite: '2026-10-01',
          estado: 'EN_CURSO',
          editable: true,
          idEquipo: 7,
          nombreEquipo: 'Equipo 7',
          idProyecto: 1,
          nombreProyecto: 'Proyecto 1',
          tareas: [],
        } as TableroHito;
      },
      async consultarEvaluacion(idHitoEquipo: number) {
        llamadas1.push(`consultarEvaluacion ${idHitoEquipo}`);
        throw new ErrorApi(404, 'NO_ENCONTRADO', 'No encontrado');
      },
    };

    const res1 = await cargarTrazabilidad(api1, 7, null);
    expect(llamadas1).toEqual(['historialEquipo 7 1000']);
    expect(res1.tablero).toBeNull();
    expect(res1.evaluacion).toBeNull();
    expect(res1.historial.map((e) => e.idLog)).toEqual([1, 2]);

    const llamadas2: string[] = [];
    const api2: ApiTrazabilidad = {
      async historialEquipo(idEquipo: number, limite: number) {
        llamadas2.push(`historialEquipo ${idEquipo} ${limite}`);
        return [entrada(1), entrada(2)];
      },
      async tableroHito(idHitoEquipo: number) {
        llamadas2.push(`tableroHito ${idHitoEquipo}`);
        return {
          idHitoEquipo,
          idHito: 1,
          nombreHito: 'Hito 1',
          fechaLimite: '2026-10-01',
          estado: 'EN_CURSO',
          editable: true,
          idEquipo: 7,
          nombreEquipo: 'Equipo 7',
          idProyecto: 1,
          nombreProyecto: 'Proyecto 1',
          tareas: [],
        } as TableroHito;
      },
      async consultarEvaluacion(idHitoEquipo: number) {
        llamadas2.push(`consultarEvaluacion ${idHitoEquipo}`);
        throw new ErrorApi(404, 'NO_ENCONTRADO', 'No encontrado');
      },
    };

    const res2 = await cargarTrazabilidad(api2, 7, 12);
    expect([...llamadas2].sort()).toEqual([
      'consultarEvaluacion 12',
      'historialEquipo 7 1000',
      'tableroHito 12',
    ]);
    expect(res2.evaluacion).toBeNull();
    expect(res2.tablero?.idHitoEquipo).toBe(12);

    const error403 = new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado');
    const apiCon403Evaluacion: ApiTrazabilidad = {
      ...api2,
      async consultarEvaluacion() {
        throw error403;
      },
    };
    await expect(
      cargarTrazabilidad(apiCon403Evaluacion, 7, 12)
    ).rejects.toBe(error403);

    const error403Historial = new ErrorApi(403, 'ACCESO_DENEGADO', 'Acceso denegado');
    const apiCon403Historial: ApiTrazabilidad = {
      ...api1,
      async historialEquipo() {
        throw error403Historial;
      },
    };
    await expect(
      cargarTrazabilidad(apiCon403Historial, 7, null)
    ).rejects.toBe(error403Historial);
  });
});
