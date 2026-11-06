import { ErrorApi } from '../api/cliente';
import type { EntradaHistorial, EvaluacionVista, TableroHito } from '../api/tipos';

export const LIMITE_HISTORIAL = 1000;

export interface DatosTrazabilidad {
  historial: EntradaHistorial[];
  tablero: TableroHito | null;
  evaluacion: EvaluacionVista | null;
}

export interface ApiTrazabilidad {
  historialEquipo(idEquipo: number, limite: number): Promise<EntradaHistorial[]>;
  tableroHito(idHitoEquipo: number): Promise<TableroHito>;
  consultarEvaluacion(idHitoEquipo: number): Promise<EvaluacionVista>;
}

export async function cargarTrazabilidad(
  api: ApiTrazabilidad,
  idEquipo: number,
  idHitoEquipo: number | null
): Promise<DatosTrazabilidad> {
  if (idHitoEquipo === null) {
    const historial = await api.historialEquipo(idEquipo, LIMITE_HISTORIAL);
    return {
      historial,
      tablero: null,
      evaluacion: null,
    };
  }

  const [historial, tablero, evaluacion] = await Promise.all([
    api.historialEquipo(idEquipo, LIMITE_HISTORIAL),
    api.tableroHito(idHitoEquipo),
    api.consultarEvaluacion(idHitoEquipo).catch((e: unknown): EvaluacionVista | null => {
      if (e instanceof ErrorApi && e.estado === 404) {
        return null;
      }
      throw e;
    }),
  ]);

  return {
    historial,
    tablero,
    evaluacion,
  };
}
