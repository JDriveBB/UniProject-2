import { ErrorApi } from '../api/cliente';
import type { EvaluacionVista } from '../api/tipos';
import { CAMPOS_NOTA_FINAL, mensajeErrorEvaluacion } from './vista';

export interface FilaConfirmar {
  idEstudiante: number;
  nombre: string;
  notaFinal: number;
  fechaConfirmacion?: string | null;
}

export interface ApiConfirmacion {
  confirmarNota: (
    idEvaluacion: number,
    idEstudiante: number,
    notaFinal: number
  ) => Promise<EvaluacionVista>;
}

export interface FalloConfirmacion {
  nombre: string;
  motivo: string;
}

export interface ResultadoConfirmarTodas {
  evaluacion: EvaluacionVista | null;
  confirmadas: number;
  fallidas: FalloConfirmacion[];
  reemplazada: boolean;
}

/**
 * Confirma en serie las notas individuales pendientes (de a una y en orden).
 * Un fallo común no detiene a las demás; 409 EVALUACION_REEMPLAZADA detiene el resto y marca reemplazada.
 */
export async function confirmarTodas(
  api: ApiConfirmacion,
  idEvaluacion: number,
  filas: readonly FilaConfirmar[]
): Promise<ResultadoConfirmarTodas> {
  let ultimaEvaluacion: EvaluacionVista | null = null;
  let confirmadas = 0;
  const fallidas: FalloConfirmacion[] = [];
  let reemplazada = false;

  for (const fila of filas) {
    if (fila.fechaConfirmacion !== undefined && fila.fechaConfirmacion !== null) {
      continue;
    }

    try {
      const ev = await api.confirmarNota(idEvaluacion, fila.idEstudiante, fila.notaFinal);
      ultimaEvaluacion = ev;
      confirmadas++;
    } catch (err: unknown) {
      if (
        (err instanceof ErrorApi && (err.estado === 409 || err.codigo === 'EVALUACION_REEMPLAZADA')) ||
        (typeof err === 'object' &&
          err !== null &&
          'codigo' in err &&
          (err as { codigo: string }).codigo === 'EVALUACION_REEMPLAZADA')
      ) {
        reemplazada = true;
        const motivo = err instanceof Error ? err.message : String(err);
        fallidas.push({ nombre: fila.nombre, motivo });
        break;
      }

      const motivo =
        err instanceof ErrorApi
          ? mensajeErrorEvaluacion(err, CAMPOS_NOTA_FINAL)
          : err instanceof Error
            ? err.message
            : String(err);

      fallidas.push({ nombre: fila.nombre, motivo });
    }
  }

  return {
    evaluacion: ultimaEvaluacion,
    confirmadas,
    fallidas,
    reemplazada,
  };
}
