import { ErrorApi } from '../api/cliente';
import type { ResultadoDependencias, SolicitudTarea, TareaVista } from '../api/tipos';
import { fechaDia } from '../formato';
import { mensajeErrorApi } from '../proyectos/errores';
import type { Campos, Mensaje } from '../proyectos/errores';

/** Formulario de P05 tal como se escribe; se convierte al enviar. idEncargado "" = sin asignar. */
export interface BorradorTarea {
  titulo: string;
  descripcion: string;
  idEncargado: string;
  horas: string;
  fechaLimite: string;
  critico: boolean;
  prerrequisitos: number[];
}

/** Decisión 26c-5: sin encargado, sin fecha (vence con el hito), sin dependencias; las horas se escriben. */
export const TAREA_VACIA: BorradorTarea = {
  titulo: '',
  descripcion: '',
  idEncargado: '',
  horas: '',
  fechaLimite: '',
  critico: false,
  prerrequisitos: [],
};

export type CampoTarea = 'titulo' | 'descripcion' | 'horas' | 'fechaLimite' | 'prerrequisitos';
export type ErroresTarea = Partial<Record<CampoTarea, string>>;

/** Orden del formulario: el foco va al primer campo con error. */
const ORDEN_CAMPOS: readonly CampoTarea[] = ['titulo', 'descripcion', 'horas', 'fechaLimite', 'prerrequisitos'];

export const CAMPOS_TAREA: Campos = [
  ['titulo', 'Título'],
  ['descripcion', 'Descripción'],
  ['idEncargado', 'Encargado'],
  ['horasEstimadas', 'Horas estimadas'],
  ['fechaLimite', 'Fecha límite'],
  ['esPrerrequisitoCritico', 'Prerrequisito crítico del hito'],
];

export const CAMPOS_DEPENDENCIAS: Campos = [['prerrequisitos', 'Bloqueada por']];

export const SOLO_LIDER = 'Solo el líder del equipo puede crear tareas.';

/** numeric(6,2) > 0: hasta 4 enteros y 2 decimales, con coma o punto. */
const HORAS = /^\d{1,4}([.,]\d{1,2})?$/;

function horasDe(texto: string): number {
  return Number(texto.trim().replace(',', '.'));
}

/** Decisión 26c-13: límites del OpenAPI y de SolicitudTarea del núcleo, y fecha dentro de las fechas del hito. */
export function validarTarea(b: BorradorTarea, hito: { fechaInicio: string; fechaLimite: string }): ErroresTarea {
  const errores: ErroresTarea = {};
  const titulo = b.titulo.trim();
  if (titulo === '') {
    errores.titulo = 'Escribe el título de la tarea.';
  } else if (titulo.length > 150) {
    errores.titulo = 'Máximo 150 caracteres.';
  }
  if (b.descripcion.trim().length > 2000) {
    errores.descripcion = 'Máximo 2000 caracteres.';
  }
  const horas = b.horas.trim();
  if (horas === '') {
    errores.horas = 'Escribe las horas estimadas.';
  } else if (!HORAS.test(horas) || horasDe(horas) <= 0) {
    errores.horas = 'Usa un número mayor que 0, con hasta 2 decimales (máximo 9999,99).';
  }
  if (b.fechaLimite !== '' && (b.fechaLimite < hito.fechaInicio || b.fechaLimite > hito.fechaLimite)) {
    errores.fechaLimite = `Debe estar entre ${fechaDia(hito.fechaInicio)} y ${fechaDia(hito.fechaLimite)} (fechas del hito).`;
  }
  if (b.prerrequisitos.length > 50) {
    errores.prerrequisitos = 'Máximo 50 tareas.';
  }
  return errores;
}

export function primerCampoConError(errores: ErroresTarea): CampoTarea | null {
  return ORDEN_CAMPOS.find((c) => errores[c] !== undefined) ?? null;
}

/** Recorta textos, convierte la coma y manda null en lo vacío. */
export function solicitudDe(b: BorradorTarea): SolicitudTarea {
  const descripcion = b.descripcion.trim();
  return {
    titulo: b.titulo.trim(),
    descripcion: descripcion === '' ? null : descripcion,
    idEncargado: b.idEncargado === '' ? null : Number(b.idEncargado),
    horasEstimadas: horasDe(b.horas),
    fechaLimite: b.fechaLimite === '' ? null : b.fechaLimite,
    esPrerrequisitoCritico: b.critico,
  };
}

/** Lo que se usa del API (en la pantalla, las funciones de api/tareas; en las pruebas, una falsa). */
export interface ApiNuevaTarea {
  crearTarea(idHitoEquipo: number, solicitud: SolicitudTarea): Promise<TareaVista>;
  agregarDependencias(idTarea: number, prerrequisitos: number[]): Promise<ResultadoDependencias>;
}

type Intento<T> = { ok: true; valor: T } | { ok: false; error: unknown };

async function intentar<T>(f: () => Promise<T>): Promise<Intento<T>> {
  try {
    return { ok: true, valor: await f() };
  } catch (error) {
    return { ok: false, error };
  }
}

function mensajeCrear(error: unknown): string {
  if (error instanceof ErrorApi && error.estado === 403) {
    return SOLO_LIDER;
  }
  return mensajeErrorApi(error, CAMPOS_TAREA);
}

/**
 * Decisión 26c-4: crea la tarea y, si hay prerrequisitos, agrega sus dependencias. Si la segunda llamada falla, la
 * tarea ya quedó creada y se avisa (no se borra ni se reintenta sola). Nunca lanza: devuelve el aviso para la pantalla.
 */
export async function crearConDependencias(api: ApiNuevaTarea, idHitoEquipo: number,
                                           b: BorradorTarea): Promise<Mensaje> {
  const creada = await intentar(() => api.crearTarea(idHitoEquipo, solicitudDe(b)));
  if (!creada.ok) {
    return { tipo: 'error', texto: mensajeCrear(creada.error) };
  }
  const nombre = `La tarea «${creada.valor.titulo}»`;
  if (b.prerrequisitos.length === 0) {
    return { tipo: 'exito', texto: `${nombre} se creó.` };
  }
  const dependencias = await intentar(() => api.agregarDependencias(creada.valor.idTarea, b.prerrequisitos));
  if (!dependencias.ok) {
    return {
      tipo: 'aviso',
      texto: `${nombre} se creó, pero no se guardaron sus dependencias: ${mensajeErrorApi(dependencias.error, CAMPOS_DEPENDENCIAS)}`,
    };
  }
  if (dependencias.valor.ciclicos.length > 0) {
    return { tipo: 'aviso', texto: `${nombre} se creó, pero algunas dependencias formarían un ciclo y no se guardaron.` };
  }
  return { tipo: 'exito', texto: `${nombre} se creó.` };
}
