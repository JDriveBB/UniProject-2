import { ErrorApi } from '../api/cliente';
import type { EquipoVista, ResultadoHabilitacion, SolicitudCorreos } from '../api/tipos';
import { CAMPOS_CORREO, mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';

/** Las operaciones del API que usan estas acciones (en las pruebas, una versión falsa). */
export interface ApiEquipos {
  habilitarCorreos(idProyecto: number, solicitud: SolicitudCorreos): Promise<ResultadoHabilitacion>;
  agregarMiembro(idEquipo: number, email: string): Promise<EquipoVista>;
}

export type ResultadoAgregar =
  | { tipo: 'agregado'; equipo: EquipoVista }
  | { tipo: 'pendiente'; mensaje: string }
  | { tipo: 'error'; mensaje: string };

export const PENDIENTE_ESTUDIANTE =
  'Correo habilitado. Cuando el estudiante cree su cuenta con ese correo, agrégalo de nuevo a este equipo.';
export const OTRO_ROL_ESTUDIANTE = 'Ese correo ya tiene una cuenta que no es de estudiante.';

/**
 * "+ Agregar estudiante" (decisión 26b-7, K2): habilita el correo como ESTUDIANTE en el proyecto y lo agrega al
 * equipo. Si aún no tiene cuenta (409 ESTUDIANTE_NO_REGISTRADO), queda habilitado y se avisa: no es un error.
 */
export async function agregarEstudiante(api: ApiEquipos, idProyecto: number, idEquipo: number,
                                        email: string): Promise<ResultadoAgregar> {
  const correo = email.trim();
  try {
    const habilitacion = await api.habilitarCorreos(idProyecto, { emails: [correo], rol: 'ESTUDIANTE' });
    if (habilitacion.conOtroRol.length > 0) {
      return { tipo: 'error', mensaje: OTRO_ROL_ESTUDIANTE };
    }
    return { tipo: 'agregado', equipo: await api.agregarMiembro(idEquipo, correo) };
  } catch (e) {
    if (e instanceof ErrorApi && e.estado === 409 && e.codigo === 'ESTUDIANTE_NO_REGISTRADO') {
      return { tipo: 'pendiente', mensaje: PENDIENTE_ESTUDIANTE };
    }
    return { tipo: 'error', mensaje: mensajeErrorApi(e, CAMPOS_CORREO) };
  }
}

/** "+ Agregar monitor" (RF14): habilita el correo como MONITOR; si ya tiene cuenta, queda asignado al proyecto. */
export async function agregarMonitor(api: Pick<ApiEquipos, 'habilitarCorreos'>, idProyecto: number,
                                     email: string): Promise<Mensaje> {
  const correo = email.trim();
  try {
    const r = await api.habilitarCorreos(idProyecto, { emails: [correo], rol: 'MONITOR' });
    if (r.conOtroRol.length > 0) {
      return { tipo: 'error', texto: 'Ese correo ya tiene una cuenta que no es de monitor.' };
    }
    if (r.yaHabilitados.length > 0) {
      return { tipo: 'aviso', texto: 'Ese correo ya estaba habilitado en este proyecto.' };
    }
    if (r.habilitados.some((c) => c.registrado)) {
      return { tipo: 'exito', texto: 'Monitor agregado.' };
    }
    return { tipo: 'aviso', texto: 'Correo habilitado como monitor. Aparecerá aquí cuando cree su cuenta.' };
  } catch (e) {
    return { tipo: 'error', texto: mensajeErrorApi(e, CAMPOS_CORREO) };
  }
}

/** El equipo que devolvió el API reemplaza al de igual id; si es nuevo, va al final. */
export function reemplazarEquipo(equipos: EquipoVista[], equipo: EquipoVista): EquipoVista[] {
  return equipos.some((e) => e.idEquipo === equipo.idEquipo)
    ? equipos.map((e) => (e.idEquipo === equipo.idEquipo ? equipo : e))
    : [...equipos, equipo];
}
