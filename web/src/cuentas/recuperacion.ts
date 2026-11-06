import { ErrorApi, mensajeDeError } from '../api/cliente';
import type { SolicitudRestablecer } from '../api/tipos';
import { errorDeContrasena } from './validacion';

/** Bloque 26a-bis: recuperar la contraseña por Telegram (endpoints del bloque 28). El código es secreto: estas funciones no lo guardan ni lo incluyen en mensajes. */

/** = ContratoInterno.PATRON_CODIGO_RECUPERACION (eventos-contrato). */
export const PATRON_CODIGO = /^[A-Za-z0-9_-]{43}$/;

export function codigoDelFragmento(hash: string): string | null {
  const limpio = hash.startsWith('#') ? hash.slice(1) : hash;
  try {
    const params = new URLSearchParams(limpio);
    const codigo = params.get('codigo')?.trim();
    if (codigo && PATRON_CODIGO.test(codigo)) {
      return codigo;
    }
  } catch {
    // No lanza
  }
  return null;
}

export interface DatosRestablecer {
  codigo: string;
  contrasena: string;
  confirmacion: string;
}

export type ErroresRestablecer = Partial<Record<keyof DatosRestablecer, string>>;

export function validarRestablecer(d: DatosRestablecer): ErroresRestablecer {
  const errores: ErroresRestablecer = {};
  const codigoTrim = d.codigo.trim();
  if (codigoTrim === '') {
    errores.codigo = 'Escribe el código que te llegó por Telegram';
  } else if (!PATRON_CODIGO.test(codigoTrim)) {
    errores.codigo = 'Copia el código completo del mensaje de Telegram (43 caracteres)';
  }
  const errorContrasena = errorDeContrasena(d.contrasena);
  if (errorContrasena !== null) {
    errores.contrasena = errorContrasena;
  }
  if (d.confirmacion !== d.contrasena) {
    errores.confirmacion = 'Las contraseñas no coinciden';
  }
  return errores;
}

export function solicitudDeRestablecer(d: DatosRestablecer): SolicitudRestablecer {
  return {
    codigo: d.codigo.trim(),
    contrasenaNueva: d.contrasena,
  };
}

export interface ErrorRestablecer {
  mensaje: string;
  pedirNuevo: boolean;
}

export const CODIGO_NO_VALIDO = 'El enlace no es válido o ya venció. Pide uno nuevo';

export function errorDeRestablecer(error: unknown): ErrorRestablecer {
  if (error instanceof ErrorApi) {
    if (error.estado === 400 && error.codigo === 'CODIGO_INVALIDO') {
      return { mensaje: error.message, pedirNuevo: true };
    }
    if (error.estado === 404 || error.estado === 410) {
      return { mensaje: CODIGO_NO_VALIDO, pedirNuevo: true };
    }
    if (error.estado === 400 && error.codigo === 'VALIDACION') {
      return { mensaje: 'Revisa los datos e inténtalo de nuevo', pedirNuevo: false };
    }
    return { mensaje: error.message, pedirNuevo: false };
  }
  return { mensaje: mensajeDeError(error), pedirNuevo: false };
}
