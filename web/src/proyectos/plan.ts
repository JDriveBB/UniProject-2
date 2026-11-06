import type {
  CriterioVista,
  HitoVista,
  ProyectoDetalle,
  ProyectoVista,
  SolicitudCriterio,
  SolicitudHito,
  SolicitudProyecto,
} from '../api/tipos';
import type { BorradorCriterio, BorradorHito, BorradorProyecto } from './borrador';
import { centesimas } from './validacion';

/** Una llamada al API. claveHito/claveCriterio: para asignar los ids de lo creado y nombrar el paso que falló. */
export type Operacion =
  | { tipo: 'crearProyecto'; solicitud: SolicitudProyecto }
  | { tipo: 'actualizarProyecto'; solicitud: SolicitudProyecto }
  | { tipo: 'crearHito'; claveHito: string; solicitud: SolicitudHito }
  | { tipo: 'actualizarHito'; claveHito: string; idHito: number; solicitud: SolicitudHito }
  | { tipo: 'eliminarCriterio'; claveHito: string; idHito: number; idCriterio: number }
  | { tipo: 'actualizarCriterio'; claveHito: string; idHito: number; idCriterio: number; solicitud: SolicitudCriterio }
  | { tipo: 'crearCriterio'; claveHito: string; idHito: number | null; claveCriterio: string;
      solicitud: SolicitudCriterio };

export function solicitudProyecto(b: BorradorProyecto): SolicitudProyecto {
  const descripcion = b.descripcion.trim();
  return {
    nombre: b.nombre.trim(),
    descripcion: descripcion === '' ? null : descripcion,
    fechaInicio: b.fechaInicio,
    fechaFin: b.fechaFin,
    usaMonitores: b.usaMonitores,
  };
}

/** PUT reemplaza todo el hito: se reenvían el orden y la descripción que P03 no edita. */
export function solicitudHito(h: BorradorHito): SolicitudHito {
  return {
    nombreHito: h.nombre.trim(),
    descripcion: h.descripcion,
    orden: h.orden,
    fechaInicio: h.fechaInicio,
    fechaLimite: h.fechaLimite,
    diasInactividadAlerta: Number(h.diasInactividad.trim()),
    notificarDocenteInactividad: h.notificarDocente,
  };
}

export function solicitudCriterio(c: BorradorCriterio): SolicitudCriterio {
  return {
    descripcion: c.descripcion.trim(),
    pesoPorcentaje: (centesimas(c.peso) ?? 0) / 100,
    puntajeMaximo: (centesimas(c.puntaje) ?? 0) / 100,
  };
}

function proyectoCambio(s: ProyectoVista, n: SolicitudProyecto): boolean {
  return s.nombre !== n.nombre || (s.descripcion ?? null) !== n.descripcion || s.fechaInicio !== n.fechaInicio
    || s.fechaFin !== n.fechaFin || s.usaMonitores !== n.usaMonitores;
}

function hitoCambio(s: HitoVista, n: SolicitudHito): boolean {
  return s.nombreHito !== n.nombreHito || s.fechaInicio !== n.fechaInicio || s.fechaLimite !== n.fechaLimite
    || s.diasInactividadAlerta !== n.diasInactividadAlerta || s.notificarDocenteInactividad !== n.notificarDocenteInactividad;
}

function enCentesimas(valor: number): number {
  return Math.round(valor * 100);
}

function criterioCambio(s: CriterioVista, n: SolicitudCriterio): boolean {
  return s.descripcion !== n.descripcion || enCentesimas(s.pesoPorcentaje) !== enCentesimas(n.pesoPorcentaje)
    || enCentesimas(s.puntajeMaximo) !== enCentesimas(n.puntajeMaximo);
}

/** Rúbrica de un hito: borrar, bajar pesos, subir pesos y crear (cada paso deja la suma ≤ la final ≤ 100 %). */
function operacionesRubrica(h: BorradorHito, guardados: CriterioVista[]): Operacion[] {
  const idHito = h.idHito;
  const borrar: Operacion[] = [];
  const bajar: Operacion[] = [];
  const subir: Operacion[] = [];
  const crear: Operacion[] = [];
  if (idHito !== null) {
    for (const g of guardados) {
      if (!h.criterios.some((c) => c.idCriterio === g.idCriterio)) {
        borrar.push({ tipo: 'eliminarCriterio', claveHito: h.clave, idHito, idCriterio: g.idCriterio });
      }
    }
  }
  for (const c of h.criterios) {
    const solicitud = solicitudCriterio(c);
    const g = c.idCriterio === null ? undefined : guardados.find((x) => x.idCriterio === c.idCriterio);
    if (g === undefined) {
      crear.push({ tipo: 'crearCriterio', claveHito: h.clave, idHito, claveCriterio: c.clave, solicitud });
    } else if (idHito !== null && criterioCambio(g, solicitud)) {
      const op: Operacion = { tipo: 'actualizarCriterio', claveHito: h.clave, idHito, idCriterio: g.idCriterio, solicitud };
      (enCentesimas(solicitud.pesoPorcentaje) < enCentesimas(g.pesoPorcentaje) ? bajar : subir).push(op);
    }
  }
  return [...borrar, ...bajar, ...subir, ...crear];
}

/**
 * Llamadas para pasar del servidor al borrador, en un orden que el núcleo acepta (decisión 26b-5):
 * 1. Proyecto nuevo: crearlo primero.
 * 2. Hitos existentes cuyo nuevo rango cabe en las fechas GUARDADAS del proyecto, antes de actualizar el proyecto
 *    (si se acorta, primero se mueven los hitos: 409 HITOS_FUERA_DE_FECHAS); los demás y los nuevos, después
 *    (si se alarga, primero el proyecto: 400 FECHAS_FUERA_DEL_PROYECTO).
 * 3. La rúbrica de cada hito va tras su hito.
 * Sin cambios devuelve [].
 */
export function planDeGuardado(servidor: ProyectoDetalle | null, b: BorradorProyecto): Operacion[] {
  const proyecto = solicitudProyecto(b);
  const antes: Operacion[] = [];
  const despues: Operacion[] = [];
  let delProyecto: Operacion[] = [];
  if (servidor === null) {
    delProyecto = [{ tipo: 'crearProyecto', solicitud: proyecto }];
  } else if (proyectoCambio(servidor.proyecto, proyecto)) {
    delProyecto = [{ tipo: 'actualizarProyecto', solicitud: proyecto }];
  }
  for (const h of b.hitos) {
    const guardado = h.idHito === null ? undefined : servidor?.hitos.find((x) => x.idHito === h.idHito);
    const solicitud = solicitudHito(h);
    const grupo: Operacion[] = [];
    let cabe = false;
    if (guardado === undefined) {
      grupo.push({ tipo: 'crearHito', claveHito: h.clave, solicitud });
    } else if (hitoCambio(guardado, solicitud)) {
      grupo.push({ tipo: 'actualizarHito', claveHito: h.clave, idHito: guardado.idHito, solicitud });
      cabe = servidor !== null && solicitud.fechaInicio >= servidor.proyecto.fechaInicio
        && solicitud.fechaLimite <= servidor.proyecto.fechaFin;
    }
    grupo.push(...operacionesRubrica(h, guardado?.criterios ?? []));
    (cabe ? antes : despues).push(...grupo);
  }
  return [...antes, ...delProyecto, ...despues];
}
