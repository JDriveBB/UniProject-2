import { describe, expect, it, vi } from 'vitest';
import { crearCliente, ErrorApi, mensajeDeError } from './cliente';

function respuesta(estado: number, cuerpo?: unknown): Response {
  return new Response(cuerpo === undefined ? null : JSON.stringify(cuerpo), {
    status: estado,
    headers: cuerpo === undefined ? {} : { 'Content-Type': 'application/json' },
  });
}

interface Llamada {
  ruta: string;
  init: RequestInit;
}

/** fetch falso: /api/sesion/csrf deja una cookie nueva ("token-<n.º de llamada>"); lo demás sale de la cola. */
function preparar(respuestas: Response[], cookieInicial?: string) {
  const cookies = new Map<string, string>();
  if (cookieInicial !== undefined) {
    cookies.set('XSRF-TOKEN', cookieInicial);
  }
  const llamadas: Llamada[] = [];
  const cola = [...respuestas];
  const falsoFetch = vi.fn(async (entrada: RequestInfo | URL, init?: RequestInit) => {
    const ruta = String(entrada);
    llamadas.push({ ruta, init: init ?? {} });
    if (ruta === '/api/sesion/csrf') {
      cookies.set('XSRF-TOKEN', 'token-' + llamadas.length);
      return respuesta(204);
    }
    const siguiente = cola.shift();
    if (siguiente === undefined) {
      throw new Error('Sin respuesta preparada para ' + ruta);
    }
    return siguiente;
  });
  const expirada = vi.fn();
  const cliente = crearCliente({
    fetch: falsoFetch as unknown as typeof fetch,
    leerCookie: (nombre) => cookies.get(nombre),
    alExpirarSesion: expirada,
  });
  return { cliente, llamadas, expirada };
}

function cabecera(llamada: Llamada | undefined, nombre: string): string | undefined {
  return (llamada?.init.headers as Record<string, string> | undefined)?.[nombre];
}

