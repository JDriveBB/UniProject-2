import type { EstadoSesion } from './ContextoSesion';

export const TEXTO_SESION_EXPIRADA = 'Tu sesión terminó. Copia lo que no hayas guardado y vuelve a ingresar.';
export const BOTON_SESION_EXPIRADA = 'Ingresar de nuevo';

/** El aviso solo se ve con la sesión marcada como vencida y mientras la pantalla sigue autenticada. */
export function avisoSesionVisible(sesion: EstadoSesion, expirada: boolean): boolean {
  return expirada && sesion.estado === 'autenticado';
}
