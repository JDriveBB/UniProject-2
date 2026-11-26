import { describe, expect, it } from 'vitest';
import type { CursoResumen, EquipoVista, EstudianteDisponibleVista, MiembroVista } from '../api/tipos';
import {
  cupoDeTexto,
  cursoParaEquipo,
  equiposParaAsignar,
  estadoInscripcion,
  etiquetaEquipoAsignar,
  lleno,
  pasadoDeCupo,
  preguntaPasarCupo,
  preguntaRepartir,
  sinEquipoDelCurso,
  textoCupo,
  textoReparto,
  textoVistaPrevia,
  tieneLider,
  validarCierre,
  validarCupo,
  validarHorasRonda,
  vistaPreviaLote,
} from './cupos';

function miembros(n: number, lider = false): MiembroVista[] {
  return Array.from({ length: n }, (_, i) => ({
    idUsuario: i + 1,
    nombre: `M${i + 1}`,
    rolEquipo: lider && i === 0 ? ('LIDER' as const) : ('INTEGRANTE' as const),
  }));
}

function equipo(nombre: string, cupo: number | null, n: number, idCurso: number | null = null): EquipoVista {
  return { idEquipo: 1, idProyecto: 1, nombreEquipo: nombre, idCurso, cupo, miembros: miembros(n), hitos: [] };
}

describe('cupo de un equipo', () => {
  it('textoCupo, lleno y pasadoDeCupo', () => {
    expect(textoCupo(equipo('A', null, 2))).toBe('Lo arma el docente');
    expect(textoCupo(equipo('A', 3, 2))).toBe('2/3');
    expect(lleno(equipo('A', null, 9))).toBe(false);
    expect(lleno(equipo('A', 3, 2))).toBe(false);
    expect(lleno(equipo('A', 3, 3))).toBe(true);
    expect(pasadoDeCupo(equipo('A', 3, 3))).toBe(false);
    expect(pasadoDeCupo(equipo('A', 3, 4))).toBe(true);
    expect(pasadoDeCupo(equipo('A', null, 4))).toBe(false);
  });

  it('tieneLider', () => {
    expect(tieneLider({ miembros: miembros(2) })).toBe(false);
    expect(tieneLider({ miembros: miembros(2, true) })).toBe(true);
    expect(tieneLider({ miembros: [] })).toBe(false);
  });
});

describe('validarCupo y cupoDeTexto', () => {
  it('vacío depende de si es obligatorio', () => {
    expect(validarCupo('  ', false)).toBeNull();
    expect(validarCupo('', true)).toBe('Escribe el cupo');
  });

  it('entero de 1 a 100', () => {
    const msg = 'El cupo debe ser un número entero entre 1 y 100';
    expect(validarCupo('1', true)).toBeNull();
    expect(validarCupo(' 100 ', true)).toBeNull();
    expect(validarCupo('0', true)).toBe(msg);
    expect(validarCupo('101', false)).toBe(msg);
    expect(validarCupo('2,5', false)).toBe(msg);
    expect(validarCupo('-3', false)).toBe(msg);
    expect(validarCupo('1e1', false)).toBe(msg);
    expect(validarCupo('abc', false)).toBe(msg);
  });

  it('cupoDeTexto', () => {
    expect(cupoDeTexto('')).toBeNull();
    expect(cupoDeTexto(' 4 ')).toBe(4);
  });
});

describe('cursoParaEquipo y sinEquipoDelCurso', () => {
  const a: CursoResumen = { idCurso: 1, nombre: 'A', periodo: '2026-2', archivado: false };
  const b: CursoResumen = { idCurso: 2, nombre: 'B', periodo: '2026-2', archivado: false };

  it('según los cursos del proyecto', () => {
    expect(cursoParaEquipo([a, b], true)).toEqual({ tipo: 'mixto' });
    expect(cursoParaEquipo([], false)).toEqual({ tipo: 'sinCursos' });
    expect(cursoParaEquipo([a], false)).toEqual({ tipo: 'unico', curso: a });
    expect(cursoParaEquipo([a, b], false)).toEqual({ tipo: 'elegir', cursos: [a, b] });
  });

  it('cuenta los sin equipo de un curso, o todos con null', () => {
    const d = (idCurso: number): EstudianteDisponibleVista => ({
      email: `${idCurso}@u.co`,
      nombre: 'X',
      idCurso,
      nombreCurso: 'C',
    });
    const lista = [d(1), d(1), d(2)];
    expect(sinEquipoDelCurso(lista, 1)).toBe(2);
    expect(sinEquipoDelCurso(lista, 3)).toBe(0);
    expect(sinEquipoDelCurso(lista, null)).toBe(3);
  });
});

