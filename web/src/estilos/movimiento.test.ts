import { describe, expect, it } from 'vitest';
import tema from './tema.css?raw';

const cssSinComentarios = tema.replace(/\/\*[\s\S]*?\*\//g, '');
const bloqueRoot = cssSinComentarios.match(/:root\s*\{([^}]+)\}/)?.[1] ?? '';

const variables = new Map<string, string>();
for (const coincidencia of bloqueRoot.matchAll(/--([a-z0-9-]+):\s*([^;]+);/g)) {
  variables.set(`--${coincidencia[1]}`, coincidencia[2].trim());
}

interface DeclaracionConContexto {
  propiedad: string;
  valor: string;
  enMovimientoPermitido: boolean;
}

/** Recorre declaraciones CSS sin comentarios registrando si están dentro de `@media (prefers-reduced-motion: no-preference)`. */
function recorrerDeclaraciones(css: string): DeclaracionConContexto[] {
  const resultado: DeclaracionConContexto[] = [];
  const pilaMovimiento: boolean[] = [];
  let inicioSegmento = 0;
  let comilla: string | null = null;

  for (let i = 0; i < css.length; i += 1) {
    const ch = css[i];
    if (comilla !== null) {
      if (ch === '\\') {
        i += 1;
      } else if (ch === comilla) {
        comilla = null;
      }
      continue;
    }
    if (ch === "'" || ch === '"') {
      comilla = ch;
      continue;
    }

    if (ch === '{') {
      const preludio = css.slice(inicioSegmento, i).replace(/\s+/g, ' ').trim();
      const esMediaPermitido = /@media\s*\(\s*prefers-reduced-motion:\s*no-preference\s*\)/.test(preludio);
      const heredado = pilaMovimiento.at(-1) ?? false;
      pilaMovimiento.push(heredado || esMediaPermitido);
      inicioSegmento = i + 1;
    } else if (ch === ';' || ch === '}') {
      const trozo = css.slice(inicioSegmento, i).trim();
      const dosPuntos = trozo.indexOf(':');
      if (dosPuntos > 0 && !trozo.startsWith('@')) {
        resultado.push({
          propiedad: trozo.slice(0, dosPuntos).trim().toLowerCase(),
          valor: trozo.slice(dosPuntos + 1).trim(),
          enMovimientoPermitido: pilaMovimiento.at(-1) ?? false,
        });
      }
      if (ch === '}') {
        pilaMovimiento.pop();
      }
      inicioSegmento = i + 1;
    }
  }

  return resultado;
}

const declaraciones = recorrerDeclaraciones(cssSinComentarios);

describe('radios, estados y movimiento en tema.css (T-015)', () => {
  it.each([
    ['--duracion-presion', '80ms'],
    ['--duracion-rapida', '120ms'],
    ['--duracion-media', '180ms'],
    ['--curva-salida', 'cubic-bezier(0.2, 0, 0, 1)'],
    ['--desplazamiento-entrada', '8px'],
    ['--escala-presion', '0.98'],
    ['--radio', '10px'],
    ['--radio-tabla', '12px'],
    ['--radio-tarjeta', '14px'],
    ['--radio-modal', '16px'],
    ['--radio-avance', '999px'],
  ] as const)('define %s con el valor %s en :root', (nombre, esperado) => {
    expect(variables.get(nombre)).toBe(esperado);
  });

  it('define reglas :hover para botones secundario y de peligro', () => {
    expect(cssSinComentarios).toMatch(/\.boton--secundario:hover(?::not\(:disabled\))?\s*\{/);
    expect(cssSinComentarios).toMatch(/\.boton--peligro:hover(?::not\(:disabled\))?\s*\{/);
  });

  it("excluye [aria-invalid='true'] y [readonly] del :hover de .campo__entrada", () => {
    const selectoresHover = [...cssSinComentarios.matchAll(/\.campo__entrada:hover[^,{]*/g)].map((m) =>
      m[0].replace(/\s+/g, ' ').trim(),
    );
    expect(selectoresHover.length).toBeGreaterThan(0);
    for (const selector of selectoresHover) {
      expect(selector).toMatch(/:not\(\s*\[aria-invalid=['"]true['"]\]\s*\)/);
      expect(selector).toMatch(/:not\(\s*\[readonly\]\s*\)/);
    }
  });

  it('usa @starting-style para animar la entrada del diálogo', () => {
    expect(cssSinComentarios).toMatch(/@starting-style\s*\{/);
  });

  it('limita scale y translate a @media (prefers-reduced-motion: no-preference)', () => {
    const deMovimiento = declaraciones.filter((d) => d.propiedad === 'scale' || d.propiedad === 'translate');
    expect(deMovimiento.some((d) => d.propiedad === 'scale')).toBe(true);
    expect(deMovimiento.some((d) => d.propiedad === 'translate')).toBe(true);
    for (const declaracion of deMovimiento) {
      expect(declaracion.enMovimientoPermitido).toBe(true);
    }
  });

  it('no usa duraciones literales en ms o s dentro de transition', () => {
    const transiciones = declaraciones.filter(
      (d) => d.propiedad === 'transition' || d.propiedad === 'transition-duration',
    );
    expect(transiciones.length).toBeGreaterThan(0);
    for (const { valor } of transiciones) {
      expect(valor).toContain('var(--');
      expect(valor).not.toMatch(/\b\d+(?:\.\d+)?\s*m?s\b/i);
      expect(valor).not.toMatch(/(?:^|[\s,])\.\d+\s*m?s\b/i);
    }
  });
});
