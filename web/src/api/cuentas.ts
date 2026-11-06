import { api } from './cliente';
import type {
  CorreoHabilitadoVista,
  SolicitudHabilitarCorreo,
  SolicitudRecuperacion,
  SolicitudRegistro,
  SolicitudRestablecer,
  UsuarioSesion,
} from './tipos';

/** registrarCuenta: 201 sin iniciar sesión; 409 CORREO_NO_HABILITADO, HABILITACION_AMBIGUA o CUENTA_EXISTENTE. */
export function registrarCuenta(solicitud: SolicitudRegistro): Promise<UsuarioSesion> {
  return api.post<UsuarioSesion>('/api/cuentas/registro', solicitud);
}

/** listarDocentesHabilitados: solo ADMINISTRADOR; vienen ordenados por correo. */
export function listarDocentesHabilitados(): Promise<CorreoHabilitadoVista[]> {
  return api.get<CorreoHabilitadoVista[]>('/api/admin/docentes');
}

/** habilitarDocente: solo ADMINISTRADOR; 409 YA_HABILITADO si el correo ya está habilitado o tiene cuenta. */
export function habilitarDocente(email: string): Promise<CorreoHabilitadoVista> {
  const solicitud: SolicitudHabilitarCorreo = { email };
  return api.post<CorreoHabilitadoVista>('/api/admin/docentes', solicitud);
}

/** 202 → undefined, exista o no la cuenta; 400 VALIDACION; 429 DEMASIADAS_SOLICITUDES. */
export function solicitarRecuperacion(email: string): Promise<void> {
  const solicitud: SolicitudRecuperacion = { email };
  return api.post<void>('/api/cuentas/recuperacion', solicitud);
}

/** 204 → undefined; 400 CODIGO_INVALIDO, CONTRASENA_INVALIDA o VALIDACION; 429. */
export function restablecerContrasena(solicitud: SolicitudRestablecer): Promise<void> {
  return api.post<void>('/api/cuentas/recuperacion/restablecer', solicitud);
}
