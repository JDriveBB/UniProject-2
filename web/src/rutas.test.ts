import { describe, expect, it } from 'vitest';
import { rutas } from './rutas';

describe('rutas del SPA', () => {
  it('coinciden con RutasWeb.java (avisos de Telegram y bandeja)', () => {
    expect(rutas.tarea(5)).toBe('/tareas/5');
    expect(rutas.misTareas()).toBe('/mis-tareas');
    expect(rutas.tableroEquipo(3)).toBe('/equipos/3/tablero');
    expect(rutas.evaluarHito(7)).toBe('/hitos/7/evaluar');
    expect(rutas.evaluacionHito(7)).toBe('/hitos/7/evaluacion');
  });

  it('recuperar contraseña coincide con RutasWeb.restablecerContrasena sin el fragmento', () => {
    expect(rutas.restablecerContrasena()).toBe('/restablecer-contrasena');
    expect(rutas.olvideContrasena()).toBe('/olvide-contrasena');
  });
});
