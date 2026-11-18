import { describe, expect, it } from 'vitest';
import { falloDeCreacion, idDeRuta, modoDeVista, vieneDeCreacion } from './vista';

describe('vista del proyecto', () => {
  it('edita solo el docente dueño; el resto ve en solo lectura', () => {
    const proyecto = { idDocente: 10 };
    expect(modoDeVista({ idUsuario: 10, rol: 'DOCENTE' }, proyecto)).toBe('edicion');
    expect(modoDeVista({ idUsuario: 11, rol: 'DOCENTE' }, proyecto)).toBe('lectura');
    expect(modoDeVista({ idUsuario: 10, rol: 'ESTUDIANTE' }, proyecto)).toBe('lectura');
    expect(modoDeVista({ idUsuario: 12, rol: 'MONITOR' }, proyecto)).toBe('lectura');
    expect(modoDeVista({ idUsuario: 10, rol: 'DOCENTE' }, null)).toBe('edicion');
    expect(modoDeVista({ idUsuario: 1, rol: 'ADMINISTRADOR' }, null)).toBe('lectura');
  });

  it('id de la ruta: solo enteros positivos seguros', () => {
    expect(idDeRuta('5')).toBe(5);
    expect(idDeRuta('123456')).toBe(123456);
    for (const malo of ['0', '-1', 'abc', '5x', '05', '', ' 5', '1.5', '9999999999999999']) {
      expect(idDeRuta(malo)).toBeNull();
    }
    expect(idDeRuta(undefined)).toBeNull();
  });

  it('aviso de proyecto creado solo con true', () => {
    expect(vieneDeCreacion({ proyectoCreado: true })).toBe(true);
    expect(vieneDeCreacion({ proyectoCreado: 'true' })).toBe(false);
    expect(vieneDeCreacion(null)).toBe(false);
  });
});

describe('falloDeCreacion', () => {
  it('devuelve el texto solo si es un string no vacío', () => {
    expect(falloDeCreacion({ proyectoCreado: true, falloCreacion: 'Error X' })).toBe('Error X');
    expect(falloDeCreacion({ falloCreacion: '' })).toBeNull();
    expect(falloDeCreacion({ falloCreacion: 3 })).toBeNull();
    expect(falloDeCreacion(null)).toBeNull();
    expect(falloDeCreacion('x')).toBeNull();
  });
});