describe('vistaPreviaLote', () => {
  it('prefijo vacío', () => {
    expect(vistaPreviaLote('  ', '', '3', 7)).toEqual({ error: 'Escribe el prefijo de los nombres' });
  });

  it('cupo inválido', () => {
    expect(vistaPreviaLote('Equipo', '', '', 7)).toEqual({ error: 'Escribe el cupo' });
    expect(vistaPreviaLote('Equipo', '', '0', 7)).toEqual({
      error: 'El cupo debe ser un número entero entre 1 y 100',
    });
  });

  it('cantidad escrita entre 1 y 50', () => {
    expect(vistaPreviaLote('Equipo', '51', '3', 7)).toEqual({
      error: 'La cantidad debe ser un número entero entre 1 y 50',
    });
    expect(vistaPreviaLote('Equipo', '0', '3', 7)).toEqual({
      error: 'La cantidad debe ser un número entero entre 1 y 50',
    });
    expect(vistaPreviaLote('Equipo', '1,5', '3', 7)).toEqual({
      error: 'La cantidad debe ser un número entero entre 1 y 50',
    });
    expect(vistaPreviaLote(' Grupo ', '2', '3', 0)).toEqual({ cantidad: 2, nombres: ['Grupo 1', 'Grupo 2'] });
  });

  it('cantidad vacía: ceil(sinEquipo / cupo)', () => {
    expect(vistaPreviaLote('Equipo', '', '3', 7)).toEqual({
      cantidad: 3,
      nombres: ['Equipo 1', 'Equipo 2', 'Equipo 3'],
    });
  });

  it('cantidad vacía con más de 50 equipos o sin estudiantes', () => {
    expect(vistaPreviaLote('Equipo', '', '1', 51)).toEqual({
      error: 'Serían más de 50 equipos. Sube el cupo o escribe la cantidad.',
    });
    expect(vistaPreviaLote('Equipo', '', '1', 50)).toMatchObject({ cantidad: 50 });
    expect(vistaPreviaLote('Equipo', '', '3', 0)).toEqual({
      error: 'No hay estudiantes sin equipo para calcular cuántos equipos crear. Escribe la cantidad.',
    });
  });
});

describe('textoVistaPrevia', () => {
  const nombres = (n: number) => Array.from({ length: n }, (_, i) => `Equipo ${i + 1}`);

  it('hasta 4 equipos se listan todos', () => {
    expect(textoVistaPrevia({ cantidad: 1, nombres: nombres(1) })).toBe('Se creará 1 equipo: Equipo 1.');
    expect(textoVistaPrevia({ cantidad: 2, nombres: nombres(2) })).toBe(
      'Se crearán 2 equipos: Equipo 1 y Equipo 2.',
    );
    expect(textoVistaPrevia({ cantidad: 3, nombres: nombres(3) })).toBe(
      'Se crearán 3 equipos: Equipo 1, Equipo 2 y Equipo 3.',
    );
    expect(textoVistaPrevia({ cantidad: 4, nombres: nombres(4) })).toBe(
      'Se crearán 4 equipos: Equipo 1, Equipo 2, Equipo 3 y Equipo 4.',
    );
  });

  it('con más de 4 se abrevia', () => {
    expect(textoVistaPrevia({ cantidad: 8, nombres: nombres(8) })).toBe(
      'Se crearán 8 equipos: Equipo 1, Equipo 2 … Equipo 8.',
    );
  });
});

