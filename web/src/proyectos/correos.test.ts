import { describe, expect, it } from 'vitest';
import { mensajeResultadoMonitores, separarCorreos, validarLoteCorreos } from './correos';
import type { ResumenMonitores } from './equipos';

describe('separarCorreos', () => {
  it('separa por espacios, tabulaciones y saltos de línea (\\n y \\r\\n)', () => {
    const texto = 'ana@uninorte.edu.co \t beto@uninorte.edu.co\ncarlos@uninorte.edu.co\r\ndiana@uninorte.edu.co';
    expect(separarCorreos(texto)).toEqual([
      'ana@uninorte.edu.co',
      'beto@uninorte.edu.co',
      'carlos@uninorte.edu.co',
      'diana@uninorte.edu.co',
    ]);
  });

  it('separa por comas y punto y coma, ignorando separadores redundantes', () => {
    const texto = 'ana@uninorte.edu.co, beto@uninorte.edu.co; carlos@uninorte.edu.co,,;;diana@uninorte.edu.co';
    expect(separarCorreos(texto)).toEqual([
      'ana@uninorte.edu.co',
      'beto@uninorte.edu.co',
      'carlos@uninorte.edu.co',
      'diana@uninorte.edu.co',
    ]);
  });

  it('elimina duplicados con distinta mayúscula conservando el texto original del primero', () => {
    const texto = 'Ana@Uninorte.edu.co, ana@uninorte.edu.co, ANA@UNINORTE.EDU.CO, beto@uninorte.edu.co';
    expect(separarCorreos(texto)).toEqual(['Ana@Uninorte.edu.co', 'beto@uninorte.edu.co']);
  });

  it('devuelve arreglo vacío ante texto vacío o texto con solo espacios extremos y separadores', () => {
    expect(separarCorreos('')).toEqual([]);
    expect(separarCorreos('   \t  \r\n  ')).toEqual([]);
    expect(separarCorreos('  , ;  ; ,  ')).toEqual([]);
  });
});

describe('validarLoteCorreos', () => {
  it('texto vacío devuelve el error de correo obligatorio', () => {
    expect(validarLoteCorreos('')).toBe('Escribe un correo electrónico');
    expect(validarLoteCorreos('   \n\t  ')).toBe('Escribe un correo electrónico');
  });

  it('más de 50 correos devuelve el error de límite de lote', () => {
    const cincuentaYUno = Array.from({ length: 51 }, (_, i) => `estudiante${i}@uninorte.edu.co`).join('\n');
    expect(validarLoteCorreos(cincuentaYUno)).toBe('Máximo 50 correos por vez.');

    const cincuenta = Array.from({ length: 50 }, (_, i) => `estudiante${i}@uninorte.edu.co`).join('\n');
    expect(validarLoteCorreos(cincuenta)).toBeNull();
  });

  it('lista de inválidos con hasta 5 correos los muestra todos', () => {
    const texto = 'invalido1, ok@uninorte.edu.co, invalido2';
    expect(validarLoteCorreos(texto)).toBe('Correos inválidos: invalido1, invalido2');
  });

  it('lista de inválidos con más de 5 correos muestra los primeros 5 y «y N más»', () => {
    const invalidos = ['bad1', 'bad2', 'bad3', 'bad4', 'bad5', 'bad6', 'bad7'].join(' ');
    expect(validarLoteCorreos(invalidos)).toBe('Correos inválidos: bad1, bad2, bad3, bad4, bad5 y 2 más');
  });

  it('devuelve null si todos los correos son válidos', () => {
    expect(validarLoteCorreos('ana@uninorte.edu.co, beto@uninorte.edu.co')).toBeNull();
  });
});

describe('mensajes de resultado', () => {
  it('resumen de monitores genera formato con asignados y habilitados', () => {
    const mixto: ResumenMonitores = {
      asignados: ['mon1@u.edu.co', 'mon2@u.edu.co'],
      habilitados: ['mon3@u.edu.co'],
      yaHabilitados: ['mon4@u.edu.co'],
      conOtroRol: ['otro@u.edu.co'],
    };
    expect(mensajeResultadoMonitores(mixto)).toEqual({
      tipo: 'aviso',
      texto:
        '2 agregados como monitores · 1 habilitado (aparecerá cuando cree su cuenta) · 1 ya estaba habilitado · 1 con otro rol: otro@u.edu.co',
    });
  });
});
