import { ErrorApi } from '../api/cliente';
import type { CorreoHabilitadoVista, EquipoVista, MiembroVista } from '../api/tipos';
import { mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';

/** Las operaciones del API que usan estas acciones (en las pruebas, una versión falsa). */
export interface ApiQuitar {
  quitarMiembro(idEquipo: number, idUsuario: number): Promise<EquipoVista>;
  quitarMonitor(idProyecto: number, idMonitor: number): Promise<void>;
}

export type ResultadoQuitarIntegrante = { tipo: 'quitado'; equipo: EquipoVista } | { tipo: 'error'; mensaje: string };

export const YA_NO_ES_MIEMBRO = 'Esa persona ya no estaba en el equipo. Recarga la página para ver la lista actual.';
export const YA_NO_ES_MONITOR = 'Ese monitor ya no estaba asignado al proyecto.';
export const ES_LIDER_AHORA = 'Esa persona es ahora el líder del equipo. Destitúyela o nombra a otro líder antes de quitarla.';

/** Decisión 26b-16: al líder no se le ofrece "Quitar"; primero se designa otro líder. */
export function sePuedeQuitar(miembro: MiembroVista): boolean {
  return miembro.rolEquipo !== 'LIDER';
}

/** Aclaración 3: al confirmar se revisa la lista actual; null si se puede quitar, o el motivo si no. */
export function bloqueoParaQuitar(miembros: MiembroVista[], idUsuario: number): string | null {
  const miembro = miembros.find((m) => m.idUsuario === idUsuario);
  if (miembro === undefined) {
    return YA_NO_ES_MIEMBRO;
  }
  return sePuedeQuitar(miembro) ? null : ES_LIDER_AHORA;
}

/** Decisión 26b-17: quitar no borra tareas ni historial. */
export function preguntaQuitarIntegrante(nombre: string): string {
  return `¿Quitar a ${nombre} del equipo? Sus tareas y su historial se conservan, y el líder puede reasignar sus tareas.`;
}

/** Bloque 30: quitar también retira su habilitación de monitor; se puede volver a agregar. */
export function preguntaQuitarMonitor(nombre: string): string {
  return `¿Quitar a ${nombre} de los monitores del proyecto? Su historial se conserva y puedes volver a agregarlo con su correo.`;
}

export function pendientesDe(correos: CorreoHabilitadoVista[]): CorreoHabilitadoVista[] {
  return correos.filter((c) => c.registrado === false);
}

export function preguntaRetirarCorreo(correo: string): string {
  return `¿Retirar la habilitación de ${correo}? Esa persona ya no podrá crear su cuenta para este proyecto. Puedes volver a habilitarla cuando quieras.`;
}

export function preguntaRetirarCorreoCurso(correo: string): string {
  return `¿Retirar la habilitación de ${correo}? Esa persona ya no podrá crear su cuenta con este curso. Puedes volver a agregar su correo cuando quieras.`;
}

export function textoCorreoRetirado(correo: string): string {
  return `Se retiró la habilitación de ${correo}.`;
}

export interface ResultadoRetirar {
  mensaje: Mensaje | null;
  recargar: boolean;
}

/** Decisión 30f-6: retira una habilitación pendiente; 409 = la persona se registró entretanto (se recarga); 404 = ya no existía. */
export async function retirarHabilitacion(
  retirar: (idCorreoHabilitado: number) => Promise<void>,
  correo: Pick<CorreoHabilitadoVista, 'idCorreoHabilitado' | 'email'>,
): Promise<ResultadoRetirar> {
  try {
    await retirar(correo.idCorreoHabilitado);
    return { mensaje: { tipo: 'exito', texto: textoCorreoRetirado(correo.email) }, recargar: true };
  } catch (e) {
    if (e instanceof ErrorApi) {
      if (e.estado === 404) {
        return { mensaje: null, recargar: true };
      }
      if (e.estado === 409) {
        return { mensaje: { tipo: 'error', texto: e.message }, recargar: true };
      }
    }
    return { mensaje: { tipo: 'error', texto: mensajeErrorApi(e, []) }, recargar: false };
  }
}

export function textoIntegranteQuitado(nombre: string): string {
  return `${nombre} salió del equipo. Sus tareas y su historial se conservan.`;
}

export function textoMonitorQuitado(nombre: string): string {
  return `${nombre} ya no es monitor de este proyecto.`;
}

/** quitarMiembro con los mensajes de 26b-19. */
export async function quitarIntegrante(api: Pick<ApiQuitar, 'quitarMiembro'>, idEquipo: number,
                                       idUsuario: number): Promise<ResultadoQuitarIntegrante> {
  try {
    return { tipo: 'quitado', equipo: await api.quitarMiembro(idEquipo, idUsuario) };
  } catch (e) {
    if (e instanceof ErrorApi && e.estado === 404) {
      return { tipo: 'error', mensaje: YA_NO_ES_MIEMBRO };
    }
    return { tipo: 'error', mensaje: mensajeErrorApi(e, []) };
  }
}

/** quitarMonitor: éxito, aviso si ya no estaba (404) o error. Quien llama recarga la lista en los dos primeros. */
export async function quitarMonitorDelProyecto(api: Pick<ApiQuitar, 'quitarMonitor'>, idProyecto: number,
                                               idMonitor: number, nombre: string): Promise<Mensaje> {
  try {
    await api.quitarMonitor(idProyecto, idMonitor);
    return { tipo: 'exito', texto: textoMonitorQuitado(nombre) };
  } catch (e) {
    if (e instanceof ErrorApi && e.estado === 404) {
      return { tipo: 'aviso', texto: YA_NO_ES_MONITOR };
    }
    return { tipo: 'error', texto: mensajeErrorApi(e, []) };
  }
}

/** Bloque 51: destituir deja al equipo sin líder y el núcleo abre una votación. */
export function preguntaDestituir(nombre: string, horas: number): string {
  return `¿Destituir a ${nombre} como líder? El equipo queda sin líder y se abre una votación de ${horas} horas entre sus integrantes.`;
}

/** Bloque 51: nombrar a un líder con una votación abierta la cancela. */
export function preguntaNombrarConRonda(nombre: string): string {
  return `Hay una votación abierta en este equipo. Si nombras a ${nombre} como líder, la votación se cancela. ¿Continuar?`;
}
