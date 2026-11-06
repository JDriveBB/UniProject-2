import { describe, expect, it } from 'vitest';
import { destinoSeguro, vieneDeRegistro, vieneDeRestablecer } from './destino';

describe('destino tras ingresar', () => {
  it('acepta rutas internas con parámetros', () => {
    expect(destinoSeguro({ desde: '/tareas/5' })).toBe('/tareas/5');
    expect(destinoSeguro({ desde: '/proyectos/3/panel?pestana=equipos' })).toBe('/proyectos/3/panel?pestana=equipos');
  });

  it('rechaza rutas que el navegador abre en otro dominio', () => {
    expect(destinoSeguro({ desde: '//evil.example' })).toBe('/');
    expect(destinoSeguro({ desde: '/\\evil.example' })).toBe('/');
    expect(destinoSeguro({ desde: '/\\/evil.example' })).toBe('/');
  });

  it('rechaza barras invertidas y caracteres de control en cualquier punto', () => {
    expect(destinoSeguro({ desde: '/tareas\\..\\x' })).toBe('/');
    expect(destinoSeguro({ desde: '/\t/evil.example' })).toBe('/');
    expect(destinoSeguro({ desde: '/\n/evil.example' })).toBe('/');
    expect(destinoSeguro({ desde: '/tareas/5\u0000' })).toBe('/');
  });

  it('sin una ruta interna válida vuelve al inicio', () => {
    expect(destinoSeguro(undefined)).toBe('/');
    expect(destinoSeguro(null)).toBe('/');
    expect(destinoSeguro('/tareas/5')).toBe('/');
    expect(destinoSeguro({ desde: 5 })).toBe('/');
    expect(destinoSeguro({ desde: '' })).toBe('/');
    expect(destinoSeguro({ desde: 'javascript:alert(1)' })).toBe('/');
    expect(destinoSeguro({ desde: 'tareas/5' })).toBe('/');
  });

  it('reconoce el aviso de cuenta creada solo con true', () => {
    expect(vieneDeRegistro({ cuentaCreada: true })).toBe(true);
    expect(vieneDeRegistro({ cuentaCreada: 'true' })).toBe(false);
    expect(vieneDeRegistro({ desde: '/' })).toBe(false);
    expect(vieneDeRegistro(null)).toBe(false);
  });

  it('reconoce el aviso de contraseña restablecida solo con true', () => {
    expect(vieneDeRestablecer({ contrasenaRestablecida: true })).toBe(true);
    expect(vieneDeRestablecer({ contrasenaRestablecida: 'true' })).toBe(false);
    expect(vieneDeRestablecer({ cuentaCreada: true })).toBe(false);
    expect(vieneDeRestablecer(null)).toBe(false);
    expect(vieneDeRestablecer(undefined)).toBe(false);
  });
});
