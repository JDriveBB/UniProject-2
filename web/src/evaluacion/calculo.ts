import type { CriterioVista } from '../api/tipos';
import { decimalEsCo } from '../formato';
import { centesimas, CIEN_POR_CIENTO } from '../proyectos/validacion';

/** Recorta, cambia la coma por punto y aplica centesimas (hasta 3 enteros y 2 decimales, sin signo). */
export function centesimasNota(texto: string): number | null {
  const normalizado = texto.trim().replace(',', '.');
  return centesimas(normalizado);
}

/** Siempre 2 decimales con coma: 4.2 → 4,20, 5 → 5,00. */
export function textoNota(valor: number): string {
  return valor.toFixed(2).replace('.', ',');
}

/** 4 decimales con coma (el ICE que guarda el núcleo): 1.25 → 1,2500. */
export function textoFactor(valor: number): string {
  return valor.toFixed(4).replace('.', ',');
}

/** Número de la API como se escribe en un campo: decimalEsCo (3.5 → 3,5, 4 → 4). */
export function textoCentesimas(valor: number): string {
  return decimalEsCo(valor);
}

/** true si hay al menos un criterio y la suma de Math.round(peso * 100) es exactamente 10000. */
export function pesosCompletos(criterios: Pick<CriterioVista, 'pesoPorcentaje'>[]): boolean {
  if (criterios.length === 0) {
    return false;
  }
  const suma = criterios.reduce<number>((acc, c) => acc + Math.round(c.pesoPorcentaje * 100), 0);
  return suma === CIEN_POR_CIENTO;
}

/**
 * Fórmula R4 en centésimas: x = Σ n · p / m; resultado en centésimas = x / 20 redondeado hacia arriba en la mitad.
 * Devuelve null si !pesosCompletos(criterios) o si algún criterio no tiene nota válida o supera el puntaje máximo.
 */
export function notaCalculada(criterios: CriterioVista[], notas: Record<number, string>): number | null {
  if (!pesosCompletos(criterios)) {
    return null;
  }
  let x = 0;
  for (const c of criterios) {
    const texto = notas[c.idCriterio];
    if (texto === undefined) {
      return null;
    }
    const n = centesimasNota(texto);
    if (n === null) {
      return null;
    }
    const m = Math.round(c.puntajeMaximo * 100);
    if (n > m) {
      return null;
    }
    const p = Math.round(c.pesoPorcentaje * 100);
    x += (n * p) / m;
  }
  return Math.round(x / 20 + 1e-7) / 100;
}
