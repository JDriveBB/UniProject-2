import type { HitoVista, ProyectoDetalle } from '../api/tipos';

/** Valor con el que empieza un hito nuevo en P03 (Figma muestra 3). El núcleo ya no tiene valor por defecto: vacío = sin alerta (bloque 30). */
export const DIAS_INACTIVIDAD_POR_DEFECTO = 3;

/** Criterio en edición. clave: "c<id>" si viene del servidor, "n<k>" si es nuevo. Números como texto del campo. */
export interface BorradorCriterio {
  clave: string;
  idCriterio: number | null;
  descripcion: string;
  peso: string;
  puntaje: string;
}

/** Hito en edición. clave: "h<id>" o "n<k>". descripcion no se edita en P03: se reenvía tal cual (PUT reemplaza). */
export interface BorradorHito {
  clave: string;
  idHito: number | null;
  orden: number;
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaLimite: string;
  diasInactividad: string;
  notificarDocente: boolean;
  criterios: BorradorCriterio[];
}

/** Lo que edita "Guardar proyecto" (decisión 26b-5). Los equipos no van aquí: se guardan al momento (26b-7). */
export interface BorradorProyecto {
  nombre: string;
  descripcion: string;
  fechaInicio: string;
  fechaFin: string;
  usaMonitores: boolean;
  equiposMixtos: boolean;
  hitos: BorradorHito[];
}

export type CambiosHito = Partial<Pick<BorradorHito, 'nombre' | 'fechaInicio' | 'fechaLimite' | 'diasInactividad'
  | 'notificarDocente'>>;
export type CambiosCriterio = Partial<Pick<BorradorCriterio, 'descripcion' | 'peso' | 'puntaje'>>;

export const BORRADOR_VACIO: BorradorProyecto = {
  nombre: '',
  descripcion: '',
  fechaInicio: '',
  fechaFin: '',
  usaMonitores: false,
  equiposMixtos: false,
  hitos: [],
};

export function claveDeHito(idHito: number): string {
  return 'h' + idHito;
}

function borradorDeHito(h: HitoVista): BorradorHito {
  const alerta = h.diasInactividadAlerta;
  return {
    clave: claveDeHito(h.idHito),
    idHito: h.idHito,
    orden: h.orden,
    nombre: h.nombreHito,
    descripcion: h.descripcion,
    fechaInicio: h.fechaInicio,
    fechaLimite: h.fechaLimite,
    diasInactividad: alerta === null ? '' : String(alerta),
    notificarDocente: h.notificarDocenteInactividad,
    criterios: h.criterios.map((c) => ({
      clave: 'c' + c.idCriterio,
      idCriterio: c.idCriterio,
      descripcion: c.descripcion,
      peso: String(c.pesoPorcentaje),
      puntaje: String(c.puntajeMaximo),
    })),
  };
}

/** Decisión 30f-1: campo vacío = el hito no tiene alerta de inactividad. */
export function sinAlerta(diasInactividad: string): boolean {
  return diasInactividad.trim() === '';
}

export function borradorDe(detalle: ProyectoDetalle): BorradorProyecto {
  return {
    nombre: detalle.proyecto.nombre,
    descripcion: detalle.proyecto.descripcion ?? '',
    fechaInicio: detalle.proyecto.fechaInicio,
    fechaFin: detalle.proyecto.fechaFin,
    usaMonitores: detalle.proyecto.usaMonitores,
    equiposMixtos: detalle.proyecto.equiposMixtos,
    hitos: detalle.hitos.map(borradorDeHito),
  };
}

/** "+ Agregar hito": siguiente orden, sin fechas y con 3 días de alerta (el docente puede vaciarlo). */
export function nuevoHito(b: BorradorProyecto, clave: string): BorradorHito {
  const orden = b.hitos.reduce((maximo, h) => Math.max(maximo, h.orden), 0) + 1;
  return {
    clave,
    idHito: null,
    orden,
    nombre: '',
    descripcion: null,
    fechaInicio: '',
    fechaLimite: '',
    diasInactividad: String(DIAS_INACTIVIDAD_POR_DEFECTO),
    notificarDocente: false,
    criterios: [],
  };
}

export function nuevoCriterio(clave: string): BorradorCriterio {
  return { clave, idCriterio: null, descripcion: '', peso: '', puntaje: '' };
}

function conHitoTransformado(b: BorradorProyecto, claveHito: string,
                             f: (h: BorradorHito) => BorradorHito): BorradorProyecto {
  return { ...b, hitos: b.hitos.map((h) => (h.clave === claveHito ? f(h) : h)) };
}

export function conHito(b: BorradorProyecto, claveHito: string, cambios: CambiosHito): BorradorProyecto {
  return conHitoTransformado(b, claveHito, (h) => ({ ...h, ...cambios }));
}

export function conCriterio(b: BorradorProyecto, claveHito: string, claveCriterio: string,
                            cambios: CambiosCriterio): BorradorProyecto {
  return conHitoTransformado(b, claveHito, (h) => ({
    ...h,
    criterios: h.criterios.map((c) => (c.clave === claveCriterio ? { ...c, ...cambios } : c)),
  }));
}

export function conCriterioNuevo(b: BorradorProyecto, claveHito: string, clave: string): BorradorProyecto {
  return conHitoTransformado(b, claveHito, (h) => ({ ...h, criterios: [...h.criterios, nuevoCriterio(clave)] }));
}

export function sinCriterio(b: BorradorProyecto, claveHito: string, claveCriterio: string): BorradorProyecto {
  return conHitoTransformado(b, claveHito, (h) => ({
    ...h,
    criterios: h.criterios.filter((c) => c.clave !== claveCriterio),
  }));
}

/** Tras un guardado que falló a medias: los hitos y criterios ya creados reciben su id (no se crean dos veces). */
export function asignarIds(b: BorradorProyecto, creados: ReadonlyMap<string, number>): BorradorProyecto {
  return {
    ...b,
    hitos: b.hitos.map((h) => ({
      ...h,
      idHito: h.idHito ?? creados.get(h.clave) ?? null,
      criterios: h.criterios.map((c) => ({ ...c, idCriterio: c.idCriterio ?? creados.get(c.clave) ?? null })),
    })),
  };
}

/** Mantiene el hito elegido si sigue existiendo; si no, el primero (o ninguno). */
export function claveSeleccionada(actual: string | null, b: BorradorProyecto): string | null {
  if (actual !== null && b.hitos.some((h) => h.clave === actual)) {
    return actual;
  }
  return b.hitos.length > 0 ? b.hitos[0].clave : null;
}
