import { ErrorApi, mensajeDeError } from '../api/cliente';
import type {
  CursoDetalle,
  CursoResumen,
  CursoVista,
  EstudianteProyectoVista,
  EstudianteRepetidoVista,
  InscripcionVista,
  ProyectoResumen,
  ResultadoCursosProyecto,
  SolicitudCurso,
} from '../api/tipos';
import type { Campos } from '../proyectos/errores';

export const CAMPOS_CURSO: Campos = [
  ['nombre', 'Nombre del curso'],
  ['periodo', 'Periodo'],
  ['descripcion', 'Descripción'],
];

export const MAXIMO_NOMBRE_CURSO = 150;
export const MAXIMO_PERIODO = 20;
export const MAXIMO_DESCRIPCION_CURSO = 2000;

export interface ErroresCurso {
  nombre?: string;
  periodo?: string;
  descripcion?: string;
}

export function validarCurso(nombre: string, periodo: string, descripcion: string): ErroresCurso {
  const errores: ErroresCurso = {};
  const n = nombre.trim();
  if (n === '') {
    errores.nombre = 'Escribe el nombre del curso';
  } else if (n.length > MAXIMO_NOMBRE_CURSO) {
    errores.nombre = `Máximo ${MAXIMO_NOMBRE_CURSO} caracteres`;
  }
  const p = periodo.trim();
  if (p === '') {
    errores.periodo = 'Escribe el periodo, por ejemplo 2026-2';
  } else if (p.length > MAXIMO_PERIODO) {
    errores.periodo = `Máximo ${MAXIMO_PERIODO} caracteres`;
  }
  const d = descripcion.trim();
  if (d.length > MAXIMO_DESCRIPCION_CURSO) {
    errores.descripcion = `Máximo ${MAXIMO_DESCRIPCION_CURSO} caracteres`;
  }
  return errores;
}

export function solicitudCurso(nombre: string, periodo: string, descripcion: string): SolicitudCurso {
  const d = descripcion.trim();
  return {
    nombre: nombre.trim(),
    periodo: periodo.trim(),
    descripcion: d === '' ? null : d,
  };
}

export function separarActivos(cursos: CursoVista[]): { activos: CursoVista[]; archivados: CursoVista[] } {
  const activos: CursoVista[] = [];
  const archivados: CursoVista[] = [];
  for (const c of cursos) {
    if (c.archivado) {
      archivados.push(c);
    } else {
      activos.push(c);
    }
  }
  return { activos, archivados };
}

export function textoEstudiantes(n: number): string {
  if (n === 0) {
    return 'Sin estudiantes';
  }
  return n === 1 ? '1 estudiante' : `${n} estudiantes`;
}

export function etiquetaCurso(c: { nombre: string; periodo: string }): string {
  return `${c.nombre} (${c.periodo})`;
}

export function listaConY(textos: string[]): string {
  if (textos.length === 0) {
    return '';
  }
  if (textos.length === 1) {
    return textos[0];
  }
  if (textos.length === 2) {
    return `${textos[0]} y ${textos[1]}`;
  }
  return `${textos.slice(0, -1).join(', ')} y ${textos[textos.length - 1]}`;
}

export function proyectosDeMisCursos(cursos: CursoVista[]): ProyectoResumen[] {
  const vistos = new Set<number>();
  const resultado: ProyectoResumen[] = [];
  for (const c of cursos) {
    for (const p of c.proyectos) {
      if (!vistos.has(p.idProyecto)) {
        vistos.add(p.idProyecto);
        resultado.push(p);
      }
    }
  }
  return resultado;
}

export type EstadoProyectoEstudiante =
  | { tipo: 'conEquipo'; idEquipo: number; nombreEquipo: string }
  | { tipo: 'abierta'; cierre: string }
  | { tipo: 'sinFecha' }
  | { tipo: 'cerrada' }
  | { tipo: 'desconocido' };

export function estadoProyectoEstudiante(v: InscripcionVista | undefined): EstadoProyectoEstudiante {
  if (v === undefined) {
    return { tipo: 'desconocido' };
  }
  if (v.idMiEquipo !== null) {
    const equipo = v.equipos.find((e) => e.idEquipo === v.idMiEquipo);
    return { tipo: 'conEquipo', idEquipo: v.idMiEquipo, nombreEquipo: equipo?.nombreEquipo ?? 'tu equipo' };
  }
  if (v.abierta && v.cierreEquipos !== null) {
    return { tipo: 'abierta', cierre: v.cierreEquipos };
  }
  if (v.cierreEquipos === null) {
    return { tipo: 'sinFecha' };
  }
  return { tipo: 'cerrada' };
}

