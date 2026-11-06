import { decimalEsCo } from '../formato';
import { sinAlerta, type BorradorCriterio, type BorradorHito, type BorradorProyecto } from './borrador';

/** Límites del contrato (SolicitudProyecto, SolicitudHito, SolicitudCriterio, SolicitudEquipo del núcleo). */
export const MAXIMO_NOMBRE_PROYECTO = 150;
export const MAXIMO_DESCRIPCION = 2000;
export const MAXIMO_NOMBRE_HITO = 150;
export const MAXIMO_CRITERIO = 500;
export const MAXIMO_NOMBRE_EQUIPO = 100;
export const MINIMO_DIAS = 1;
export const MAXIMO_DIAS = 60;
/** 100 % en centésimas. */
export const CIEN_POR_CIENTO = 10000;

const FECHA = /^\d{4}-\d{2}-\d{2}$/;
/** numeric(5,2): hasta 3 enteros y 2 decimales. */
const DECIMAL = /^\d{1,3}(\.\d{1,2})?$/;
const ENTERO = /^\d{1,2}$/;

export type ErroresProyecto = Partial<Record<'nombre' | 'descripcion' | 'fechaInicio' | 'fechaFin', string>>;
export type ErroresHito = Partial<Record<'nombre' | 'fechaInicio' | 'fechaLimite' | 'diasInactividad', string>>;
export type ErroresCriterio = Partial<Record<'descripcion' | 'peso' | 'puntaje', string>>;

/** Errores del borrador: por clave de hito, por clave de criterio y, por clave de hito, la suma de la rúbrica. */
export interface ErroresBorrador {
  proyecto: ErroresProyecto;
  hitos: Record<string, ErroresHito>;
  criterios: Record<string, ErroresCriterio>;
  rubricas: Record<string, string>;
}

export const SIN_ERRORES: ErroresBorrador = { proyecto: {}, hitos: {}, criterios: {}, rubricas: {} };

function vacio(objeto: object): boolean {
  return Object.keys(objeto).length === 0;
}

/** "33.5" → 3350; null si no es un decimal de hasta 3 enteros y 2 decimales. Sumar centésimas evita la coma flotante. */
export function centesimas(texto: string): number | null {
  const t = texto.trim();
  return DECIMAL.test(t) ? Math.round(Number(t) * 100) : null;
}

export function validarDatosProyecto(b: BorradorProyecto): ErroresProyecto {
  const e: ErroresProyecto = {};
  const nombre = b.nombre.trim();
  if (nombre === '') {
    e.nombre = 'Escribe el nombre del proyecto';
  } else if (nombre.length > MAXIMO_NOMBRE_PROYECTO) {
    e.nombre = 'El nombre no puede pasar de 150 caracteres';
  }
  if (b.descripcion.trim().length > MAXIMO_DESCRIPCION) {
    e.descripcion = 'La descripción no puede pasar de 2000 caracteres';
  }
  if (!FECHA.test(b.fechaInicio)) {
    e.fechaInicio = 'Elige la fecha de inicio';
  }
  if (!FECHA.test(b.fechaFin)) {
    e.fechaFin = 'Elige la fecha de fin';
  } else if (FECHA.test(b.fechaInicio) && b.fechaFin < b.fechaInicio) {
    e.fechaFin = 'La fecha de fin no puede ser anterior a la de inicio';
  }
  return e;
}

/** Fechas "aaaa-mm-dd": se comparan como texto. El hito va dentro de las fechas del proyecto (FECHAS_FUERA_DEL_PROYECTO). */
export function validarHito(h: BorradorHito, proyecto: Pick<BorradorProyecto, 'fechaInicio' | 'fechaFin'>): ErroresHito {
  const e: ErroresHito = {};
  const nombre = h.nombre.trim();
  if (nombre === '') {
    e.nombre = 'Escribe el nombre del hito';
  } else if (nombre.length > MAXIMO_NOMBRE_HITO) {
    e.nombre = 'El nombre no puede pasar de 150 caracteres';
  }
  const conProyecto = FECHA.test(proyecto.fechaInicio) && FECHA.test(proyecto.fechaFin);
  if (!FECHA.test(h.fechaInicio)) {
    e.fechaInicio = 'Elige la fecha de inicio';
  } else if (conProyecto && (h.fechaInicio < proyecto.fechaInicio || h.fechaInicio > proyecto.fechaFin)) {
    e.fechaInicio = 'Debe estar dentro de las fechas del proyecto';
  }
  if (!FECHA.test(h.fechaLimite)) {
    e.fechaLimite = 'Elige la fecha límite';
  } else if (FECHA.test(h.fechaInicio) && h.fechaLimite < h.fechaInicio) {
    e.fechaLimite = 'No puede ser anterior a la fecha de inicio';
  } else if (conProyecto && (h.fechaLimite < proyecto.fechaInicio || h.fechaLimite > proyecto.fechaFin)) {
    e.fechaLimite = 'Debe estar dentro de las fechas del proyecto';
  }
  if (!sinAlerta(h.diasInactividad)) {
    const dias = h.diasInactividad.trim();
    if (!ENTERO.test(dias) || Number(dias) < MINIMO_DIAS || Number(dias) > MAXIMO_DIAS) {
      e.diasInactividad = 'Escribe un número entero de 1 a 60';
    }
  }
  return e;
}

