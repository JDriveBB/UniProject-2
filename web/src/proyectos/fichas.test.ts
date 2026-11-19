import { describe, expect, it } from 'vitest';
import type { ResumenEstudiantes, ResumenMonitores } from './equipos';
import type { Sugerencia } from './fichas';
import {
  agregarTexto,
  aplicarResultadoEstudiantes,
  aplicarResultadoMonitores,
  AVISO_LIMITE_CORREOS,
  filtrarSugerencias,
  quitar,
  TEXTO_HABILITADO_SIN_CUENTA,
  validas,
} from './fichas';

describe('agregarTexto, quitar y validas', () => {
  it('crea fichas al pegar una lista con comas, punto y coma, espacios y saltos de línea', () => {
    const { fichas, aviso } = agregarTexto(
      [],
      'ana@uninorte.edu.co, beto@uninorte.edu.co; carlos@uninorte.edu.co\ndiana@uninorte.edu.co\r\nelena@uninorte.edu.co',
    );
    expect(aviso).toBeNull();
    expect(fichas).toEqual([
      { correo: 'ana@uninorte.edu.co', error: null },
      { correo: 'beto@uninorte.edu.co', error: null },
      { correo: 'carlos@uninorte.edu.co', error: null },
      { correo: 'diana@uninorte.edu.co', error: null },
      { correo: 'elena@uninorte.edu.co', error: null },
    ]);
  });

  it('ignora repetidos sin distinguir mayúsculas dentro del texto y contra fichas existentes', () => {
    const inicial = agregarTexto([], 'Ana@Uninorte.edu.co').fichas;
    const { fichas, aviso } = agregarTexto(inicial, 'ana@uninorte.edu.co, BETO@uninorte.edu.co, beto@uninorte.edu.co');
    expect(aviso).toBeNull();
    expect(fichas).toEqual([
      { correo: 'Ana@Uninorte.edu.co', error: null },
      { correo: 'BETO@uninorte.edu.co', error: null },
    ]);
  });

  it('marca correos inválidos con el mensaje de validarCorreo y validas los excluye', () => {
    const { fichas } = agregarTexto([], 'ok@uninorte.edu.co, no-es-correo, ' + 'a'.repeat(250) + '@u.co');
    expect(fichas[0]).toEqual({ correo: 'ok@uninorte.edu.co', error: null });
    expect(fichas[1].error).toBe('Escribe un correo válido, como nombre@uninorte.edu.co');
    expect(fichas[2].error).toBe('El correo no puede pasar de 254 caracteres');
    expect(validas(fichas)).toEqual([{ correo: 'ok@uninorte.edu.co', error: null }]);
  });

  it('respeta el máximo de 50 correos, descarta el sobrante y devuelve aviso', () => {
    const cuarentaYOcho = Array.from({ length: 48 }, (_, i) => `est${i}@uninorte.edu.co`).join(' ');
    const base = agregarTexto([], cuarentaYOcho);
    expect(base.aviso).toBeNull();
    expect(base.fichas).toHaveLength(48);

    const excedido = agregarTexto(base.fichas, 'est0@uninorte.edu.co, a@u.co, b@u.co, c@u.co, d@u.co');
    expect(excedido.aviso).toBe(AVISO_LIMITE_CORREOS);
    expect(excedido.fichas).toHaveLength(50);
    expect(excedido.fichas.slice(48).map((f) => f.correo)).toEqual(['a@u.co', 'b@u.co']);

    const enTopeConRepetido = agregarTexto(excedido.fichas, 'a@u.co');
    expect(enTopeConRepetido.aviso).toBeNull();
    expect(enTopeConRepetido.fichas).toHaveLength(50);

    const enTopeConNuevo = agregarTexto(excedido.fichas, 'nuevo@u.co');
    expect(enTopeConNuevo.aviso).toBe(AVISO_LIMITE_CORREOS);
    expect(enTopeConNuevo.fichas).toHaveLength(50);
  });

  it('quitar elimina la ficha sin distinguir mayúsculas', () => {
    const { fichas } = agregarTexto([], 'Ana@Uninorte.edu.co, beto@uninorte.edu.co');
    expect(quitar(fichas, 'ana@uninorte.edu.co')).toEqual([{ correo: 'beto@uninorte.edu.co', error: null }]);
  });
});

