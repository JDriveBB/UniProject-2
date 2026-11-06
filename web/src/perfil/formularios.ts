import { ErrorApi } from '../api/cliente';
import { errorDeContrasena, MAXIMO_NOMBRE } from '../cuentas/validacion';

export const ERROR_NOMBRE_VACIO = 'Escribe tu nombre completo';
export const ERROR_NOMBRE_LARGO = 'El nombre no puede pasar de 120 caracteres';
export const ERROR_ACTUAL_VACIA = 'Escribe tu contraseña actual';
export const ERROR_IGUAL_A_ACTUAL = 'La contraseña nueva debe ser distinta de la actual';
export const ERROR_NO_COINCIDEN = 'Las contraseñas no coinciden';
export const ERROR_CONTRASENA_VACIA = 'Escribe tu contraseña';
export const ERROR_ENTIENDO = 'Marca la casilla para confirmar';

export function validarNombre(texto: string): string | undefined {
  const limpio = texto.trim();
  if (limpio === '') {
    return ERROR_NOMBRE_VACIO;
  }
  if (limpio.length > MAXIMO_NOMBRE) {
    return ERROR_NOMBRE_LARGO;
  }
  return undefined;
}

export interface BorradorContrasena {
  actual: string;
  nueva: string;
  confirmacion: string;
}

export const CONTRASENA_VACIA: BorradorContrasena = {
  actual: '',
  nueva: '',
  confirmacion: '',
};

export type CampoContrasena = keyof BorradorContrasena;
export type ErroresContrasena = Partial<Record<CampoContrasena, string>>;

export function validarCambioContrasena(b: BorradorContrasena): ErroresContrasena {
  const errores: ErroresContrasena = {};
  if (b.actual === '') {
    errores.actual = ERROR_ACTUAL_VACIA;
  }
  const errNueva = errorDeContrasena(b.nueva);
  if (errNueva !== null) {
    errores.nueva = errNueva;
  } else if (b.nueva === b.actual) {
    errores.nueva = ERROR_IGUAL_A_ACTUAL;
  }
  if (b.confirmacion !== b.nueva) {
    errores.confirmacion = ERROR_NO_COINCIDEN;
  }
  return errores;
}

export function primerErrorContrasena(e: ErroresContrasena): CampoContrasena | null {
  if (e.actual !== undefined) {
    return 'actual';
  }
  if (e.nueva !== undefined) {
    return 'nueva';
  }
  if (e.confirmacion !== undefined) {
    return 'confirmacion';
  }
  return null;
}

export function campoDelErrorContrasena(error: unknown): 'actual' | 'nueva' | null {
  if (error instanceof ErrorApi && error.estado === 400) {
    if (error.codigo === 'CONTRASENA_INCORRECTA') {
      return 'actual';
    }
    if (error.codigo === 'CONTRASENA_INVALIDA' || error.codigo === 'CONTRASENA_REPETIDA') {
      return 'nueva';
    }
  }
  return null;
}

export interface BorradorSupresion {
  contrasena: string;
  entiendo: boolean;
}

export const SUPRESION_VACIA: BorradorSupresion = {
  contrasena: '',
  entiendo: false,
};

export function validarSupresion(b: BorradorSupresion): Partial<Record<'contrasena' | 'entiendo', string>> {
  const errores: Partial<Record<'contrasena' | 'entiendo', string>> = {};
  if (b.contrasena === '') {
    errores.contrasena = ERROR_CONTRASENA_VACIA;
  }
  if (!b.entiendo) {
    errores.entiendo = ERROR_ENTIENDO;
  }
  return errores;
}
