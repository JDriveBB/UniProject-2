import { ErrorApi } from '../api/cliente';
import type { EquipoVista, ResultadoHabilitacion, SolicitudCorreos } from '../api/tipos';
import { CAMPOS_CORREO, mensajeErrorApi } from './errores';
import { separarCorreos } from './correos';

/** Las operaciones del API que usan estas acciones (en las pruebas, una versión falsa). */
export interface ApiEquipos {
  habilitarCorreos(idProyecto: number, solicitud: SolicitudCorreos): Promise<ResultadoHabilitacion>;
  agregarMiembro(idEquipo: number, email: string): Promise<EquipoVista>;
}

export interface ErrorAlAgregar {
  email: string;
  motivo: string;
}

export interface ResumenEstudiantes {
  agregados: string[];
  habilitados: string[];
  yaHabilitados: string[];
  conOtroRol: string[];
  errores: ErrorAlAgregar[];
  equipo?: EquipoVista;
}

export interface ResumenMonitores {
  asignados: string[];
  habilitados: string[];
  yaHabilitados: string[];
  conOtroRol: string[];
}

export const PENDIENTE_ESTUDIANTE =
  'Correo habilitado. Cuando el estudiante cree su cuenta con ese correo, agrégalo de nuevo a este equipo.';
export const OTRO_ROL_ESTUDIANTE = 'Ese correo ya tiene una cuenta que no es de estudiante.';

/**
 * "+ Agregar estudiante" en lote (bloque 31): habilita todos los correos en UNA llamada
 * habilitarCorreos(idProyecto, { emails, rol: 'ESTUDIANTE' }); para los que ya tienen cuenta de estudiante,
 * los agrega al equipo con agregarMiembro uno a uno; los demás quedan solo habilitados.
 * Devuelve un resumen por categoría. Un error en un correo no detiene los demás.
 */
export async function agregarEstudiante(
  api: ApiEquipos,
  idProyecto: number,
  idEquipo: number,
  emails: string[] | string,
): Promise<ResumenEstudiantes> {
  const lista = typeof emails === 'string' ? separarCorreos(emails) : emails;
  if (lista.length === 0) {
    return {
      agregados: [],
      habilitados: [],
      yaHabilitados: [],
      conOtroRol: [],
      errores: [],
    };
  }

  const hab = await api.habilitarCorreos(idProyecto, { emails: lista, rol: 'ESTUDIANTE' });
  const conCuenta = hab.habilitados.filter((c) => c.registrado);
  const sinCuenta = hab.habilitados.filter((c) => !c.registrado);

  const agregados: string[] = [];
  const errores: ErrorAlAgregar[] = [];
  let ultimoEquipo: EquipoVista | undefined;

  for (const c of conCuenta) {
    try {
      ultimoEquipo = await api.agregarMiembro(idEquipo, c.email);
      agregados.push(c.email);
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 409 && e.codigo === 'ESTUDIANTE_NO_REGISTRADO') {
        sinCuenta.push(c);
      } else {
        const motivo = mensajeErrorApi(e, CAMPOS_CORREO);
        errores.push({ email: c.email, motivo });
      }
    }
  }

  const yaHabilitados: string[] = [];
  for (const correo of hab.yaHabilitados) {
    try {
      ultimoEquipo = await api.agregarMiembro(idEquipo, correo);
      agregados.push(correo);
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 409 && e.codigo === 'ESTUDIANTE_NO_REGISTRADO') {
        yaHabilitados.push(correo);
      } else {
        const motivo = mensajeErrorApi(e, CAMPOS_CORREO);
        errores.push({ email: correo, motivo });
      }
    }
  }

  return {
    agregados,
    habilitados: sinCuenta.map((c) => c.email),
    yaHabilitados,
    conOtroRol: hab.conOtroRol,
    errores,
    equipo: ultimoEquipo,
  };
}

/**
 * "+ Agregar monitor" en lote (bloque 31): igual con rol: 'MONITOR';
 * resumen: asignados/habilitados, ya habilitados, con otro rol.
 */
export async function agregarMonitor(
  api: Pick<ApiEquipos, 'habilitarCorreos'>,
  idProyecto: number,
  emails: string[] | string,
): Promise<ResumenMonitores> {
  const lista = typeof emails === 'string' ? separarCorreos(emails) : emails;
  if (lista.length === 0) {
    return {
      asignados: [],
      habilitados: [],
      yaHabilitados: [],
      conOtroRol: [],
    };
  }

  const r = await api.habilitarCorreos(idProyecto, { emails: lista, rol: 'MONITOR' });
  return {
    asignados: r.habilitados.filter((c) => c.registrado).map((c) => c.email),
    habilitados: r.habilitados.filter((c) => !c.registrado).map((c) => c.email),
    yaHabilitados: r.yaHabilitados,
    conOtroRol: r.conOtroRol,
  };
}

/** El equipo que devolvió el API reemplaza al de igual id; si es nuevo, va al final. */
export function reemplazarEquipo(equipos: EquipoVista[], equipo: EquipoVista): EquipoVista[] {
  return equipos.some((e) => e.idEquipo === equipo.idEquipo)
    ? equipos.map((e) => (e.idEquipo === equipo.idEquipo ? equipo : e))
    : [...equipos, equipo];
}
