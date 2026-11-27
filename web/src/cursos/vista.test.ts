import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import type {
  CursoDetalle,
  CursoResumen,
  CursoVista,
  EstudianteProyectoVista,
  InscripcionVista,
} from '../api/tipos';
import {
  estadoProyectoEstudiante,
  etiquetaCurso,
  listaConY,
  MAXIMO_DESCRIPCION_CURSO,
  MAXIMO_NOMBRE_CURSO,
  MAXIMO_PERIODO,
  mensajeErrorCargaCurso,
  mismaSeleccion,
  opcionesCursos,
  preguntaArchivarCurso,
  preguntaBorrarCurso,
  preguntaQuitarDelCurso,
  proyectosDeMisCursos,
  repetidosDelProyecto,
  separarActivos,
  solicitudCurso,
  textoConfirmarCursos,
  textoEstudiantes,
  validarCurso,
} from './vista';

function curso(parcial: Partial<CursoVista> & { idCurso: number; nombre: string }): CursoVista {
  return {
    periodo: '2026-2',
    descripcion: null,
    archivado: false,
    nombreDocente: 'Prof. Ana',
    estudiantes: 0,
    proyectos: [],
    ...parcial,
  };
}

describe('vista de cursos', () => {
  it('validarCurso detecta textos vacíos y largos', () => {
    expect(validarCurso('  ', '  ', '')).toEqual({
      nombre: 'Escribe el nombre del curso',
      periodo: 'Escribe el periodo, por ejemplo 2026-2',
    });
    expect(
      validarCurso('a'.repeat(MAXIMO_NOMBRE_CURSO + 1), '2'.repeat(MAXIMO_PERIODO + 1), 'd'.repeat(MAXIMO_DESCRIPCION_CURSO + 1)),
    ).toEqual({
      nombre: `Máximo ${MAXIMO_NOMBRE_CURSO} caracteres`,
      periodo: `Máximo ${MAXIMO_PERIODO} caracteres`,
      descripcion: `Máximo ${MAXIMO_DESCRIPCION_CURSO} caracteres`,
    });
    expect(validarCurso(' Diseño I ', ' 2026-2 ', 'Descripción')).toEqual({});
  });

  it('solicitudCurso recorta textos y vuelve null la descripción en blanco', () => {
    expect(solicitudCurso(' Diseño I ', ' 2026-2 ', '   ')).toEqual({
      nombre: 'Diseño I',
      periodo: '2026-2',
      descripcion: null,
    });
    expect(solicitudCurso('Diseño I', '2026-2', ' Grupo A ')).toEqual({
      nombre: 'Diseño I',
      periodo: '2026-2',
      descripcion: 'Grupo A',
    });
  });

  it('separarActivos conserva el orden y textos de formato', () => {
    const a1 = curso({ idCurso: 1, nombre: 'A' });
    const ar = curso({ idCurso: 2, nombre: 'B', archivado: true });
    const a2 = curso({ idCurso: 3, nombre: 'C' });
    expect(separarActivos([a1, ar, a2])).toEqual({ activos: [a1, a2], archivados: [ar] });
    expect(textoEstudiantes(0)).toBe('Sin estudiantes');
    expect(textoEstudiantes(1)).toBe('1 estudiante');
    expect(textoEstudiantes(4)).toBe('4 estudiantes');
    expect(etiquetaCurso({ nombre: 'Diseño I', periodo: '2026-2' })).toBe('Diseño I (2026-2)');
  });

  it('listaConY formatea 0, 1, 2 y 3 elementos', () => {
    expect(listaConY([])).toBe('');
    expect(listaConY(['A'])).toBe('A');
    expect(listaConY(['A', 'B'])).toBe('A y B');
    expect(listaConY(['A', 'B', 'C'])).toBe('A, B y C');
  });

  it('proyectosDeMisCursos deduplica por id en orden de aparición', () => {
    const c1 = curso({
      idCurso: 1,
      nombre: 'C1',
      proyectos: [
        { idProyecto: 10, nombre: 'P1' },
        { idProyecto: 20, nombre: 'P2' },
      ],
    });
    const c2 = curso({
      idCurso: 2,
      nombre: 'C2',
      proyectos: [
        { idProyecto: 20, nombre: 'P2' },
        { idProyecto: 30, nombre: 'P3' },
      ],
    });
    expect(proyectosDeMisCursos([c1, c2])).toEqual([
      { idProyecto: 10, nombre: 'P1' },
      { idProyecto: 20, nombre: 'P2' },
      { idProyecto: 30, nombre: 'P3' },
    ]);
  });

  it('estadoProyectoEstudiante cubre los 5 estados', () => {
    expect(estadoProyectoEstudiante(undefined)).toEqual({ tipo: 'desconocido' });
    const base: InscripcionVista = {
      cierreEquipos: null,
      horasRonda: 48,
      abierta: false,
      idCursoQueCuenta: 1,
      idMiEquipo: null,
      equipos: [
        {
          idEquipo: 7,
          nombreEquipo: 'Alfa',
          idCurso: 1,
          nombreCurso: 'C1',
          cupo: 4,
          integrantes: 2,
          nombres: ['Ana', 'Luis'],
          puedeUnirse: false,
        },
      ],
    };
    expect(estadoProyectoEstudiante({ ...base, idMiEquipo: 7 })).toEqual({
      tipo: 'conEquipo',
      idEquipo: 7,
      nombreEquipo: 'Alfa',
    });
    expect(estadoProyectoEstudiante({ ...base, idMiEquipo: 99 })).toEqual({
      tipo: 'conEquipo',
      idEquipo: 99,
      nombreEquipo: 'tu equipo',
    });
    expect(estadoProyectoEstudiante({ ...base, abierta: true, cierreEquipos: '2026-09-10T23:59:00Z' })).toEqual({
      tipo: 'abierta',
      cierre: '2026-09-10T23:59:00Z',
    });
    expect(estadoProyectoEstudiante({ ...base, abierta: false, cierreEquipos: null })).toEqual({ tipo: 'sinFecha' });
    expect(estadoProyectoEstudiante({ ...base, abierta: false, cierreEquipos: '2026-09-01T23:59:00Z' })).toEqual({
      tipo: 'cerrada',
    });
  });

  it('opcionesCursos excluye archivados no asignados e incluye archivados asignados', () => {
    const mios = [
      curso({ idCurso: 1, nombre: 'Activo', archivado: false }),
      curso({ idCurso: 2, nombre: 'Archivado asignado', archivado: true }),
      curso({ idCurso: 3, nombre: 'Archivado libre', archivado: true }),
    ];
    const asignados: CursoResumen[] = [{ idCurso: 2, nombre: 'Archivado asignado', periodo: '2026-2', archivado: true }];
    expect(opcionesCursos(mios, asignados)).toEqual([
      { idCurso: 1, nombre: 'Activo', periodo: '2026-2', archivado: false },
      { idCurso: 2, nombre: 'Archivado asignado', periodo: '2026-2', archivado: true },
    ]);
  });

  it('mismaSeleccion compara conjuntos sin importar el orden', () => {
    expect(mismaSeleccion([1, 2], [2, 1])).toBe(true);
    expect(mismaSeleccion([1, 2], [1, 3])).toBe(false);
    expect(mismaSeleccion([], [])).toBe(true);
  });

  it('textoConfirmarCursos sin repetidos, con 1 y con 2', () => {
    const c1: CursoResumen = { idCurso: 1, nombre: 'A', periodo: '2026-2', archivado: false };
    const c2: CursoResumen = { idCurso: 2, nombre: 'B', periodo: '2026-2', archivado: false };
    expect(textoConfirmarCursos({ aplicado: false, cursos: [c1, c2], repetidos: [] })).toBe(
      'El proyecto quedará con 2 cursos: A (2026-2) y B (2026-2).',
    );
    expect(
      textoConfirmarCursos({
        aplicado: false,
        cursos: [c1, c2],
        repetidos: [{ idUsuario: 1, nombre: 'Ana', email: 'ana@u.co', cursos: [c1, c2], idCursoQueCuenta: 1 }],
      }),
    ).toBe(
      'El proyecto quedará con 2 cursos: A (2026-2) y B (2026-2). 1 estudiante está en más de uno de esos cursos y cuenta una sola vez. Puedes cambiar con qué curso cuenta después de confirmar.',
    );
    expect(
      textoConfirmarCursos({
        aplicado: false,
        cursos: [c1, c2],
        repetidos: [
          { idUsuario: 1, nombre: 'Ana', email: 'ana@u.co', cursos: [c1, c2], idCursoQueCuenta: 1 },
          { idUsuario: 2, nombre: 'Beto', email: 'beto@u.co', cursos: [c1, c2], idCursoQueCuenta: 2 },
        ],
      }),
    ).toBe(
      'El proyecto quedará con 2 cursos: A (2026-2) y B (2026-2). 2 estudiantes están en más de uno de esos cursos y cada uno cuenta una sola vez. Puedes cambiar con qué curso cuenta después de confirmar.',
    );
  });

  it('repetidosDelProyecto filtra repetidos participantes y ordena por nombre y correo', () => {
    const c1: CursoResumen = { idCurso: 1, nombre: 'A', periodo: '2026-2', archivado: false };
    const c2: CursoResumen = { idCurso: 2, nombre: 'B', periodo: '2026-2', archivado: false };
    const detalles: CursoDetalle[] = [
      {
        curso: curso({ idCurso: 1, nombre: 'A' }),
        estudiantes: [
          { idUsuario: 10, nombre: 'Luis', email: 'luis@u.co', fechaIngreso: '2026-08-01T00:00:00Z' },
          { idUsuario: 11, nombre: 'Ana', email: 'ana@u.co', fechaIngreso: '2026-08-01T00:00:00Z' },
          { idUsuario: 12, nombre: 'Solo Uno', email: 'uno@u.co', fechaIngreso: '2026-08-01T00:00:00Z' },
          { idUsuario: 13, nombre: 'No Participa', email: 'no@u.co', fechaIngreso: '2026-08-01T00:00:00Z' },
        ],
        pendientes: [],
      },
      {
        curso: curso({ idCurso: 2, nombre: 'B' }),
        estudiantes: [
          { idUsuario: 10, nombre: 'Luis', email: 'luis@u.co', fechaIngreso: '2026-08-01T00:00:00Z' },
          { idUsuario: 11, nombre: 'Ana', email: 'ana@u.co', fechaIngreso: '2026-08-01T00:00:00Z' },
          { idUsuario: 13, nombre: 'No Participa', email: 'no@u.co', fechaIngreso: '2026-08-01T00:00:00Z' },
        ],
        pendientes: [],
      },
    ];
    const estudiantes: EstudianteProyectoVista[] = [
      { idUsuario: 10, nombre: 'Luis', email: 'luis@u.co', idCurso: 2, nombreCurso: 'B', idEquipo: null },
      { idUsuario: 11, nombre: 'Ana', email: 'ana@u.co', idCurso: 1, nombreCurso: 'A', idEquipo: 3 },
      { idUsuario: 12, nombre: 'Solo Uno', email: 'uno@u.co', idCurso: 1, nombreCurso: 'A', idEquipo: null },
    ];
    expect(repetidosDelProyecto([c1, c2], detalles, estudiantes)).toEqual([
      { idUsuario: 11, nombre: 'Ana', email: 'ana@u.co', cursos: [c1, c2], idCursoQueCuenta: 1 },
      { idUsuario: 10, nombre: 'Luis', email: 'luis@u.co', cursos: [c1, c2], idCursoQueCuenta: 2 },
    ]);
  });

  it('mensajeErrorCargaCurso y preguntas de confirmación', () => {
    expect(mensajeErrorCargaCurso(new ErrorApi(403, 'ACCESO_DENEGADO', 'x'))).toBe(
      'No tienes acceso a este curso o no existe.',
    );
    expect(mensajeErrorCargaCurso(new ErrorApi(404, 'NO_ENCONTRADO', 'x'))).toBe('Este curso no existe.');
    expect(mensajeErrorCargaCurso(new ErrorApi(500, 'ERROR', 'Fallo'))).toBe('Fallo');
    expect(preguntaQuitarDelCurso('Ana')).toContain('¿Quitar a Ana del curso?');
    expect(preguntaArchivarCurso('Diseño I')).toContain('¿Archivar «Diseño I»?');
    expect(preguntaBorrarCurso('Diseño I')).toContain('¿Borrar «Diseño I»?');
  });
});
