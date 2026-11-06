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

const BLOQUEADA: Marca = { texto: 'Bloqueada por dependencia', tono: 'neutro' };
const RUTA_CRITICA: Marca = { texto: 'Ruta crítica', tono: 'info' };
const PRERREQUISITO: Marca = { texto: 'Prerrequisito crítico', tono: 'critico' };
const RIESGO: Record<NivelRiesgo, Marca> = {
  ALTO: { texto: 'Riesgo alto', tono: 'peligro' },
  MEDIO: { texto: 'Riesgo medio', tono: 'aviso' },
  BAJO: { texto: 'Riesgo bajo', tono: 'exito' },
};

/** Leyenda «Marcas:» de P04, en el orden de Figma. */
export const LEYENDA: readonly Marca[] = [RUTA_CRITICA, PRERREQUISITO, BLOQUEADA, RIESGO.ALTO, RIESGO.MEDIO, RIESGO.BAJO];

export function estadoTarea(estado: EstadoTarea): Marca {
  return ESTADO_TAREA[estado];
}

export function estadoHito(estado: EstadoHitoEquipo): Marca {
  return ESTADO_HITO[estado];
}

/** Marcas de una fila, en el orden de Figma: bloqueada, ruta crítica, prerrequisito crítico, riesgo. */
export function marcasDe(t: Pick<TareaVista, 'bloqueada' | 'enRutaCritica' | 'esPrerrequisitoCritico' | 'nivelRiesgo'>): Marca[] {
  const marcas: Marca[] = [];
  if (t.bloqueada) {
    marcas.push(BLOQUEADA);
  }
  if (t.enRutaCritica) {
    marcas.push(RUTA_CRITICA);
  }
  if (t.esPrerrequisitoCritico) {
    marcas.push(PRERREQUISITO);
  }
  if (t.nivelRiesgo !== null) {
    marcas.push(RIESGO[t.nivelRiesgo]);
  }
  return marcas;
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

/** Decisión 26c-8: promedio simple del avance, redondeado (Figma: 100, 60, 80, 0, 0 → 48 %). 0 sin tareas. */
export function avanceDelHito(tareas: Pick<TareaVista, 'porcentajeAvance'>[]): number {
  if (tareas.length === 0) {
    return 0;
  }
  return Math.round(tareas.reduce((suma, t) => suma + t.porcentajeAvance, 0) / tareas.length);
}
