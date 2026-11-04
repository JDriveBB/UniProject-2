import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import {
  mensajeErrorHabilitar,
  mensajeErrorRegistro,
  solicitudDeRegistro,
  validarCorreoDocente,
  validarRegistro,
} from './validacion';
import type { DatosRegistro } from './validacion';

const validos: DatosRegistro = {
  nombre: 'Ana María Pérez',
  email: 'ana.perez@uninorte.edu.co',
  contrasena: 'clave-segura-1',
  confirmacion: 'clave-segura-1',
  autoriza: true,
};

describe('validación del registro', () => {
  it('datos válidos no tienen errores', () => {
    expect(validarRegistro(validos)).toEqual({});
  });

  it('campos vacíos o solo con espacios piden cada dato', () => {
    expect(validarRegistro({ nombre: '   ', email: '  ', contrasena: '', confirmacion: '', autoriza: false })).toEqual({
      nombre: 'Escribe tu nombre completo',
      email: 'Escribe un correo electrónico',
      contrasena: 'La contraseña debe tener al menos 10 caracteres',
      autoriza: 'Debes autorizar el tratamiento de tus datos para crear la cuenta',
    });
  });

  it('la contraseña sigue el mínimo real del núcleo (10) y no el de Figma (8)', () => {
    const nueve = 'a'.repeat(9);
    const diez = 'a'.repeat(10);
    expect(validarRegistro({ ...validos, contrasena: nueve, confirmacion: nueve }).contrasena).toBe(
      'La contraseña debe tener al menos 10 caracteres',
    );
    expect(validarRegistro({ ...validos, contrasena: diez, confirmacion: diez })).toEqual({});
  });

  it('la contraseña cuenta bytes UTF-8 hasta 72, como bcrypt', () => {
    const setentaYDos = 'a'.repeat(72);
    const conTildes = 'ñ'.repeat(37); // 37 caracteres, 74 bytes
    expect(validarRegistro({ ...validos, contrasena: setentaYDos, confirmacion: setentaYDos })).toEqual({});
    expect(validarRegistro({ ...validos, contrasena: conTildes, confirmacion: conTildes }).contrasena).toBe(
      'La contraseña es demasiado larga (máximo 72 bytes; las tildes y los emojis ocupan más de uno)',
    );
  });

  it('las contraseñas deben coincidir', () => {
    expect(validarRegistro({ ...validos, confirmacion: 'otra-clave-1' })).toEqual({
      confirmacion: 'Las contraseñas no coinciden',
    });
  });

  it('correo sin forma de correo y nombre largo', () => {
    expect(validarRegistro({ ...validos, email: 'ana.perez', nombre: 'x'.repeat(121) })).toEqual({
      nombre: 'El nombre no puede pasar de 120 caracteres',
      email: 'Escribe un correo válido, como nombre@uninorte.edu.co',
    });
  });

  it('la solicitud recorta nombre y correo y no toca la contraseña', () => {
    expect(
      solicitudDeRegistro({
        ...validos,
        nombre: '  Ana María Pérez ',
        email: ' Ana.Perez@uninorte.edu.co ',
        contrasena: ' clave con espacios ',
      }),
    ).toEqual({
      nombre: 'Ana María Pérez',
      email: 'Ana.Perez@uninorte.edu.co',
      contrasena: ' clave con espacios ',
      autorizaTratamientoDatos: true,
    });
  });
});

describe('errores de la API en registro y alta de docentes', () => {
  it('los 409 del registro muestran el mensaje del núcleo', () => {
    const noHabilitado = new ErrorApi(
      409,
      'CORREO_NO_HABILITADO',
      'Ese correo no está habilitado. Pide a tu docente que lo agregue al proyecto',
    );
    expect(mensajeErrorRegistro(noHabilitado)).toBe(
      'Ese correo no está habilitado. Pide a tu docente que lo agregue al proyecto',
    );
    const existente = new ErrorApi(409, 'CUENTA_EXISTENTE', 'Ya existe una cuenta con ese correo');
    expect(mensajeErrorRegistro(existente)).toBe('Ya existe una cuenta con ese correo');
  });

  it('VALIDACION nombra los campos en el orden del formulario, sin repetir', () => {
    const error = new ErrorApi(400, 'VALIDACION', 'Datos inválidos', [
      'contrasena: size must be between 10 and 72',
      'email: must be a well-formed email address',
      'email: must not be blank',
    ]);
    expect(mensajeErrorRegistro(error)).toBe('Revisa estos campos: Correo electrónico, Contraseña');
  });

  it('VALIDACION sin campos conocidos y los demás errores usan el texto general', () => {
    expect(mensajeErrorRegistro(new ErrorApi(400, 'VALIDACION', 'Datos inválidos', ['otro: x']))).toBe(
      'Datos inválidos',
    );
    expect(
      mensajeErrorRegistro(
        new ErrorApi(400, 'CONTRASENA_INVALIDA', 'La contraseña debe tener al menos 10 caracteres y no más de 72 bytes'),
      ),
    ).toBe('La contraseña debe tener al menos 10 caracteres y no más de 72 bytes');
    expect(
      mensajeErrorRegistro(
        new ErrorApi(0, 'SIN_CONEXION', 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo'),
      ),
    ).toBe('No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo');
    expect(mensajeErrorRegistro(new Error('x'))).toBe('Ocurrió un error inesperado. Recarga la página');
  });

  it('alta de docentes: correo del cliente, 409 YA_HABILITADO y 400 VALIDACION', () => {
    expect(validarCorreoDocente('  ')).toBe('Escribe un correo electrónico');
    expect(validarCorreoDocente(' docente@uninorte.edu.co ')).toBeNull();
    expect(
      mensajeErrorHabilitar(new ErrorApi(409, 'YA_HABILITADO', 'Ese correo ya está habilitado o ya tiene cuenta')),
    ).toBe('Ese correo ya está habilitado o ya tiene cuenta');
    expect(
      mensajeErrorHabilitar(
        new ErrorApi(400, 'VALIDACION', 'Datos inválidos', ['email: must be a well-formed email address']),
      ),
    ).toBe('Escribe un correo válido, como nombre@uninorte.edu.co');
  });
});