export function opcionesCursos(mios: CursoVista[], asignados: CursoResumen[]): CursoResumen[] {
  const idsAsignados = new Set(asignados.map((c) => c.idCurso));
  return mios
    .filter((c) => !c.archivado || idsAsignados.has(c.idCurso))
    .map((c) => ({
      idCurso: c.idCurso,
      nombre: c.nombre,
      periodo: c.periodo,
      archivado: c.archivado,
    }));
}

export function mismaSeleccion(a: number[], b: number[]): boolean {
  const conjuntoA = new Set(a);
  const conjuntoB = new Set(b);
  if (conjuntoA.size !== conjuntoB.size) {
    return false;
  }
  for (const id of conjuntoA) {
    if (!conjuntoB.has(id)) {
      return false;
    }
  }
  return true;
}

export function textoConfirmarCursos(r: ResultadoCursosProyecto): string {
  const etiquetas = r.cursos.map(etiquetaCurso);
  let base: string;
  if (r.cursos.length === 0) {
    base = 'El proyecto quedará sin cursos.';
  } else if (r.cursos.length === 1) {
    base = `El proyecto quedará con 1 curso: ${listaConY(etiquetas)}.`;
  } else {
    base = `El proyecto quedará con ${r.cursos.length} cursos: ${listaConY(etiquetas)}.`;
  }
  if (r.repetidos.length === 0) {
    return base;
  }
  if (r.repetidos.length === 1) {
    return `${base} 1 estudiante está en más de uno de esos cursos y cuenta una sola vez. Puedes cambiar con qué curso cuenta después de confirmar.`;
  }
  return `${base} ${r.repetidos.length} estudiantes están en más de uno de esos cursos y cada uno cuenta una sola vez. Puedes cambiar con qué curso cuenta después de confirmar.`;
}

export function repetidosDelProyecto(
  asignados: CursoResumen[],
  detalles: CursoDetalle[],
  estudiantes: EstudianteProyectoVista[],
): EstudianteRepetidoVista[] {
  const detallesPorCurso = new Map<number, CursoDetalle>();
  for (const d of detalles) {
    detallesPorCurso.set(d.curso.idCurso, d);
  }
  const participantes = new Map<number, EstudianteProyectoVista>();
  for (const e of estudiantes) {
    participantes.set(e.idUsuario, e);
  }
  const cursosPorUsuario = new Map<number, CursoResumen[]>();
  for (const curso of asignados) {
    const detalle = detallesPorCurso.get(curso.idCurso);
    if (detalle === undefined) {
      continue;
    }
    for (const est of detalle.estudiantes) {
      if (!participantes.has(est.idUsuario)) {
        continue;
      }
      const lista = cursosPorUsuario.get(est.idUsuario) ?? [];
      lista.push(curso);
      cursosPorUsuario.set(est.idUsuario, lista);
    }
  }
  const resultado: EstudianteRepetidoVista[] = [];
  for (const [idUsuario, cursos] of cursosPorUsuario.entries()) {
    if (cursos.length < 2) {
      continue;
    }
    const p = participantes.get(idUsuario);
    if (p === undefined) {
      continue;
    }
    resultado.push({
      idUsuario,
      nombre: p.nombre,
      email: p.email,
      cursos,
      idCursoQueCuenta: p.idCurso,
    });
  }
  resultado.sort((a, b) => {
    const porNombre = a.nombre.localeCompare(b.nombre, 'es', { sensitivity: 'base' });
    if (porNombre !== 0) {
      return porNombre;
    }
    return a.email.localeCompare(b.email, 'es', { sensitivity: 'base' });
  });
  return resultado;
}

export function mensajeErrorCargaCurso(e: unknown): string {
  if (e instanceof ErrorApi && e.estado === 403) {
    return 'No tienes acceso a este curso o no existe.';
  }
  if (e instanceof ErrorApi && e.estado === 404) {
    return 'Este curso no existe.';
  }
  return mensajeDeError(e);
}

export function preguntaQuitarDelCurso(nombre: string): string {
  return `¿Quitar a ${nombre} del curso? Sale de los proyectos del curso donde no tiene equipo; si ya está en un equipo, sigue en él hasta que lo saques.`;
}

export function preguntaArchivarCurso(nombre: string): string {
  return `¿Archivar «${nombre}»? Ya no aparecerá al asignar cursos a proyectos. Los proyectos, las notas y las cuentas no cambian, y puedes desarchivarlo cuando quieras.`;
}

export function preguntaBorrarCurso(nombre: string): string {
  return `¿Borrar «${nombre}»? Se borran su lista de estudiantes y sus correos pendientes. Las cuentas de los estudiantes no se borran.`;
}
