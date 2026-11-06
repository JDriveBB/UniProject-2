import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import {
  campoDelErrorContrasena,
  primerErrorContrasena,
  SUPRESION_VACIA,
  validarCambioContrasena,
  validarNombre,
  validarSupresion,
} from './formularios';

describe('validación de formularios del perfil', () => {
  it('validarNombre revisa vacíos y longitud máxima', () => {
    expect(validarNombre('')).toBe('Escribe tu nombre completo');
    expect(validarNombre('   ')).toBe('Escribe tu nombre completo');
    expect(validarNombre('a'.repeat(121))).toBe('El nombre no puede pasar de 120 caracteres');
    expect(validarNombre(' Ana ')).toBeUndefined();
    expect(validarNombre('a'.repeat(120))).toBeUndefined();
  });

  it('validarCambioContrasena revisa actual, reglas de nueva y coincidencia', () => {
    const err = validarCambioContrasena({ actual: '', nueva: 'corta', confirmacion: 'otra' });
    expect(err).toEqual({
      actual: 'Escribe tu contraseña actual',
      nueva: 'La contraseña debe tener al menos 10 caracteres',
      confirmacion: 'Las contraseñas no coinciden',
    });
    expect(primerErrorContrasena(err)).toBe('actual');

    const iguales = validarCambioContrasena({
      actual: 'ClaveVieja123',
      nueva: 'ClaveVieja123',
      confirmacion: 'ClaveVieja123',
    });
    expect(iguales).toEqual({
      nueva: 'La contraseña nueva debe ser distinta de la actual',
    });
    expect(primerErrorContrasena(iguales)).toBe('nueva');

    const larga = validarCambioContrasena({
      actual: 'ClaveVieja123',
      nueva: 'ñ'.repeat(37),
      confirmacion: 'ñ'.repeat(37),
    });
    expect(larga.nueva).toBe(
      'La contraseña es demasiado larga (máximo 72 bytes; las tildes y los emojis ocupan más de uno)',
    );

    const validas = validarCambioContrasena({
      actual: 'ClaveVieja123',
      nueva: 'ClaveNueva2026',
      confirmacion: 'ClaveNueva2026',
    });
    expect(validas).toEqual({});
    expect(primerErrorContrasena(validas)).toBeNull();
  });

  it('campoDelErrorContrasena asocia errores del API y validarSupresion revisa contraseña y casilla', () => {
    expect(campoDelErrorContrasena(new ErrorApi(400, 'CONTRASENA_INCORRECTA', 'x'))).toBe('actual');
    expect(campoDelErrorContrasena(new ErrorApi(400, 'CONTRASENA_INVALIDA', 'x'))).toBe('nueva');
    expect(campoDelErrorContrasena(new ErrorApi(400, 'CONTRASENA_REPETIDA', 'x'))).toBe('nueva');
    expect(campoDelErrorContrasena(new ErrorApi(429, 'DEMASIADAS_SOLICITUDES', 'x'))).toBeNull();
    expect(campoDelErrorContrasena(new Error('x'))).toBeNull();

    expect(validarSupresion(SUPRESION_VACIA)).toEqual({
      contrasena: 'Escribe tu contraseña',
      entiendo: 'Marca la casilla para confirmar',
    });
    expect(validarSupresion({ contrasena: 'x', entiendo: true })).toEqual({});
  });
});