export function validarCriterio(c: BorradorCriterio): ErroresCriterio {
  const e: ErroresCriterio = {};
  const descripcion = c.descripcion.trim();
  if (descripcion === '') {
    e.descripcion = 'Escribe el criterio';
  } else if (descripcion.length > MAXIMO_CRITERIO) {
    e.descripcion = 'El criterio no puede pasar de 500 caracteres';
  }
  const peso = centesimas(c.peso);
  if (peso === null || peso <= 0 || peso > CIEN_POR_CIENTO) {
    e.peso = 'Mayor que 0 y hasta 100, con 2 decimales como máximo';
  }
  const puntaje = centesimas(c.puntaje);
  if (puntaje === null || puntaje <= 0) {
    e.puntaje = 'Mayor que 0 y hasta 999,99, con 2 decimales como máximo';
  }
  return e;
}

/** Suma de pesos en centésimas; los pesos que no son números no cuentan. */
export function sumaPesos(criterios: BorradorCriterio[]): number {
  return criterios.reduce((total, c) => total + (centesimas(c.peso) ?? 0), 0);
}

export type EstadoSuma = 'completa' | 'incompleta' | 'excedida';

/** completa = exactamente 100 % (insignia de éxito); lo demás, insignia de aviso (decisión 26b-9). */
export function estadoSuma(suma: number): EstadoSuma {
  if (suma === CIEN_POR_CIENTO) {
    return 'completa';
  }
  return suma > CIEN_POR_CIENTO ? 'excedida' : 'incompleta';
}

/** "Suma de pesos: 100 %" (Figma P03). */
export function textoSumaPesos(suma: number): string {
  return `Suma de pesos: ${decimalEsCo(suma / 100)} %`;
}

/** Todo el borrador. Una suma por encima de 100 % no se envía (el núcleo la rechaza: PESOS_EXCEDEN_100). */
export function validarBorrador(b: BorradorProyecto): ErroresBorrador {
  const resultado: ErroresBorrador = { proyecto: validarDatosProyecto(b), hitos: {}, criterios: {}, rubricas: {} };
  for (const h of b.hitos) {
    const errorHito = validarHito(h, b);
    if (!vacio(errorHito)) {
      resultado.hitos[h.clave] = errorHito;
    }
    for (const c of h.criterios) {
      const errorCriterio = validarCriterio(c);
      if (!vacio(errorCriterio)) {
        resultado.criterios[c.clave] = errorCriterio;
      }
    }
    const suma = sumaPesos(h.criterios);
    if (suma > CIEN_POR_CIENTO) {
      resultado.rubricas[h.clave] = `Los pesos suman ${decimalEsCo(suma / 100)} %; el máximo es 100 %`;
    }
  }
  return resultado;
}

export function borradorValido(e: ErroresBorrador): boolean {
  return vacio(e.proyecto) && vacio(e.hitos) && vacio(e.criterios) && vacio(e.rubricas);
}

export function hitoTieneErrores(h: BorradorHito, e: ErroresBorrador): boolean {
  return h.clave in e.hitos || h.clave in e.rubricas || h.criterios.some((c) => c.clave in e.criterios);
}

/** Para seleccionar el hito con errores al intentar guardar. */
export function primerHitoConErrores(b: BorradorProyecto, e: ErroresBorrador): string | null {
  return b.hitos.find((h) => hitoTieneErrores(h, e))?.clave ?? null;
}

export function validarNombreEquipo(nombre: string): string | null {
  const n = nombre.trim();
  if (n === '') {
    return 'Escribe el nombre del equipo';
  }
  return n.length > MAXIMO_NOMBRE_EQUIPO ? 'El nombre no puede pasar de 100 caracteres' : null;
}
