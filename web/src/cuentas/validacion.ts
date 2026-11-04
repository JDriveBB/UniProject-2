import { ErrorApi, mensajeDeError } from '../api/cliente';
import type { SolicitudRegistro } from '../api/tipos';

/** Límites reales del núcleo (SolicitudRegistro.java, ServicioCuentas.java). Figma decía 8: manda el backend (26a-1). */
export const MINIMO_CARACTERES_CONTRASENA = 10;
export const MAXIMO_BYTES_CONTRASENA = 72;
export const MAXIMO_NOMBRE = 120;
export const MAXIMO_CORREO = 254;

export interface DatosRegistro {
  nombre: string;
  email: string;
  contrasena: string;
  confirmacion: string;
  autoriza: boolean;
}

export type ErroresRegistro = Partial<Record<keyof DatosRegistro, string>>;

/** Forma mínima de un correo; la validación completa la hace el servidor (@Email). */
const FORMA_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const codificador = new TextEncoder();

function bytesUtf8(texto: string): number {
  return codificador.encode(texto).length;
}

/** Mensaje si el correo (ya recortado) no sirve; null si sirve. */
function errorDeCorreo(email: string): string | null {
  if (email === '') {
    return 'Escribe un correo electrónico';
  }
  if (email.length > MAXIMO_CORREO) {
    return 'El correo no puede pasar de 254 caracteres';
  }
  if (!FORMA_CORREO.test(email)) {
    return 'Escribe un correo válido, como nombre@uninorte.edu.co';
  }
  return null;
}

/** Validación en el cliente para avisar antes de enviar (decisión 26a-2); no reemplaza la del servidor. */
export function validarRegistro(d: DatosRegistro): ErroresRegistro {
  const errores: ErroresRegistro = {};
  const nombre = d.nombre.trim();
  if (nombre === '') {
    errores.nombre = 'Escribe tu nombre completo';
  } else if (nombre.length > MAXIMO_NOMBRE) {
    errores.nombre = 'El nombre no puede pasar de 120 caracteres';
  }
  const correo = errorDeCorreo(d.email.trim());
  if (correo !== null) {
    errores.email = correo;
  }
  if (d.contrasena.length < MINIMO_CARACTERES_CONTRASENA) {
    errores.contrasena = 'La contraseña debe tener al menos 10 caracteres';
  } else if (bytesUtf8(d.contrasena) > MAXIMO_BYTES_CONTRASENA) {
    errores.contrasena = 'La contraseña es demasiado larga (máximo 72 bytes; las tildes y los emojis ocupan más de uno)';
  }
  if (d.confirmacion !== d.contrasena) {
    errores.confirmacion = 'Las contraseñas no coinciden';
  }
  if (!d.autoriza) {
    errores.autoriza = 'Debes autorizar el tratamiento de tus datos para crear la cuenta';
  }
  return errores;
}

export function hayErrores(errores: ErroresRegistro): boolean {
  return Object.keys(errores).length > 0;
}

/** Cuerpo de POST /api/cuentas/registro. Recorta nombre y correo (26a-4); la contraseña va tal cual. */
export function solicitudDeRegistro(d: DatosRegistro): SolicitudRegistro {
  return {
    nombre: d.nombre.trim(),
    email: d.email.trim(),
    contrasena: d.contrasena,
    autorizaTratamientoDatos: d.autoriza,
  };
}

/** Campos del contrato → etiqueta en pantalla, en el orden del formulario. */
const ETIQUETAS_REGISTRO: ReadonlyArray<readonly [string, string]> = [
  ['nombre', 'Nombre completo'],
  ['email', 'Correo electrónico'],
  ['contrasena', 'Contraseña'],
  ['autorizaTratamientoDatos', 'Autorización de tratamiento de datos'],
];

/**
 * Texto de la alerta del registro (decisión 26a-3). 409 y 400 CONTRASENA_INVALIDA traen un mensaje propio del núcleo
 * y se muestran tal cual. 400 VALIDACION trae "campo: mensaje" en detalles; el mensaje sale de Hibernate Validator y
 * puede venir en otro idioma, así que solo se nombran los campos.
 */
export function mensajeErrorRegistro(error: unknown): string {
  if (error instanceof ErrorApi && error.estado === 400 && error.codigo === 'VALIDACION') {
    const detalles = error.detalles;
    const campos = ETIQUETAS_REGISTRO
      .filter(([campo]) => detalles.some((d) => d.startsWith(campo + ':')))
      .map(([, etiqueta]) => etiqueta);
    return campos.length > 0 ? 'Revisa estos campos: ' + campos.join(', ') : error.message;
  }
  if (error instanceof ErrorApi && error.estado === 409) {
    return error.message;
  }
  return mensajeDeError(error);
}

/** Alta de docentes: mensaje si el correo no sirve; null si sirve. */
export function validarCorreoDocente(email: string): string | null {
  return errorDeCorreo(email.trim());
}

/** Alta de docentes: 409 YA_HABILITADO trae su texto; 400 VALIDACION es siempre el correo (único campo). */
export function mensajeErrorHabilitar(error: unknown): string {
  if (error instanceof ErrorApi && error.estado === 400 && error.codigo === 'VALIDACION') {
    return 'Escribe un correo válido, como nombre@uninorte.edu.co';
  }
  return mensajeDeError(error);
}
