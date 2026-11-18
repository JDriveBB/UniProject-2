import type { RespuestaError } from './tipos';

/** Lo emite el cliente cuando la API responde 401 fuera de /api/sesion/* (decisión 25-5). */
export const EVENTO_SESION_EXPIRADA = 'uniproject:sesion-expirada';

const COOKIE_CSRF = 'XSRF-TOKEN';
const CABECERA_CSRF = 'X-XSRF-TOKEN';
const RUTA_CSRF = '/api/sesion/csrf';

type Metodo = 'GET' | 'POST' | 'PUT' | 'DELETE';

/** Error de la API en el formato RespuestaError. estado 0 = sin conexión. */
export class ErrorApi extends Error {
  readonly estado: number;
  readonly codigo: string;
  readonly detalles: string[];

  constructor(estado: number, codigo: string, mensaje: string, detalles: string[] = []) {
    super(mensaje);
    this.name = 'ErrorApi';
    this.estado = estado;
    this.codigo = codigo;
    this.detalles = detalles;
  }
}

export interface OpcionesCliente {
  fetch?: typeof fetch;
  leerCookie?: (nombre: string) => string | undefined;
  alExpirarSesion?: () => void;
}

export interface ClienteApi {
  get<T>(ruta: string): Promise<T>;
  post<T>(ruta: string, cuerpo?: unknown): Promise<T>;
  put<T>(ruta: string, cuerpo?: unknown): Promise<T>;
  borrar<T>(ruta: string): Promise<T>;
  subirArchivo<T>(ruta: string, campo: string, archivo: Blob, nombre: string): Promise<T>;
  descargar(ruta: string): Promise<Blob>;
  prepararCsrf(): Promise<void>;
}

/** Cuando la respuesta no trae RespuestaError (p. ej. el 401 de Spring Security, sin cuerpo). */
const POR_ESTADO: Partial<Record<number, readonly [string, string]>> = {
  400: ['SOLICITUD_INVALIDA', 'Revisa los datos e inténtalo de nuevo'],
  401: ['NO_AUTENTICADO', 'Tu sesión terminó. Vuelve a ingresar'],
  403: ['ACCESO_DENEGADO', 'No tienes permiso para esta acción'],
  404: ['NO_ENCONTRADO', 'Lo que buscas no existe o fue eliminado'],
  409: ['CONFLICTO', 'La operación choca con el estado actual. Recarga e inténtalo de nuevo'],
};

function leerCookieDelNavegador(nombre: string): string | undefined {
  const prefijo = nombre + '=';
  for (const parte of document.cookie.split(';')) {
    const limpia = parte.trim();
    if (limpia.startsWith(prefijo)) {
      return decodeURIComponent(limpia.substring(prefijo.length));
    }
  }
  return undefined;
}

function esRespuestaError(valor: unknown): valor is RespuestaError {
  if (typeof valor !== 'object' || valor === null) {
    return false;
  }
  const v = valor as Record<string, unknown>;
  return typeof v.codigo === 'string' && typeof v.mensaje === 'string';
}

async function errorDe(respuesta: Response): Promise<ErrorApi> {
  try {
    const cuerpo: unknown = await respuesta.json();
    if (esRespuestaError(cuerpo)) {
      const detalles = Array.isArray(cuerpo.detalles)
        ? cuerpo.detalles.filter((d): d is string => typeof d === 'string')
        : [];
      return new ErrorApi(respuesta.status, cuerpo.codigo, cuerpo.mensaje, detalles);
    }
  } catch {
    // Sin cuerpo o no es JSON: se usa el mensaje por estado.
  }
  const [codigo, mensaje] = POR_ESTADO[respuesta.status] ?? [
    'ERROR_SERVIDOR',
    'Algo falló en el servidor. Inténtalo más tarde',
  ];
  return new ErrorApi(respuesta.status, codigo, mensaje);
}

