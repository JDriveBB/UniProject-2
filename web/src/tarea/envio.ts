import { ErrorApi, mensajeDeError } from '../api/cliente';
import type { EnvioVista, TareaVista } from '../api/tipos';
import { nombreConCodigo } from '../tablero/marcas';

export interface PendienteEnvio {
  clave: string;
  nombre: string;
  porcentaje: number | null;
}

export type ResultadoEnvio =
  | { tipo: 'enviado'; envio: EnvioVista }
  | { tipo: 'rechazado'; pendientes: PendienteEnvio[] }
  | { tipo: 'error'; texto: string };

export interface ApiEnvio {
  enviarHito(idHitoEquipo: number): Promise<EnvioVista>;
}

export const SOLO_INTEGRANTES = 'Solo los integrantes del equipo pueden enviar el hito.';

export function pendientesDelEnvio(
  titulos: string[],
  tareas: TareaVista[],
  codigos: Map<number, string>
): PendienteEnvio[] {
  const tareasOrdenadas = [...tareas].sort((a, b) => a.idTarea - b.idTarea);
  const usadas = new Set<number>();

  return titulos.map((titulo, i) => {
    const t = tareasOrdenadas.find(
      (cand) =>
        !usadas.has(cand.idTarea) &&
        cand.titulo === titulo &&
        cand.esPrerrequisitoCritico &&
        cand.estado !== 'COMPLETADA'
    );

    if (t !== undefined) {
      usadas.add(t.idTarea);
      return {
        clave: `tarea-${t.idTarea}`,
        nombre: nombreConCodigo(codigos, t),
        porcentaje: t.porcentajeAvance,
      };
    }

    return {
      clave: `titulo-${i}`,
      nombre: titulo,
      porcentaje: null,
    };
  });
}

export async function enviarConResultado(
  api: ApiEnvio,
  idHitoEquipo: number,
  tareas: TareaVista[],
  codigos: Map<number, string>
): Promise<ResultadoEnvio> {
  try {
    const envio = await api.enviarHito(idHitoEquipo);
    return { tipo: 'enviado', envio };
  } catch (error) {
    if (error instanceof ErrorApi) {
      if (error.estado === 409 && error.codigo === 'PRERREQUISITOS_INCOMPLETOS') {
        return {
          tipo: 'rechazado',
          pendientes: pendientesDelEnvio(error.detalles, tareas, codigos),
        };
      }
      if (error.estado === 403) {
        return { tipo: 'error', texto: SOLO_INTEGRANTES };
      }
      return { tipo: 'error', texto: mensajeDeError(error) };
    }
    return { tipo: 'error', texto: mensajeDeError(error) };
  }
}
