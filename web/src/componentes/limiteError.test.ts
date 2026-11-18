import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { LimiteError } from './LimiteError';

describe('LimiteError', () => {
  it('pasa a estado de fallo cuando ocurre un error al renderizar', () => {
    expect(LimiteError.getDerivedStateFromError()).toEqual({ fallo: true });
  });

  it('muestra el contenido de los hijos cuando no hay fallo', () => {
    const html = renderToStaticMarkup(createElement(LimiteError, { children: createElement('p', null, 'contenido') }));
    expect(html).toContain('contenido');
  });

  it('muestra el aviso con acciones de reintentar e ir al inicio cuando hay fallo', () => {
    const instancia = new LimiteError({ children: null });
    instancia.state = { fallo: true };
    const html = renderToStaticMarkup(createElement(MemoryRouter, null, instancia.render()));
    expect(html).toContain('No se pudo mostrar esta pantalla');
    expect(html).toContain('Reintentar');
    expect(html).toContain('href="/"');
  });
});
