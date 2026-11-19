import { describe, expect, it } from 'vitest';
import tema from './tema.css?raw';

const cssSinComentarios = tema.replace(/\/\*[\s\S]*?\*\//g, '');
const bloqueRoot = cssSinComentarios.match(/:root\s*\{([^}]+)\}/)?.[1] ?? '';

describe('tipografía e insignias en tema.css (T-006)', () => {
  it('tema.css se carga en la prueba', () => {
    expect(tema).toContain(':root');
    expect(tema.length).toBeGreaterThan(1000);
  });

  it('declara @font-face para Inter Variable con el archivo woff2 local', () => {
    const bloques = Array.from(cssSinComentarios.matchAll(/@font-face\s*\{([^}]+)\}/g), (m) => m[1]);
    const tieneInter = bloques.some(
      (b) =>
        /font-family:\s*'Inter Variable'\s*;/.test(b) && b.includes("url('../fuentes/inter-latin-wght-normal.woff2')"),
    );
    expect(tieneInter).toBe(true);
  });

  it("--fuente empieza por 'Inter Variable'", () => {
    const valorFuente = bloqueRoot.match(/--fuente:\s*([^;]+);/)?.[1]?.trim() ?? '';
    expect(valorFuente.startsWith("'Inter Variable'")).toBe(true);
  });

  it('.insignia tiene white-space: nowrap', () => {
    const bloqueInsignia = cssSinComentarios.match(/\.insignia\s*\{([^}]+)\}/)?.[1] ?? '';
    expect(bloqueInsignia).toMatch(/white-space:\s*nowrap\b/);
  });
});