describe('filtrarSugerencias', () => {
  const ines: Sugerencia = { email: 'ines@uninorte.edu.co', nombre: null, registrado: false };
  const ana: Sugerencia = { email: 'ana@uninorte.edu.co', nombre: 'Ana Pérez', registrado: true };
  const beto: Sugerencia = { email: 'beto@uninorte.edu.co', nombre: 'Beto Inés', registrado: true };

  it('sin texto devuelve todas las que no están en las fichas, sin distinguir mayúsculas', () => {
    const fichas = agregarTexto([], 'ANA@uninorte.edu.co').fichas;
    expect(filtrarSugerencias([ines, ana, beto], fichas, '')).toEqual([ines, beto]);
  });

  it('filtra por nombre o correo sin distinguir mayúsculas ni tildes', () => {
    expect(filtrarSugerencias([ines, ana, beto], [], 'INES')).toEqual([ines, beto]);
    expect(filtrarSugerencias([ines, ana, beto], [], ' perez ')).toEqual([ana]);
    expect(filtrarSugerencias([ines, ana, beto], [], 'beto@')).toEqual([beto]);
    expect(filtrarSugerencias([ines, ana, beto], [], 'nadie')).toEqual([]);
  });

  it('devuelve como máximo 6', () => {
    const muchas = Array.from({ length: 9 }, (_, i) => ({ email: `e${i}@u.co`, nombre: null, registrado: false }));
    expect(filtrarSugerencias(muchas, [], '')).toEqual(muchas.slice(0, 6));
  });
});

describe('aplicarResultadoEstudiantes y aplicarResultadoMonitores', () => {
  it('estudiantes: quita agregados e invitados, deja los demás con motivo y arma resumen corto', () => {
    const { fichas } = agregarTexto(
      [],
      'Ok1@uninorte.edu.co ok2@uninorte.edu.co inv@uninorte.edu.co ya@uninorte.edu.co doc@uninorte.edu.co err@uninorte.edu.co',
    );
    const resumen: ResumenEstudiantes = {
      agregados: ['ok1@uninorte.edu.co', 'ok2@uninorte.edu.co'],
      invitados: ['inv@uninorte.edu.co'],
      habilitados: [],
      yaHabilitados: ['ya@uninorte.edu.co'],
      conOtroRol: ['doc@uninorte.edu.co'],
      errores: [{ email: 'err@uninorte.edu.co', motivo: 'Ya pertenece a otro equipo' }],
    };

    const resultado = aplicarResultadoEstudiantes(fichas, resumen);
    expect(resultado.fichas).toEqual([
      { correo: 'ya@uninorte.edu.co', error: null, resultado: 'Ya estaba habilitado; aún no tiene cuenta' },
      { correo: 'doc@uninorte.edu.co', error: null, resultado: 'Tiene otro rol: no es estudiante' },
      { correo: 'err@uninorte.edu.co', error: null, resultado: 'Ya pertenece a otro equipo' },
    ]);
    expect(resultado.resumen).toBe(
      '2 agregados · 1 invitado: entrará al equipo cuando cree su cuenta · 3 con problemas',
    );
  });

  it('estudiantes: singular/plural y solo las partes con número', () => {
    const { fichas } = agregarTexto([], 'a@u.co b@u.co c@u.co');
    const resultado = aplicarResultadoEstudiantes(fichas, {
      agregados: ['a@u.co'],
      invitados: ['b@u.co', 'c@u.co'],
      habilitados: [],
      yaHabilitados: [],
      conOtroRol: [],
      errores: [],
    });
    expect(resultado.fichas).toEqual([]);
    expect(resultado.resumen).toBe('1 agregado · 2 invitados: entrarán al equipo cuando creen su cuenta');
    expect(TEXTO_HABILITADO_SIN_CUENTA.estudiantePlural).toBe('invitados: entrarán al equipo cuando creen su cuenta');
  });

  it('monitores: quita asignados y habilitados, deja los demás con motivo y arma resumen corto', () => {
    const { fichas } = agregarTexto(
      [],
      'mon1@uninorte.edu.co inv@uninorte.edu.co ya@uninorte.edu.co est@uninorte.edu.co',
    );
    const resumen: ResumenMonitores = {
      asignados: ['mon1@uninorte.edu.co'],
      habilitados: ['inv@uninorte.edu.co'],
      yaHabilitados: ['ya@uninorte.edu.co'],
      conOtroRol: ['est@uninorte.edu.co'],
    };

    const resultado = aplicarResultadoMonitores(fichas, resumen);
    expect(resultado.fichas).toEqual([
      { correo: 'ya@uninorte.edu.co', error: null, resultado: 'Ya estaba habilitado' },
      { correo: 'est@uninorte.edu.co', error: null, resultado: 'Tiene otro rol: no es monitor' },
    ]);
    expect(resultado.resumen).toBe(`1 agregado · 1 ${TEXTO_HABILITADO_SIN_CUENTA.monitorSingular} · 2 con problemas`);

    const soloUnoConProblema = aplicarResultadoMonitores(agregarTexto([], 'ya@u.co').fichas, {
      asignados: [],
      habilitados: [],
      yaHabilitados: ['ya@u.co'],
      conOtroRol: [],
    });
    expect(soloUnoConProblema.resumen).toBe('1 con problema');
  });
});
