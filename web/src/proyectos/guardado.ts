import type {
  CriterioVista,
  HitoVista,
  ProyectoVista,
  SolicitudCriterio,
  SolicitudHito,
  SolicitudProyecto,
} from '../api/tipos';
import type { Operacion } from './plan';

/** Las operaciones del API que usa el guardado (en las pruebas, una versión falsa). */
export interface ApiGuardado {
  crearProyecto(solicitud: SolicitudProyecto): Promise<ProyectoVista>;
  actualizarProyecto(idProyecto: number, solicitud: SolicitudProyecto): Promise<ProyectoVista>;
  crearHito(idProyecto: number, solicitud: SolicitudHito): Promise<HitoVista>;
  actualizarHito(idHito: number, solicitud: SolicitudHito): Promise<HitoVista>;
  crearCriterio(idHito: number, solicitud: SolicitudCriterio): Promise<CriterioVista>;
  actualizarCriterio(idHito: number, idCriterio: number, solicitud: SolicitudCriterio): Promise<CriterioVista>;
  eliminarCriterio(idHito: number, idCriterio: number): Promise<void>;
}

export interface ResultadoGuardado {
  idProyecto: number | null;
  /** clave local del hito o criterio creado → id que le dio el servidor. */
  creados: Map<string, number>;
  /** Primera operación que falló (las siguientes no se intentan). */
  fallo: { operacion: Operacion; indice: number; causa: unknown } | null;
}

function exigir(id: number | null): number {
  if (id === null) {
    throw new Error('Falta un id que debía crear un paso anterior del plan');
  }
  return id;
}

/** Hace el plan en orden, una llamada a la vez, y para en el primer error (decisión 26b-5). */
export async function ejecutarPlan(plan: Operacion[], idProyectoInicial: number | null,
                                   api: ApiGuardado): Promise<ResultadoGuardado> {
  let idProyecto = idProyectoInicial;
  const creados = new Map<string, number>();
  for (const [indice, op] of plan.entries()) {
    try {
      switch (op.tipo) {
        case 'crearProyecto':
          idProyecto = (await api.crearProyecto(op.solicitud)).idProyecto;
          break;
        case 'actualizarProyecto':
          await api.actualizarProyecto(exigir(idProyecto), op.solicitud);
          break;
        case 'crearHito':
          creados.set(op.claveHito, (await api.crearHito(exigir(idProyecto), op.solicitud)).idHito);
          break;
        case 'actualizarHito':
          await api.actualizarHito(op.idHito, op.solicitud);
          break;
        case 'eliminarCriterio':
          await api.eliminarCriterio(op.idHito, op.idCriterio);
          break;
        case 'actualizarCriterio':
          await api.actualizarCriterio(op.idHito, op.idCriterio, op.solicitud);
          break;
        case 'crearCriterio': {
          const idHito = exigir(op.idHito ?? creados.get(op.claveHito) ?? null);
          creados.set(op.claveCriterio, (await api.crearCriterio(idHito, op.solicitud)).idCriterio);
          break;
        }
      }
    } catch (causa) {
      return { idProyecto, creados, fallo: { operacion: op, indice, causa } };
    }
  }
  return { idProyecto, creados, fallo: null };
}
