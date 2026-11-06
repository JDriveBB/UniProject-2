import { ErrorApi } from '../api/cliente';
import type {
  CriterioVista,
  EquipoVista,
  EvaluacionVista,
  HitoVista,
  ProyectoDetalle,
  ProyectoVista,
  TableroHito,
} from '../api/tipos';
import { etiquetaHito } from '../tablero/ubicacion';

export interface DatosEvaluacion {
  tablero: TableroHito;
  proyecto: ProyectoVista;
  hito: HitoVista | null;
  equipo: EquipoVista | null;
  evaluacion: EvaluacionVista | null;
}

export interface ApiCargaEvaluacion {
  tableroHito(idHitoEquipo: number): Promise<TableroHito>;
  detalleProyecto(idProyecto: number): Promise<ProyectoDetalle>;
  consultarEvaluacion(idHitoEquipo: number): Promise<EvaluacionVista>;
}

/** Carga los datos necesarios para evaluar o ver el resultado de la evaluación. */
export async function cargarEvaluacion(
  api: ApiCargaEvaluacion,
  idHitoEquipo: number
): Promise<DatosEvaluacion> {
  const tablero = await api.tableroHito(idHitoEquipo);

  const [detalle, evaluacion] = await Promise.all([
    api.detalleProyecto(tablero.idProyecto),
    api.consultarEvaluacion(idHitoEquipo).catch((err: unknown) => {
      if (err instanceof ErrorApi && err.estado === 404) {
        return null;
      }
      throw err;
    }),
  ]);

  const hito = detalle.hitos.find((h) => h.idHito === tablero.idHito) ?? null;
  const equipo = detalle.equipos.find((eq) => eq.idEquipo === tablero.idEquipo) ?? null;

  return {
    tablero,
    proyecto: detalle.proyecto,
    hito,
    equipo,
    evaluacion,
  };
}

export function etiquetaDeHito(d: DatosEvaluacion): string {
  return d.hito !== null ? etiquetaHito(d.hito.orden, d.hito.nombreHito) : d.tablero.nombreHito;
}

export function criteriosDelHito(d: DatosEvaluacion): CriterioVista[] {
  if (d.hito === null) {
    return [];
  }
  return [...d.hito.criterios].sort((a, b) => a.idCriterio - b.idCriterio);
}
