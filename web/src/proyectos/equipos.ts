import { ErrorApi } from '../api/cliente';
import type {
  EquipoVista,
  EstudianteDisponibleVista,
  ResultadoHabilitacion,
  SolicitudCorreos,
} from '../api/tipos';
import { separarCorreos } from './correos';
import { CAMPOS_CORREO, mensajeErrorApi } from './errores';
import type { Sugerencia } from './fichas';

/** Las operaciones del API que usan estas acciones (en las pruebas, una versión falsa). */
export interface ApiEquipos {
  habilitarCorreos(idProyecto: number, solicitud: SolicitudCorreos): Promise<ResultadoHabilitacion>;
  agregarMiembro(idEquipo: number, email: string, pasarCupo?: boolean): Promise<EquipoVista>;
}

export interface ErrorAlAgregar {
  email: string;
  motivo: string;
}

export interface ResumenEstudiantes {
  agregados: string[];
  cupoLleno: string[];
  errores: ErrorAlAgregar[];
  equipo?: EquipoVista;
}

export interface ResumenMonitores {
  asignados: string[];
  habilitados: string[];
  yaHabilitados: string[];
  conOtroRol: string[];
}

function esCupoLleno(e: unknown): boolean {
  return e instanceof ErrorApi && e.estado === 409 && e.codigo === 'CUPO_LLENO';
}

/**
 * "+ Agregar estudiante" en lote: llama agregarMiembro por cada correo en orden. Un 409 CUPO_LLENO con pasarCupo
 * false se separa en cupoLleno; cualquier otro error va a errores y no detiene los demás.
 */
export async function agregarEstudiantes(
  api: Pick<ApiEquipos, 'agregarMiembro'>,
  idEquipo: number,
  emails: string[] | string,
  pasarCupo = false,
): Promise<ResumenEstudiantes> {
  const lista = typeof emails === 'string' ? separarCorreos(emails) : emails;
  if (lista.length === 0) {
    return {
      agregados: [],
      cupoLleno: [],
      errores: [],
    };
  }

  const agregados: string[] = [];
  const cupoLleno: string[] = [];
  const errores: ErrorAlAgregar[] = [];
  let ultimoEquipo: EquipoVista | undefined;

  for (const correo of lista) {
    try {
      ultimoEquipo = await api.agregarMiembro(idEquipo, correo, pasarCupo);
      agregados.push(correo);
    } catch (e) {
      if (!pasarCupo && esCupoLleno(e)) {
        cupoLleno.push(correo);
      } else {
        errores.push({ email: correo, motivo: mensajeErrorApi(e, CAMPOS_CORREO) });
      }
    }
  }

  return {
    agregados,
    cupoLleno,
    errores,
    equipo: ultimoEquipo,
  };
}

/**
 * Sugerencias del campo de fichas: participantes sin equipo en el proyecto (si el equipo es de un solo curso,
 * solo los de ese curso), ordenados por nombre y luego correo.
 */
export function sugerenciasDe(
  disponibles: EstudianteDisponibleVista[],
  idCursoEquipo: number | null,
): Sugerencia[] {
  const filtrados =
    idCursoEquipo === null ? disponibles : disponibles.filter((d) => d.idCurso === idCursoEquipo);
  const resultado: Sugerencia[] = filtrados.map((d) => ({
    email: d.email,
    nombre: d.nombre,
    registrado: true,
  }));
  resultado.sort((a, b) => {
    const porNombre = (a.nombre ?? a.email).localeCompare(b.nombre ?? b.email, 'es', { sensitivity: 'base' });
    if (porNombre !== 0) {
      return porNombre;
    }
    return a.email.localeCompare(b.email, 'es', { sensitivity: 'base' });
  });
  return resultado;
}

/** Clave que cambia solo cuando cambian los integrantes de algún equipo (para recargar los disponibles). */
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
