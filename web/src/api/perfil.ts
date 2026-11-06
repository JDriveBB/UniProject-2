import { api } from './cliente';
import type {
  CodigoGenerado,
  DatosPersonales,
  SolicitudCambioContrasena,
  SolicitudNombre,
  SolicitudSupresion,
  UsuarioSesion,
} from './tipos';

/** verDatosPersonales: 200 con los datos personales del titular; 401 NO_AUTENTICADO. */
export function datosPersonales(): Promise<DatosPersonales> {
  return api.get<DatosPersonales>('/api/perfil/datos');
}

/** rectificarNombre: 200 con UsuarioSesion actualizado; 400 VALIDACION o NOMBRE_INVALIDO. */
export function rectificarNombre(nombre: string): Promise<UsuarioSesion> {
  const solicitud: SolicitudNombre = { nombre };
  return api.put<UsuarioSesion>('/api/perfil/nombre', solicitud);
}

/** cambiarContrasena: 204; 400 CONTRASENA_INCORRECTA, CONTRASENA_INVALIDA o CONTRASENA_REPETIDA; 429. */
export async function cambiarContrasena(contrasenaActual: string, contrasenaNueva: string): Promise<void> {
  const solicitud: SolicitudCambioContrasena = { contrasenaActual, contrasenaNueva };
  await api.post<void>('/api/perfil/contrasena', solicitud);
  // la sesión cambió de id; si falla, el cliente igual renueva el CSRF al primer 403 CSRF_INVALIDO
  try {
    await api.prepararCsrf();
  } catch {
    // ignorado deliberadamente
  }
}

/** suprimirCuenta: 204; 400 CONTRASENA_INCORRECTA; 409 CUENTA_ADMINISTRADOR, DOCENTE_CON_PROYECTOS_ACTIVOS, LIDER_DE_EQUIPO_ACTIVO. */
export function suprimirCuenta(contrasena: string): Promise<void> {
  const solicitud: SolicitudSupresion = { contrasena };
  return api.post<void>('/api/perfil/supresion', solicitud);
}

/** generarCodigoTelegram: 201 con código de vinculación; 409 TELEGRAM_NO_CONFIGURADO. */
export function generarCodigoTelegram(): Promise<CodigoGenerado> {
  return api.post<CodigoGenerado>('/api/perfil/telegram/codigo');
}

/** desvincularTelegram: 204 desvincula Telegram de la cuenta. */
export function desvincularTelegram(): Promise<void> {
  return api.borrar<void>('/api/perfil/telegram');
}
