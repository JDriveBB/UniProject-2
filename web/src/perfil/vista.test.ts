import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import {
  CAMPOS_NOMBRE,
  esEnlaceTelegram,
  mensajeErrorPerfil,
  puedeEliminarse,
  textoAutorizacion,
  textoVence,
} from './vista';

describe('funciones de presentación de perfil', () => {
  it('esEnlaceTelegram valida enlaces seguros a t.me y textoVence formatea en zona horaria local', () => {
    expect(esEnlaceTelegram('https:' + '//t.me/UniProjectBot?start=abc')).toBe(true);
    expect(esEnlaceTelegram('http:' + '//t.me/x')).toBe(false);
    expect(esEnlaceTelegram('https:' + '//t.me.otro.example/x')).toBe(false);
    expect(esEnlaceTelegram('https:' + '//otro.example/t.me')).toBe(false);
    expect(esEnlaceTelegram('javascript:alert(1)')).toBe(false);
    expect(esEnlaceTelegram('')).toBe(false);

    expect(textoVence('2026-09-23T19:05:00Z')).toBe('El código vence el 23/09/2026 14:05.');
  });

  it('textoAutorizacion, puedeEliminarse y mensajeErrorPerfil', () => {
    expect(
      textoAutorizacion({ autorizaTratamientoDatos: true, fechaAutorizacion: '2026-09-01T13:00:00Z' }),
    ).toBe('Autorizada el 01/09/2026 08:00');
    expect(textoAutorizacion({ autorizaTratamientoDatos: true, fechaAutorizacion: null })).toBe('Autorizada');
    expect(textoAutorizacion({ autorizaTratamientoDatos: false, fechaAutorizacion: null })).toBe('No autorizada');

    expect(puedeEliminarse('ADMINISTRADOR')).toBe(false);
    expect(puedeEliminarse('DOCENTE')).toBe(true);
    expect(puedeEliminarse('ESTUDIANTE')).toBe(true);
    expect(puedeEliminarse('MONITOR')).toBe(true);

    expect(mensajeErrorPerfil(new ErrorApi(403, 'ACCESO_DENEGADO', 'x'), CAMPOS_NOMBRE)).toBe(
      'No puedes hacer este cambio en tu cuenta.',
    );
    expect(
      mensajeErrorPerfil(
        new ErrorApi(409, 'LIDER_DE_EQUIPO_ACTIVO', 'Eres líder de un equipo activo'),
        CAMPOS_NOMBRE,
      ),
    ).toBe('Eres líder de un equipo activo');
    expect(
      mensajeErrorPerfil(
        new ErrorApi(400, 'VALIDACION', 'Error', ['nombre: no debe estar vacío']),
        CAMPOS_NOMBRE,
      ),
    ).toBe('Revisa estos campos: Nombre completo');
  });
});
