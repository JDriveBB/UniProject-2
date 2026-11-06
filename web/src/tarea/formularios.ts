import type {
  SolicitudAvance,
  SolicitudReasignacion,
  SolicitudTarea,
  TipoReasignacion,
  TareaVista,
} from '../api/tipos';
import { decimalEsCo } from '../formato';
import type { Campos } from '../proyectos/errores';
import { esEnlaceWeb } from './vista';

export const CAMPOS_AVANCE: Campos = [
  ['porcentajeAvance', 'Porcentaje de avance'],
  ['observacion', 'Observación'],
];

export const CAMPOS_ENLACE: Campos = [['url', 'Enlace']];

export const CAMPOS_COMENTARIO: Campos = [['contenido', 'Comentario']];

export const CAMPOS_CORRECCION: Campos = [
  ['idEvidencia', 'Evidencia que debe rehacer'],
  ['observaciones', 'Puntos que debe corregir'],
];

export const CAMPOS_REASIGNACION: Campos = [
  ['idNuevoEncargado', 'Nuevo encargado'],
  ['tipo', 'Qué pasa al nuevo encargado'],
  ['tituloParcial', 'Título de la parte'],
  ['horasParcial', 'Horas de la parte'],
  ['motivo', 'Motivo'],
];

export const CAMPOS_APOYO: Campos = [['mensaje', '¿Qué necesitas?']];

export function validarTexto(texto: string, maximo: number, vacio: string | null): string | undefined {
  const recortado = texto.trim();
  if (recortado === '' && vacio !== null) {
    return vacio;
  }
  if (recortado.length > maximo) {
    return `Máximo ${maximo} caracteres.`;
  }
  return undefined;
}

export function textoOpcional(texto: string): string | null {
  const recortado = texto.trim();
  return recortado === '' ? null : recortado;
}

export interface BorradorAvance {
  porcentaje: string;
  observacion: string;
}

export type ErroresAvance = Partial<Record<'porcentaje' | 'observacion', string>>;

const PORCENTAJE = /^\d{1,3}$/;

export function validarAvance(b: BorradorAvance): ErroresAvance {
  const errores: ErroresAvance = {};
  const p = b.porcentaje.trim();
  if (!PORCENTAJE.test(p) || Number(p) > 100) {
    errores.porcentaje = 'Escribe un número entero de 0 a 100.';
  }
  const errObs = validarTexto(b.observacion, 500, null);
  if (errObs !== undefined) {
    errores.observacion = errObs;
  }
  return errores;
}

export function solicitudAvance(b: BorradorAvance): SolicitudAvance {
  return {
    porcentajeAvance: Number(b.porcentaje.trim()),
    observacion: textoOpcional(b.observacion),
  };
}

export const TIPOS_ARCHIVO = ['application/pdf', 'image/png', 'image/jpeg'];
export const MAXIMO_BYTES = 10 * 1024 * 1024;
export const ACEPTA_ARCHIVO = '.pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg';

export function validarArchivo(a: { size: number; type: string } | null): string | undefined {
  if (a === null) {
    return 'Elige un archivo.';
  }
  if (!TIPOS_ARCHIVO.includes(a.type)) {
    return 'Solo se aceptan archivos PDF, PNG o JPEG.';
  }
  if (a.size === 0) {
    return 'El archivo está vacío.';
  }
  if (a.size > MAXIMO_BYTES) {
    return 'El archivo pesa más de 10 MB. Súbelo a un repositorio y adjunta el enlace.';
  }
  return undefined;
}

export function validarEnlace(url: string): string | undefined {
  const recortado = url.trim();
  if (recortado === '') {
    return 'Pega el enlace.';
  }
  if (recortado.length > 2048) {
    return 'Máximo 2048 caracteres.';
  }
  if (!esEnlaceWeb(recortado)) {
    return 'Usa un enlace completo que empiece por http o https.';
  }
  return undefined;
}

export interface BorradorCorreccion {
  idEvidencia: string;
  observaciones: string;
}

export type ErroresCorreccion = Partial<Record<'idEvidencia' | 'observaciones', string>>;

export function validarCorreccion(b: BorradorCorreccion): ErroresCorreccion {
  const errores: ErroresCorreccion = {};
  if (b.idEvidencia.trim() === '') {
    errores.idEvidencia = 'Elige la evidencia que debe rehacer.';
  }
  const errObs = validarTexto(b.observaciones, 1000, 'Escribe los puntos que debe corregir.');
  if (errObs !== undefined) {
    errores.observaciones = errObs;
  }
  return errores;
}

export interface BorradorReasignacion {
  idNuevo: string;
  tipo: TipoReasignacion;
  titulo: string;
  horas: string;
  motivo: string;
}

export const REASIGNACION_VACIA: BorradorReasignacion = {
  idNuevo: '',
  tipo: 'TOTAL',
  titulo: '',
  horas: '',
  motivo: '',
};

export type CampoReasignacion = 'idNuevo' | 'titulo' | 'horas' | 'motivo';
export type ErroresReasignacion = Partial<Record<CampoReasignacion, string>>;

const ORDEN_REASIGNACION: readonly CampoReasignacion[] = ['idNuevo', 'titulo', 'horas', 'motivo'];

export function primerErrorReasignacion(e: ErroresReasignacion): CampoReasignacion | null {
  return ORDEN_REASIGNACION.find((c) => e[c] !== undefined) ?? null;
}

const REGEX_HORAS = /^\d{1,4}([.,]\d{1,2})?$/;

export function validarReasignacion(b: BorradorReasignacion, horasTarea: number): ErroresReasignacion {
  const errores: ErroresReasignacion = {};
  if (b.idNuevo.trim() === '') {
    errores.idNuevo = 'Elige el nuevo encargado.';
  }
  if (b.tipo === 'PARCIAL') {
    const errTitulo = validarTexto(b.titulo, 150, 'Escribe el título de la parte.');
    if (errTitulo !== undefined) {
      errores.titulo = errTitulo;
    }
    const hTexto = b.horas.trim();
    const hVal = Number(hTexto.replace(',', '.'));
    if (!REGEX_HORAS.test(hTexto) || hVal <= 0 || hVal >= horasTarea) {
      errores.horas = `Usa un número mayor que 0 y menor que ${decimalEsCo(horasTarea)} (las horas de la tarea).`;
    }
  }
  const errMotivo = validarTexto(b.motivo, 500, null);
  if (errMotivo !== undefined) {
    errores.motivo = errMotivo;
  }
  return errores;
}

export function solicitudReasignacion(b: BorradorReasignacion): SolicitudReasignacion {
  const esParcial = b.tipo === 'PARCIAL';
  return {
    idNuevoEncargado: Number(b.idNuevo),
    tipo: b.tipo,
    tituloParcial: esParcial ? b.titulo.trim() : null,
    horasParcial: esParcial ? Number(b.horas.trim().replace(',', '.')) : null,
    motivo: textoOpcional(b.motivo),
  };
}

export function solicitudAsignar(t: TareaVista, idEncargado: number): SolicitudTarea {
  return {
    titulo: t.titulo,
    descripcion: t.descripcion,
    horasEstimadas: t.horasEstimadas,
    fechaLimite: t.fechaLimite,
    esPrerrequisitoCritico: t.esPrerrequisitoCritico,
    idEncargado,
  };
}
