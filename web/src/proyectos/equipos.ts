import { ErrorApi } from '../api/cliente';
import type {
  EquipoVista,
  EstudianteDisponibleVista,
  EstudianteProyectoVista,
  ResultadoHabilitacion,
  SolicitudCorreos,
} from '../api/tipos';
import { CAMPOS_CORREO, mensajeErrorApi } from './errores';
import { separarCorreos } from './correos';

/** Las operaciones del API que usan estas acciones (en las pruebas, una versión falsa). */
export interface ApiEquipos {
  habilitarCorreos(idProyecto: number, solicitud: SolicitudCorreos): Promise<ResultadoHabilitacion>;
  agregarMiembro(idEquipo: number, email: string): Promise<EquipoVista>;
  invitarAlEquipo(idEquipo: number, email: string): Promise<EstudianteProyectoVista>;
}

export interface ErrorAlAgregar {
  email: string;
  motivo: string;
}

export interface ResumenEstudiantes {
  agregados: string[];
  /** Sin cuenta: quedaron invitados y entran al equipo cuando se registran (D-12). */
  invitados: string[];
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

export const OTRO_ROL_ESTUDIANTE = 'Ese correo ya tiene una cuenta que no es de estudiante.';

function esNoRegistrado(e: unknown): boolean {
  return e instanceof ErrorApi && e.estado === 409 && e.codigo === 'ESTUDIANTE_NO_REGISTRADO';
}

/**
 * "+ Agregar estudiante" en lote (bloque 31, D-12): habilita todos los correos en UNA llamada
 * habilitarCorreos(idProyecto, { emails, rol: 'ESTUDIANTE' }); a los que tienen cuenta de estudiante (y a los ya
 * habilitados, por si la tienen) los agrega al equipo con agregarMiembro uno a uno; a los que no tienen cuenta
 * (o responden ESTUDIANTE_NO_REGISTRADO) los invita al equipo con invitarAlEquipo: entran al registrarse.
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
      invitados: [],
      conOtroRol: [],
      errores: [],
    };
  }

  const hab = await api.habilitarCorreos(idProyecto, { emails: lista, rol: 'ESTUDIANTE' });
  const porAgregar = [...hab.habilitados.filter((c) => c.registrado).map((c) => c.email), ...hab.yaHabilitados];
  const porInvitar = hab.habilitados.filter((c) => !c.registrado).map((c) => c.email);

  const agregados: string[] = [];
  const invitados: string[] = [];
  const errores: ErrorAlAgregar[] = [];
  let ultimoEquipo: EquipoVista | undefined;

  for (const correo of porAgregar) {
    try {
      ultimoEquipo = await api.agregarMiembro(idEquipo, correo);
      agregados.push(correo);
    } catch (e) {
      if (esNoRegistrado(e)) {
        porInvitar.push(correo);
      } else {
        errores.push({ email: correo, motivo: mensajeErrorApi(e, CAMPOS_CORREO) });
      }
    }
  }

  for (const correo of porInvitar) {
    try {
      await api.invitarAlEquipo(idEquipo, correo);
      invitados.push(correo);
    } catch (e) {
      errores.push({ email: correo, motivo: mensajeErrorApi(e, CAMPOS_CORREO) });
    }
  }

  return {
    agregados,
    invitados,
    conOtroRol: hab.conOtroRol,
    errores,
    equipo: ultimoEquipo,
  };
}

/**
 * Sugerencias del campo de fichas (D-11, T-046): estudiantes habilitados sin equipo ni invitación más las cuentas
 * de estudiante sin equipo en el proyecto, sin repetir correos y ordenados por nombre (o correo si falta).
 */
export function sugerenciasDe(
  estudiantes: EstudianteProyectoVista[],
  disponibles: EstudianteDisponibleVista[] = [],
): Pick<EstudianteProyectoVista, 'email' | 'nombre' | 'registrado'>[] {
  const vistos = new Set<string>();
  const resultado: Pick<EstudianteProyectoVista, 'email' | 'nombre' | 'registrado'>[] = [];
  for (const e of estudiantes) {
    vistos.add(e.email.toLowerCase());
    if (e.idEquipo === null && e.idEquipoInvitado === null) {
      resultado.push({ email: e.email, nombre: e.nombre, registrado: e.registrado });
    }
  }
  for (const d of disponibles) {
    const clave = d.email.toLowerCase();
    if (!vistos.has(clave)) {
      vistos.add(clave);
      resultado.push({ email: d.email, nombre: d.nombre, registrado: true });
    }
  }
  resultado.sort((a, b) => {
    const porNombre = (a.nombre ?? a.email).localeCompare(b.nombre ?? b.email, 'es', { sensitivity: 'base' });
    if (porNombre !== 0) {
      return porNombre;
    }
    return a.email.localeCompare(b.email, 'es', { sensitivity: 'base' });
  });
  return resultado;
}

/** Correos invitados a ese equipo que aún no crean su cuenta (D-12), en el orden de la lista. */
export function invitadosDe(estudiantes: EstudianteProyectoVista[], idEquipo: number): string[] {
  return estudiantes.filter((e) => e.idEquipoInvitado === idEquipo).map((e) => e.email);
}

/** Clave que cambia solo cuando cambian los integrantes de algún equipo (para recargar los estudiantes). */
export function claveIntegrantes(equipos: EquipoVista[]): string {
  return equipos.map((e) => `${e.idEquipo}:${e.miembros.map((m) => m.idUsuario).join(',')}`).join('|');
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