describe('asignación y reparto', () => {
  it('equiposParaAsignar filtra por curso y ordena por nombre', () => {
    const equipos = [
      equipo('Zeta', 3, 0, 1),
      equipo('Beta', null, 0, null),
      equipo('Alfa', 3, 0, 2),
      equipo('Épsilon', 3, 0, 1),
    ];
    expect(equiposParaAsignar(equipos, 1).map((e) => e.nombreEquipo)).toEqual(['Beta', 'Épsilon', 'Zeta']);
  });

  it('etiquetaEquipoAsignar', () => {
    expect(etiquetaEquipoAsignar(equipo('Alfa', 3, 1))).toBe('Alfa (1/3)');
    expect(etiquetaEquipoAsignar(equipo('Beta', null, 1))).toBe('Beta (Lo arma el docente)');
  });

  it('textoReparto', () => {
    const asignado = (n: number) => ({ idUsuario: n, nombre: `A${n}`, idEquipo: 1, nombreEquipo: 'E' });
    const sin = (n: string): EstudianteDisponibleVista => ({ email: `${n}@u.co`, nombre: n, idCurso: 1, nombreCurso: 'C' });
    expect(textoReparto({ asignados: [], sinCupo: [] })).toEqual({
      tipo: 'aviso',
      texto: 'No había estudiantes sin equipo.',
    });
    expect(textoReparto({ asignados: [asignado(1)], sinCupo: [] })).toEqual({
      tipo: 'exito',
      texto: 'Se asignó 1 estudiante.',
    });
    expect(textoReparto({ asignados: [asignado(1), asignado(2)], sinCupo: [sin('Luis')] })).toEqual({
      tipo: 'aviso',
      texto: 'Se asignaron 2 estudiantes. 1 quedó sin cupo: Luis.',
    });
    expect(textoReparto({ asignados: [], sinCupo: [sin('Ana'), sin('Luis'), sin('Eva')] })).toEqual({
      tipo: 'aviso',
      texto: '3 quedaron sin cupo: Ana, Luis y Eva.',
    });
  });
});

describe('estadoInscripcion', () => {
  it('sin fecha, abierta y cerrada', () => {
    expect(estadoInscripcion({ cierreEquipos: null, abierta: false })).toBe(
      'Sin fecha de cierre: los estudiantes aún no pueden inscribirse solos en los equipos con cupo.',
    );
    expect(estadoInscripcion({ cierreEquipos: '2026-11-30T22:00:00Z', abierta: true })).toBe(
      'Inscripción abierta hasta el 30/11/2026 17:00. Al cerrar, cada equipo abre su votación de líder.',
    );
    expect(estadoInscripcion({ cierreEquipos: '2026-11-30T22:00:00Z', abierta: false })).toBe(
      'La inscripción cerró el 30/11/2026 17:00.',
    );
  });
});

describe('validaciones de la inscripción', () => {
  it('validarHorasRonda', () => {
    const msg = 'Escribe un número entero de horas entre 1 y 336';
    expect(validarHorasRonda('1')).toBeNull();
    expect(validarHorasRonda('336')).toBeNull();
    expect(validarHorasRonda('0')).toBe(msg);
    expect(validarHorasRonda('337')).toBe(msg);
    expect(validarHorasRonda('')).toBe(msg);
    expect(validarHorasRonda('2.5')).toBe(msg);
  });

  it('validarCierre', () => {
    const ahora = new Date('2026-11-26T12:00:00Z');
    expect(validarCierre('', ahora)).toBeNull();
    expect(validarCierre('mañana', ahora)).toBe('Escribe una fecha y hora válidas');
    expect(validarCierre('2026-11-26T07:00', ahora)).toBe('La fecha de cierre debe ser futura');
    expect(validarCierre('2026-11-26T07:01', ahora)).toBeNull();
  });
});

describe('preguntas', () => {
  it('preguntaPasarCupo', () => {
    expect(preguntaPasarCupo('Equipo Alfa', ['Ana', 'Luis'])).toBe(
      'Equipo Alfa ya tiene su cupo completo. ¿Agregar igual a Ana y Luis? El equipo quedará pasado de cupo.',
    );
  });

  it('preguntaRepartir', () => {
    expect(preguntaRepartir(5)).toBe(
      'Se repartirán los 5 estudiantes sin equipo entre los equipos con cupo libre, respetando el curso y sin pasar ningún cupo. ¿Continuar?',
    );
    expect(preguntaRepartir(1)).toContain('el estudiante sin equipo');
  });
});
