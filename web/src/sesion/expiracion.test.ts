import { describe, expect, it } from 'vitest';
import type { UsuarioSesion } from '../api/tipos';
import { avisoSesionVisible, BOTON_SESION_EXPIRADA, TEXTO_SESION_EXPIRADA } from './expiracion';

const usuario = { idUsuario: 1, nombre: 'Ana', rol: 'DOCENTE' } as unknown as UsuarioSesion;

describe('aviso de sesión vencida', () => {
  it('es visible solo si la sesión venció y sigue autenticada', () => {
    expect(avisoSesionVisible({ estado: 'autenticado', usuario }, true)).toBe(true);
    expect(avisoSesionVisible({ estado: 'autenticado', usuario }, false)).toBe(false);
  });

  it('no es visible sin sesión autenticada y los textos son exactos', () => {
    expect(avisoSesionVisible({ estado: 'anonimo' }, true)).toBe(false);
    expect(avisoSesionVisible({ estado: 'cargando' }, true)).toBe(false);
    expect(TEXTO_SESION_EXPIRADA).toBe('Tu sesión terminó. Copia lo que no hayas guardado y vuelve a ingresar.');
    expect(BOTON_SESION_EXPIRADA).toBe('Ingresar de nuevo');
  });
});
