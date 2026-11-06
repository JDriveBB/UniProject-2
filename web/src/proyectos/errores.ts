import { ErrorApi, mensajeDeError } from '../api/cliente';
import type { BorradorProyecto } from './borrador';
import type { Operacion } from './plan';

/** Aviso en una Alerta de la pantalla. */
export interface Mensaje {
  tipo: 'exito' | 'aviso' | 'error';
  texto: string;
}

/** Campo del contrato → etiqueta en pantalla, en el orden del formulario. */
export type Campos = ReadonlyArray<readonly [string, string]>;

export const CAMPOS_PROYECTO: Campos = [
  ['nombre', 'Nombre del proyecto'],
  ['descripcion', 'Descripción'],
  ['fechaInicio', 'Fecha de inicio'],
  ['fechaFin', 'Fecha de fin'],
  ['usaMonitores', 'Monitores'],
];

export const CAMPOS_HITO: Campos = [
  ['nombreHito', 'Nombre del hito'],
  ['descripcion', 'Descripción del hito'],
  ['orden', 'Orden'],
  ['fechaInicio', 'Fecha de inicio'],
  ['fechaLimite', 'Fecha límite'],
  ['diasInactividadAlerta', 'Alerta de inactividad'],
  ['notificarDocenteInactividad', 'Notificar también al docente'],
];

export const CAMPOS_CRITERIO: Campos = [
  ['descripcion', 'Criterio'],
  ['pesoPorcentaje', 'Peso (%)'],
  ['puntajeMaximo', 'Puntaje máximo'],
];

export const CAMPOS_EQUIPO: Campos = [['nombreEquipo', 'Nombre del equipo']];

export const CAMPOS_CORREO: Campos = [
  ['email', 'Correo'],
  ['emails', 'Correo'],
];

export const SIN_PERMISO_EDITAR = 'Solo el docente del proyecto puede cambiarlo';

/**
 * Error de la API al cambiar algo del proyecto (decisión 26b-8). 400 VALIDACION nombra los campos (el texto de
 * Hibernate Validator puede venir en otro idioma); 403 ACCESO_DENEGADO dice quién puede; 400, 404 y 409 con código
 * propio traen un mensaje del núcleo en español y se muestran tal cual; lo demás (401, sin conexión, 5xx) usa
 * mensajeDeError.
 */
export function mensajeErrorApi(error: unknown, campos: Campos): string {
  if (error instanceof ErrorApi) {
    if (error.estado === 400 && error.codigo === 'VALIDACION') {
      const detalles = error.detalles;
      const etiquetas = [...new Set(campos
        .filter(([campo]) => detalles.some((d) => d.startsWith(campo + ':') || d.startsWith(campo + '[')))
        .map(([, etiqueta]) => etiqueta))];
      return etiquetas.length > 0 ? 'Revisa estos campos: ' + etiquetas.join(', ') : error.message;
    }
    if (error.estado === 403 && error.codigo === 'ACCESO_DENEGADO') {
      return SIN_PERMISO_EDITAR;
    }
    if (error.estado === 400 || error.estado === 404 || error.estado === 409) {
      return error.message;
    }
  }
  return mensajeDeError(error);
}

/** Al abrir un proyecto. El núcleo responde 403 también si no existe (decisión 15-2). */
export function mensajeErrorCarga(error: unknown): string {
  if (error instanceof ErrorApi && error.estado === 403) {
    return 'No tienes acceso a este proyecto o no existe.';
  }
  if (error instanceof ErrorApi && error.estado === 404) {
    return 'Este proyecto no existe.';
  }
  return mensajeDeError(error);
}

/** "Error al guardar el hito «Diseño»: …", y si había pasos antes, que esos sí quedaron guardados (26b-5). */
export function textoErrorGuardado(op: Operacion, causa: unknown, b: BorradorProyecto, indice: number): string {
  const nombreHito = (clave: string) => {
    const h = b.hitos.find((x) => x.clave === clave);
    return h === undefined || h.nombre.trim() === '' ? 'sin nombre' : h.nombre.trim();
  };
  let que: string;
  let campos: Campos;
  switch (op.tipo) {
    case 'crearProyecto':
    case 'actualizarProyecto':
      que = 'los datos del proyecto';
      campos = CAMPOS_PROYECTO;
      break;
    case 'crearHito':
    case 'actualizarHito':
      que = `el hito «${nombreHito(op.claveHito)}»`;
      campos = CAMPOS_HITO;
      break;
    default:
      que = `la rúbrica del hito «${nombreHito(op.claveHito)}»`;
      campos = CAMPOS_CRITERIO;
  }
  const texto = `Error al guardar ${que}: ${mensajeErrorApi(causa, campos)}`;
  return indice > 0 ? texto + '. Los cambios anteriores a ese paso sí quedaron guardados.' : texto;
}
