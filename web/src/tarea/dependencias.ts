import type { ResultadoDependencias, TareaVista } from '../api/tipos';
import { CAMPOS_DEPENDENCIAS } from '../tablero/nuevaTarea';
import { mensajeErrorTarea, SIN_PERMISO_LIDER } from './vista';

export interface ApiDependencias {
  agregarDependencias(idTarea: number, prerrequisitos: number[]): Promise<ResultadoDependencias>;
  quitarDependencia(idTarea: number, idPrerrequisito: number): Promise<TareaVista>;
}

export type ResultadoGuardado =
  | { tipo: 'sinCambios' }
  | { tipo: 'guardado'; ciclicos: number[] }
  | { tipo: 'error'; texto: string; huboCambios: boolean };

export const MAXIMO_NUEVAS = 50;

export function cambiosDeDependencias(
  actuales: number[],
  elegidas: number[]
): { quitar: number[]; agregar: number[] } {
  const setActuales = new Set(actuales);
  const setElegidas = new Set(elegidas);

  const quitar = [...setActuales].filter((id) => !setElegidas.has(id)).sort((a, b) => a - b);
  const agregar = [...setElegidas].filter((id) => !setActuales.has(id)).sort((a, b) => a - b);

  return { quitar, agregar };
}

export async function guardarDependencias(
  api: ApiDependencias,
  idTarea: number,
  actuales: number[],
  elegidas: number[]
): Promise<ResultadoGuardado> {
  const { quitar, agregar } = cambiosDeDependencias(actuales, elegidas);

  if (quitar.length === 0 && agregar.length === 0) {
    return { tipo: 'sinCambios' };
  }

  if (agregar.length > MAXIMO_NUEVAS) {
    return {
      tipo: 'error',
      texto: `Máximo ${MAXIMO_NUEVAS} tareas nuevas a la vez.`,
      huboCambios: false,
    };
  }

  let huboCambios = false;

  for (const idPrerrequisito of quitar) {
    try {
      await api.quitarDependencia(idTarea, idPrerrequisito);
      huboCambios = true;
    } catch (e) {
      return {
        tipo: 'error',
        texto: `No se guardaron todas las dependencias: ${mensajeErrorTarea(e, CAMPOS_DEPENDENCIAS, SIN_PERMISO_LIDER)}`,
        huboCambios,
      };
    }
  }

  if (agregar.length > 0) {
    try {
      const res = await api.agregarDependencias(idTarea, agregar);
      return { tipo: 'guardado', ciclicos: res.ciclicos };
    } catch (e) {
      return {
        tipo: 'error',
        texto: `No se guardaron todas las dependencias: ${mensajeErrorTarea(e, CAMPOS_DEPENDENCIAS, SIN_PERMISO_LIDER)}`,
        huboCambios,
      };
    }
  }

  return { tipo: 'guardado', ciclicos: [] };
}

function formatearLista(nombres: string[]): string {
  if (nombres.length === 0) {
    return '';
  }
  if (nombres.length === 1) {
    return nombres[0] ?? '';
  }
  const ultimosDos = nombres.slice(-2);
  const previos = nombres.slice(0, -2);
  if (previos.length === 0) {
    return `${ultimosDos[0]} y ${ultimosDos[1]}`;
  }
  return `${previos.join(', ')}, ${ultimosDos[0]} y ${ultimosDos[1]}`;
}

export function textoCiclo(nombres: string[]): string {
  if (nombres.length === 1) {
    return `Dependencia circular: se excluyó ${nombres[0]} porque ya depende de esta tarea. Las demás dependencias se guardaron y la ruta crítica se recalculó.`;
  }
  const lista = formatearLista(nombres);
  return `Dependencia circular: se excluyeron ${lista} porque ya dependen de esta tarea. Las demás dependencias se guardaron y la ruta crítica se recalculó.`;
}