export function crearCliente(opciones: OpcionesCliente = {}): ClienteApi {
  const hacerFetch: typeof fetch = opciones.fetch ?? ((entrada, init) => fetch(entrada, init));
  const leerCookie = opciones.leerCookie ?? leerCookieDelNavegador;
  const alExpirarSesion = opciones.alExpirarSesion ?? (() => undefined);

  async function llamar(ruta: string, init: RequestInit): Promise<Response> {
    try {
      return await hacerFetch(ruta, init);
    } catch {
      throw new ErrorApi(0, 'SIN_CONEXION', 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo');
    }
  }

  async function prepararCsrf(): Promise<void> {
    const r = await llamar(RUTA_CSRF, { method: 'GET', credentials: 'same-origin' });
    if (!r.ok) {
      throw await errorDe(r);
    }
  }

  async function enviar(metodo: Metodo, ruta: string, cuerpo: BodyInit | undefined, tipo: string | undefined,
                        reintentar: boolean): Promise<Response> {
    const cabeceras: Record<string, string> = {};
    if (tipo !== undefined) {
      cabeceras['Content-Type'] = tipo;
    }
    if (metodo !== 'GET') {
      let token = leerCookie(COOKIE_CSRF);
      if (token === undefined) {
        await prepararCsrf();
        token = leerCookie(COOKIE_CSRF);
      }
      if (token !== undefined) {
        cabeceras[CABECERA_CSRF] = token;
      }
    }
    const r = await llamar(ruta, { method: metodo, headers: cabeceras, body: cuerpo, credentials: 'same-origin' });
    if (r.ok) {
      return r;
    }
    const error = await errorDe(r);
    if (r.status === 403 && error.codigo === 'CSRF_INVALIDO' && reintentar) {
      await prepararCsrf();
      return enviar(metodo, ruta, cuerpo, tipo, false);
    }
    if (r.status === 401 && !ruta.startsWith('/api/sesion/')) {
      alExpirarSesion();
    }
    throw error;
  }

  async function json<T>(metodo: Metodo, ruta: string, cuerpo?: unknown): Promise<T> {
    const conCuerpo = cuerpo !== undefined;
    const r = await enviar(metodo, ruta, conCuerpo ? JSON.stringify(cuerpo) : undefined,
      conCuerpo ? 'application/json' : undefined, true);
    // 202 sin cuerpo: POST /api/cuentas/recuperacion (bloque 28).
    if (r.status === 204 || r.status === 202) {
      return undefined as T;
    }
    return (await r.json()) as T;
  }

  return {
    get<T>(ruta: string) {
      return json<T>('GET', ruta);
    },
    post<T>(ruta: string, cuerpo?: unknown) {
      return json<T>('POST', ruta, cuerpo);
    },
    put<T>(ruta: string, cuerpo?: unknown) {
      return json<T>('PUT', ruta, cuerpo);
    },
    borrar<T>(ruta: string) {
      return json<T>('DELETE', ruta);
    },
    async subirArchivo<T>(ruta: string, campo: string, archivo: Blob, nombre: string) {
      const datos = new FormData();
      datos.append(campo, archivo, nombre);
      const r = await enviar('POST', ruta, datos, undefined, true);
      return (await r.json()) as T;
    },
    async descargar(ruta: string) {
      const r = await enviar('GET', ruta, undefined, undefined, true);
      return r.blob();
    },
    prepararCsrf,
  };
}

/** Campo del contrato → etiqueta para el usuario (los nombres técnicos no se muestran). */
export const ETIQUETAS_CAMPOS: Readonly<Record<string, string>> = {
  nombre: 'Nombre',
  email: 'Correo',
  emails: 'Correo',
  contrasena: 'Contraseña',
  contrasenaActual: 'Contraseña actual',
  contrasenaNueva: 'Contraseña nueva',
  codigo: 'Código',
  nombreEquipo: 'Nombre del equipo',
  nombreHito: 'Nombre del hito',
  descripcion: 'Descripción',
  orden: 'Orden',
  fechaInicio: 'Fecha de inicio',
  fechaFin: 'Fecha de fin',
  fechaLimite: 'Fecha límite',
  diasInactividadAlerta: 'Alerta de inactividad',
  usaMonitores: 'Monitores',
  pesoPorcentaje: 'Peso (%)',
  puntajeMaximo: 'Puntaje máximo',
  titulo: 'Título',
  horasEstimadas: 'Horas estimadas',
  idEncargado: 'Encargado',
  esPrerrequisitoCritico: 'Prerrequisito crítico',
  porcentajeAvance: 'Avance (%)',
  observacion: 'Observación',
  observaciones: 'Observaciones',
  idEvidencia: 'Evidencia',
  url: 'Enlace',
  contenido: 'Comentario',
  mensaje: 'Mensaje',
  prerrequisitos: 'Prerrequisitos',
  idNuevoEncargado: 'Nuevo encargado',
  tipo: 'Tipo de reasignación',
  tituloParcial: 'Título de la parte',
  horasParcial: 'Horas de la parte',
  motivo: 'Motivo',
  criterios: 'Criterios',
  nota: 'Nota',
  notaGrupal: 'Nota grupal',
  retroalimentacion: 'Retroalimentación',
  rol: 'Rol',
  idUsuario: 'Integrante',
  autorizaTratamientoDatos: 'Autorización de tratamiento de datos',
};

/** 'criterios[0].nota: must be…' → 'nota'; 'emails[3]: …' → 'emails'. */
function campoDeDetalle(detalle: string): string {
  const antes = detalle.split(':')[0] ?? '';
  const sinIndices = antes.replace(/\[\d+\]/g, '');
  return sinIndices.split('.').pop() ?? '';
}

/** Texto para mostrar al usuario; con 400 y 409 agrega los detalles (campos inválidos, títulos pendientes). */
export function mensajeDeError(error: unknown): string {
  if (error instanceof ErrorApi) {
    if (error.estado === 400 && error.codigo === 'VALIDACION') {
      const etiquetas: string[] = [];
      for (const detalle of error.detalles) {
        const etiqueta = ETIQUETAS_CAMPOS[campoDeDetalle(detalle)];
        if (etiqueta !== undefined && !etiquetas.includes(etiqueta)) {
          etiquetas.push(etiqueta);
        }
      }
      return etiquetas.length > 0 ? 'Revisa estos campos: ' + etiquetas.join(', ') : error.message;
    }
    if ((error.estado === 400 || error.estado === 409) && error.detalles.length > 0) {
      return error.message + ': ' + error.detalles.join('; ');
    }
    return error.message;
  }
  return 'Ocurrió un error inesperado. Recarga la página';
}

export const api: ClienteApi = crearCliente({
  alExpirarSesion: () => window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA)),
});