describe('cliente de la API', () => {
  it('GET devuelve el JSON, usa la cookie de sesión y no manda CSRF', async () => {
    const { cliente, llamadas } = preparar([respuesta(200, { idUsuario: 1 })]);
    await expect(cliente.get('/api/sesion/yo')).resolves.toEqual({ idUsuario: 1 });
    expect(llamadas).toHaveLength(1);
    expect(llamadas[0]?.init.credentials).toBe('same-origin');
    expect(cabecera(llamadas[0], 'X-XSRF-TOKEN')).toBeUndefined();
  });

  it('POST manda la cookie XSRF-TOKEN en la cabecera X-XSRF-TOKEN', async () => {
    const { cliente, llamadas } = preparar([respuesta(201, { idProyecto: 5 })], 'abc');
    await expect(cliente.post('/api/proyectos', { nombre: 'P' })).resolves.toEqual({ idProyecto: 5 });
    expect(cabecera(llamadas[0], 'X-XSRF-TOKEN')).toBe('abc');
    expect(cabecera(llamadas[0], 'Content-Type')).toBe('application/json');
    expect(llamadas[0]?.init.body).toBe('{"nombre":"P"}');
  });

  it('sin cookie pide /api/sesion/csrf antes de un POST; 204 devuelve undefined', async () => {
    const { cliente, llamadas } = preparar([respuesta(204)]);
    await expect(cliente.post('/api/sesion/logout')).resolves.toBeUndefined();
    expect(llamadas.map((l) => l.ruta)).toEqual(['/api/sesion/csrf', '/api/sesion/logout']);
    expect(cabecera(llamadas[1], 'X-XSRF-TOKEN')).toBe('token-1');
  });

  it('202 sin cuerpo devuelve undefined', async () => {
    const { cliente, llamadas } = preparar([respuesta(202)], 'abc');
    await expect(cliente.post('/api/cuentas/recuperacion', { email: 'ana@uninorte.edu.co' })).resolves.toBeUndefined();
    expect(llamadas).toHaveLength(1);
    expect(llamadas[0]?.ruta).toBe('/api/cuentas/recuperacion');
    expect(cabecera(llamadas[0], 'X-XSRF-TOKEN')).toBe('abc');
  });

  it('403 CSRF_INVALIDO renueva el token y reintenta una vez', async () => {
    const csrf = { codigo: 'CSRF_INVALIDO', mensaje: 'La sesión cambió', detalles: [] };
    const { cliente, llamadas } = preparar([respuesta(403, csrf), respuesta(200, { porcentajeAvance: 50 })], 'viejo');
    await expect(cliente.put('/api/tareas/1/avance', { porcentajeAvance: 50 })).resolves.toEqual({
      porcentajeAvance: 50,
    });
    expect(llamadas.map((l) => l.ruta)).toEqual(['/api/tareas/1/avance', '/api/sesion/csrf', '/api/tareas/1/avance']);
    expect(cabecera(llamadas[2], 'X-XSRF-TOKEN')).toBe('token-2');
  });

  it('un segundo 403 CSRF_INVALIDO ya no se reintenta', async () => {
    const csrf = { codigo: 'CSRF_INVALIDO', mensaje: 'La sesión cambió', detalles: [] };
    const { cliente, llamadas } = preparar([respuesta(403, csrf), respuesta(403, csrf)], 'viejo');
    await expect(cliente.post('/api/notificaciones/leidas')).rejects.toMatchObject({
      estado: 403,
      codigo: 'CSRF_INVALIDO',
    });
    expect(llamadas).toHaveLength(3);
  });

  it('401 fuera de /api/sesion avisa que la sesión expiró', async () => {
    const { cliente, expirada } = preparar([respuesta(401)]);
    await expect(cliente.get('/api/proyectos')).rejects.toMatchObject({ estado: 401, codigo: 'NO_AUTENTICADO' });
    expect(expirada).toHaveBeenCalledTimes(1);
  });

  it('el 401 del login no cuenta como sesión expirada', async () => {
    const credenciales = { codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'Correo o contraseña incorrectos', detalles: [] };
    const { cliente, expirada } = preparar([respuesta(401, credenciales)], 'a');
    const error = await cliente.post('/api/sesion/login', { email: 'x', contrasena: 'y' }).catch((e: unknown) => e);
    expect(error).toMatchObject({ codigo: 'CREDENCIALES_INVALIDAS' });
    expect(mensajeDeError(error)).toBe('Correo o contraseña incorrectos');
    expect(expirada).not.toHaveBeenCalled();
  });

  it('409 conserva código y detalles para mostrarlos', async () => {
    const conflicto = {
      codigo: 'PRERREQUISITOS_INCOMPLETOS',
      mensaje: 'Completa las tareas marcadas como prerrequisito crítico',
      detalles: ['Requisitos'],
    };
    const { cliente } = preparar([respuesta(409, conflicto)], 'a');
    const error = await cliente.post('/api/hitos-equipo/3/envio').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ErrorApi);
    expect(error).toMatchObject({ estado: 409, codigo: 'PRERREQUISITOS_INCOMPLETOS', detalles: ['Requisitos'] });
    expect(mensajeDeError(error)).toBe('Completa las tareas marcadas como prerrequisito crítico: Requisitos');
  });

  it('400 VALIDACION muestra etiquetas, no nombres técnicos', () => {
    const detalles = [
      'emails[0]: must be a well-formed email address',
      'criterios[1].nota: must be less than or equal to 5',
      'emails[2]: must not be blank',
    ];
    expect(mensajeDeError(new ErrorApi(400, 'VALIDACION', 'Datos inválidos', detalles))).toBe(
      'Revisa estos campos: Correo, Nota',
    );
    expect(mensajeDeError(new ErrorApi(400, 'VALIDACION', 'Datos inválidos', ['raro: x']))).toBe('Datos inválidos');
  });

  it('sin red lanza SIN_CONEXION', async () => {
    const sinRed = vi.fn(async () => {
      throw new TypeError('fallo de red');
    });
    const cliente = crearCliente({ fetch: sinRed as unknown as typeof fetch, leerCookie: () => undefined });
    await expect(cliente.get('/api/proyectos')).rejects.toMatchObject({ estado: 0, codigo: 'SIN_CONEXION' });
  });
});
