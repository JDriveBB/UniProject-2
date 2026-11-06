import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import {
  CODIGO_NO_VALIDO,
  codigoDelFragmento,
  errorDeRestablecer,
  solicitudDeRestablecer,
  validarRestablecer,
} from './recuperacion';
import { mensajeErrorHabilitar } from './validacion';

const C43 = 'A'.repeat(20) + '-_' + 'b'.repeat(21);

describe('recuperación y restablecimiento de contraseña', () => {
  it('obtiene el código del fragmento de la URL', () => {
    expect(codigoDelFragmento('#codigo=' + C43)).toBe(C43);
    expect(codigoDelFragmento('codigo=' + C43)).toBe(C43);
    expect(codigoDelFragmento('#codigo=' + C43 + '&x=1')).toBe(C43);
    expect(codigoDelFragmento('#x=1&codigo=' + C43)).toBe(C43);
    expect(codigoDelFragmento('')).toBeNull();
    expect(codigoDelFragmento('#')).toBeNull();
    expect(codigoDelFragmento('#codigo=')).toBeNull();
    expect(codigoDelFragmento('#otro=' + C43)).toBeNull();
    expect(codigoDelFragmento('#codigo=' + C43 + 'x')).toBeNull();
    expect(codigoDelFragmento('#codigo=' + C43.slice(1))).toBeNull();
    expect(codigoDelFragmento('#codigo=' + C43.slice(1) + '.')).toBeNull();
  });

  it('valida datos de restablecimiento y arma la solicitud', () => {
    expect(
      validarRestablecer({ codigo: C43, contrasena: 'ClaveSegura26', confirmacion: 'ClaveSegura26' })
    ).toEqual({});
    expect(
      validarRestablecer({ codigo: '   ', contrasena: 'ClaveSegura26', confirmacion: 'ClaveSegura26' })
    ).toEqual({ codigo: 'Escribe el código que te llegó por Telegram' });
    expect(
      validarRestablecer({ codigo: C43.slice(1), contrasena: 'ClaveSegura26', confirmacion: 'ClaveSegura26' })
    ).toEqual({ codigo: 'Copia el código completo del mensaje de Telegram (43 caracteres)' });
    expect(
      validarRestablecer({ codigo: ' ' + C43 + ' ', contrasena: 'ClaveSegura26', confirmacion: 'ClaveSegura26' })
    ).toEqual({});
    expect(
      validarRestablecer({ codigo: C43, contrasena: 'Corta1234', confirmacion: 'Corta1234' })
    ).toEqual({ contrasena: 'La contraseña debe tener al menos 10 caracteres' });
    expect(
      validarRestablecer({ codigo: C43, contrasena: 'ñ'.repeat(37), confirmacion: 'ñ'.repeat(37) })
    ).toEqual({
      contrasena: 'La contraseña es demasiado larga (máximo 72 bytes; las tildes y los emojis ocupan más de uno)',
    });
    expect(
      validarRestablecer({ codigo: C43, contrasena: 'ClaveSegura26', confirmacion: 'OtraClave26' })
    ).toEqual({ confirmacion: 'Las contraseñas no coinciden' });
    expect(
      solicitudDeRestablecer({ codigo: '  ' + C43 + ' ', contrasena: ' Clave 2026 ', confirmacion: ' Clave 2026 ' })
    ).toEqual({ codigo: C43, contrasenaNueva: ' Clave 2026 ' });
  });

  it('transforma errores de la API para restablecer y recuperar', () => {
    expect(
      errorDeRestablecer(new ErrorApi(400, 'CODIGO_INVALIDO', 'El enlace no es válido o ya venció. Pide uno nuevo'))
    ).toEqual({ mensaje: 'El enlace no es válido o ya venció. Pide uno nuevo', pedirNuevo: true });
    expect(
      errorDeRestablecer(new ErrorApi(404, 'NO_ENCONTRADO', 'Lo que buscas no existe o fue eliminado'))
    ).toEqual({ mensaje: CODIGO_NO_VALIDO, pedirNuevo: true });
    expect(
      errorDeRestablecer(new ErrorApi(410, 'VENCIDO', 'x'))
    ).toEqual({ mensaje: CODIGO_NO_VALIDO, pedirNuevo: true });
    expect(
      errorDeRestablecer(new ErrorApi(400, 'CONTRASENA_INVALIDA', 'La contraseña no cumple las reglas', ['contrasenaNueva: x']))
    ).toEqual({ mensaje: 'La contraseña no cumple las reglas', pedirNuevo: false });
    expect(
      errorDeRestablecer(new ErrorApi(400, 'VALIDACION', 'Datos inválidos', ['codigo: size must be between 0 and 100']))
    ).toEqual({ mensaje: 'Revisa los datos e inténtalo de nuevo', pedirNuevo: false });
    expect(
      errorDeRestablecer(new ErrorApi(429, 'DEMASIADAS_SOLICITUDES', 'Demasiados intentos. Espera un rato y vuelve a intentarlo'))
    ).toEqual({ mensaje: 'Demasiados intentos. Espera un rato y vuelve a intentarlo', pedirNuevo: false });
    expect(
      errorDeRestablecer(new Error('x'))
    ).toEqual({ mensaje: 'Ocurrió un error inesperado. Recarga la página', pedirNuevo: false });

    const error429 = new ErrorApi(429, 'DEMASIADAS_SOLICITUDES', 'Demasiados intentos. Espera un rato y vuelve a intentarlo');
    expect(mensajeErrorHabilitar(error429)).toBe('Demasiados intentos. Espera un rato y vuelve a intentarlo');
    const error400Validacion = new ErrorApi(400, 'VALIDACION', 'Datos inválidos', ['email: x']);
    expect(mensajeErrorHabilitar(error400Validacion)).toBe('Escribe un correo válido, como nombre@uninorte.edu.co');
  });
});
