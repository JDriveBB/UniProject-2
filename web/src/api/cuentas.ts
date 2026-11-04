import { api } from './cliente';
import type { CorreoHabilitadoVista, SolicitudHabilitarCorreo, SolicitudRegistro, UsuarioSesion } from './tipos';

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
