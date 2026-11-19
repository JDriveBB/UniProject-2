import type { EstadoHitoEquipo, EstadoTarea, NivelRiesgo, TareaVista } from '../api/tipos';
import type { TonoInsignia } from '../componentes/Insignia';

/** Texto y tono de una insignia. */
export interface Marca {
  texto: string;
  tono: TonoInsignia;
}

const ESTADO_TAREA: Record<EstadoTarea, Marca> = {
  PENDIENTE: { texto: 'Pendiente', tono: 'neutro' },
  EN_PROGRESO: { texto: 'En progreso', tono: 'info' },
  COMPLETADA: { texto: 'Completada', tono: 'exito' },
};

/** EN_CURSO = «En progreso» (Figma P04). ENVIADO = «En Revisión» morado (Figma P08b, 26d-11); EVALUADO sin Figma. */
const ESTADO_HITO: Record<EstadoHitoEquipo, Marca> = {
  EN_CURSO: { texto: 'En progreso', tono: 'info' },
  ENVIADO: { texto: 'En Revisión', tono: 'critico' },
  EVALUADO: { texto: 'Evaluado', tono: 'exito' },
};

export const BLOQUEADA: Marca = { texto: 'Bloqueada por dependencia', tono: 'neutro' };
export const PRERREQUISITO: Marca = { texto: 'Prerrequisito crítico', tono: 'critico' };
export const RIESGO: Record<NivelRiesgo, Marca> = {
  ALTO: { texto: 'Riesgo alto', tono: 'peligro' },
  MEDIO: { texto: 'Riesgo medio', tono: 'aviso' },
  BAJO: { texto: 'Riesgo bajo', tono: 'exito' },
};

/** Elemento de la leyenda plegable del tablero. */
export interface ElementoLeyenda {
  marca: Marca | null;
  titulo: string;
  explicacion: string;
}

/** Leyenda del tablero: franja de ruta crítica y las marcas posibles en filas. */
export const LEYENDA: readonly ElementoLeyenda[] = [
  {
    marca: null,
    titulo: 'Franja violeta · Ruta crítica',
    explicacion: 'Si esta tarea se atrasa, se atrasa la entrega del hito.',
  },
  {
    marca: BLOQUEADA,
    titulo: BLOQUEADA.texto,
    explicacion: 'Espera a que termine otra tarea antes de poder avanzar.',
  },
  {
    marca: RIESGO.ALTO,
    titulo: RIESGO.ALTO.texto,
    explicacion: 'Con el trabajo que falta no alcanza a terminar antes de su fecha límite (4 h de trabajo por día).',
  },
  {
    marca: RIESGO.MEDIO,
    titulo: RIESGO.MEDIO.texto,
    explicacion: 'Alcanza, pero con poco margen.',
  },
];

export function estadoTarea(estado: EstadoTarea): Marca {
  return ESTADO_TAREA[estado];
}

export function estadoHito(estado: EstadoHitoEquipo): Marca {
  return ESTADO_HITO[estado];
}

/** Marca única de una fila en tablas: prioridad Bloqueada > Riesgo alto > Riesgo medio (null si completada o riesgo bajo). */
export function marcaPrincipal(t: Pick<TareaVista, 'estado' | 'bloqueada' | 'nivelRiesgo'>): Marca | null {
  if (t.estado === 'COMPLETADA') {
    return null;
  }
  if (t.bloqueada) {
    return BLOQUEADA;
  }
  if (t.nivelRiesgo === 'ALTO' || t.nivelRiesgo === 'MEDIO') {
    return RIESGO[t.nivelRiesgo];
  }
  return null;
}

/** Decisión 26c-9: «T1», «T2»… por idTarea dentro del hito. Solo para mostrar; no es un id del sistema. */
export function codigosDeTareas(tareas: Pick<TareaVista, 'idTarea'>[]): Map<number, string> {
  const ids = tareas.map((t) => t.idTarea).sort((a, b) => a - b);
  return new Map(ids.map((id, i) => [id, `T${i + 1}`] as const));
}

/** «T2 · Diagramas de secuencia» (chips de P05). */
export function nombreConCodigo(codigos: Map<number, string>, t: Pick<TareaVista, 'idTarea' | 'titulo'>): string {
  return `${codigos.get(t.idTarea) ?? '?'} · ${t.titulo}`;
}

/**
 * D-12 (antes 26c-8, promedio simple): avance ponderado por horas estimadas, redondeado, igual que el panel
 * (avanceHitoActual). 0 sin tareas o sin horas. Una tarea sin horasEstimadas pesa 1 h: solo pasa con datos que no
 * traen las horas (p. ej. el tipo de reportes/vista.ts#resumenHito), donde queda el promedio simple.
 */
export function avanceDelHito(
  tareas: (Pick<TareaVista, 'porcentajeAvance'> & Partial<Pick<TareaVista, 'horasEstimadas'>>)[],
): number {
  let totalHoras = 0;
  let sumaPonderada = 0;
  for (const t of tareas) {
    const horas = t.horasEstimadas ?? 1;
    totalHoras += horas;
    sumaPonderada += t.porcentajeAvance * horas;
  }
  if (totalHoras <= 0) {
    return 0;
  }
  return Math.round(sumaPonderada / totalHoras);
}
