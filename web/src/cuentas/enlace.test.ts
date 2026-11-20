import { describe, expect, it } from 'vitest';
import { ErrorApi } from '../api/cliente';
import {
  codigoDeLaUrl,
  copiarAlPortapapeles,
  enlaceDeRegistro,
  prepararEnlaceInvitacion,
  textoConfirmacionEnlace,
} from './enlace';

const C43 = 'A'.repeat(20) + '-_' + 'b'.repeat(21);

describe('enlace de invitación para registro', () => {
  it('arma la URL de registro usando URLSearchParams', () => {
    expect(enlaceDeRegistro('http://localhost:8080', C43)).toBe(`http://localhost:8080/registro?codigo=${C43}`);
    expect(enlaceDeRegistro('https://uniproject.edu.co/', C43)).toBe(
      `https://uniproject.edu.co/registro?codigo=${C43}`,
    );
    expect(enlaceDeRegistro('http://localhost:8080', 'a+b/c=')).toBe(
      'http://localhost:8080/registro?codigo=a%2Bb%2Fc%3D',
    );
  });

  it('extrae el código válido de la query de la URL o devuelve null', () => {
    expect(codigoDeLaUrl(`?codigo=${C43}`)).toBe(C43);
    expect(codigoDeLaUrl(`codigo=${C43}`)).toBe(C43);
    expect(codigoDeLaUrl(`?utm=1&codigo=${C43}&x=2`)).toBe(C43);
    expect(codigoDeLaUrl(`?codigo=%20${C43}%20`)).toBe(C43);
    expect(codigoDeLaUrl('')).toBeNull();
    expect(codigoDeLaUrl('?')).toBeNull();
    expect(codigoDeLaUrl('?codigo=')).toBeNull();
    expect(codigoDeLaUrl(`?otro=${C43}`)).toBeNull();
    expect(codigoDeLaUrl(`?codigo=${C43}x`)).toBeNull();
    expect(codigoDeLaUrl(`?codigo=${C43.slice(1)}`)).toBeNull();
    expect(codigoDeLaUrl(`?codigo=${C43.slice(1)}.`)).toBeNull();
  });

  it('formatea el texto de confirmación en hora de Bogotá según si copió o no', () => {
    expect(textoConfirmacionEnlace('ines@demo.uniproject.test', '2026-12-20T04:30:00Z', true)).toBe(
      'Enlace copiado. Vence el 19/12/2026. Envíalo solo a ines@demo.uniproject.test: sirve una vez y el anterior deja de servir.',
    );
    expect(textoConfirmacionEnlace('ines@demo.uniproject.test', '2026-12-20T05:00:00Z', false)).toBe(
      'Copia el enlace de abajo. Vence el 20/12/2026. Envíalo solo a ines@demo.uniproject.test: sirve una vez y el anterior deja de servir.',
    );
  });

  it('copiarAlPortapapeles devuelve true al copiar y false si falta o falla', async () => {
    let escrito = '';
    expect(
      await copiarAlPortapapeles('https://ejemplo/registro?codigo=1', {
        async writeText(texto) {
          escrito = texto;
        },
      }),
    ).toBe(true);
    expect(escrito).toBe('https://ejemplo/registro?codigo=1');

    expect(await copiarAlPortapapeles('x', undefined)).toBe(false);
    expect(
      await copiarAlPortapapeles('x', {
        async writeText() {
          throw new Error('Permiso denegado');
        },
      }),
    ).toBe(false);
  });

  it('prepararEnlaceInvitacion devuelve confirmación o marca recargar en 404/409', async () => {
    const okCopiado = await prepararEnlaceInvitacion(
      async () => ({ codigo: C43, venceEn: '2026-12-20T15:00:00Z' }),
      'ana@uninorte.edu.co',
      'http://localhost:8080',
      () => 'error',
      async () => true,
    );
    expect(okCopiado).toEqual({
      ok: true,
      enlace: {
        email: 'ana@uninorte.edu.co',
        url: `http://localhost:8080/registro?codigo=${C43}`,
        texto:
          'Enlace copiado. Vence el 20/12/2026. Envíalo solo a ana@uninorte.edu.co: sirve una vez y el anterior deja de servir.',
      },
    });

    const okSinPortapapeles = await prepararEnlaceInvitacion(
      async () => ({ codigo: C43, venceEn: '2026-12-20T15:00:00Z' }),
      'ana@uninorte.edu.co',
      'http://localhost:8080',
      () => 'error',
      async () => false,
    );
    expect(okSinPortapapeles).toEqual({
      ok: true,
      enlace: {
        email: 'ana@uninorte.edu.co',
        url: `http://localhost:8080/registro?codigo=${C43}`,
        texto:
          'Copia el enlace de abajo. Vence el 20/12/2026. Envíalo solo a ana@uninorte.edu.co: sirve una vez y el anterior deja de servir.',
      },
    });

    const err409 = await prepararEnlaceInvitacion(
      async () => {
        throw new ErrorApi(409, 'HABILITACION_USADA', 'Ya tiene cuenta');
      },
      'ana@uninorte.edu.co',
      'http://localhost:8080',
      (e) => (e instanceof ErrorApi ? e.message : 'otro'),
    );
    expect(err409).toEqual({ ok: false, mensaje: 'Ya tiene cuenta', recargar: true });

    const err404 = await prepararEnlaceInvitacion(
      async () => {
        throw new ErrorApi(404, 'NO_ENCONTRADO', 'No existe');
      },
      'ana@uninorte.edu.co',
      'http://localhost:8080',
      (e) => (e instanceof ErrorApi ? e.message : 'otro'),
    );
    expect(err404).toEqual({ ok: false, mensaje: 'No existe', recargar: true });

    const err403 = await prepararEnlaceInvitacion(
      async () => {
        throw new ErrorApi(403, 'ACCESO_DENEGADO', 'Sin permiso');
      },
      'ana@uninorte.edu.co',
      'http://localhost:8080',
      () => 'Solo el docente del proyecto puede cambiarlo',
    );
    expect(err403).toEqual({ ok: false, mensaje: 'Solo el docente del proyecto puede cambiarlo', recargar: false });
  });
});
