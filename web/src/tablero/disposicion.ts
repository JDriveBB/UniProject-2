import type { NodoRutaCritica } from '../api/tipos';
import { decimalEsCo } from '../formato';

/**
 * Geometría de Figma P06 (nodo 6:273) en px del SVG. Va aquí y no en tema.css porque los atributos de SVG no aceptan
 * variables de CSS (y style en línea está prohibido). altoNodo 70 = 14 + 20 + 4 + 18 + 14 (interpretación C5).
 */
export const GEOMETRIA = {
  margenX: 60,
  margenSuperior: 50,
  margenInferior: 60,
  pasoX: 320,
  pasoY: 120,
  anchoNodo: 240,
  altoNodo: 70,
  rellenoNodo: 14,
  radioNodo: 10,
  baseTitulo: 29,
  baseDetalle: 51,
  maxTitulo: 26,
} as const;

export interface NodoDispuesto {
  idTarea: number;
  x: number;
  y: number;
  nivel: number;
  critica: boolean;
  /** Recortado a GEOMETRIA.maxTitulo caracteres para que quepa en el nodo. */
  titulo: string;
  tituloCompleto: string;
  /** «16 h · holgura 0 h». */
  detalle: string;
}

export interface Arista {
  desde: number;
  hasta: number;
  /** Trazo SVG del borde derecho de la predecesora al borde izquierdo de la dependiente. */
  d: string;
  /** Entre dos tareas críticas encadenadas (la predecesora termina justo cuando empieza la dependiente). */
  critica: boolean;
}

export interface Disposicion {
  /** En el mismo orden que llegaron. */
  nodos: NodoDispuesto[];
  aristas: Arista[];
  ancho: number;
  alto: number;
  niveles: number;
}

export function recortar(texto: string, max: number): string {
  const limpio = texto.trim();
  return limpio.length <= max ? limpio : limpio.slice(0, max - 1).trimEnd() + '…';
}

export function detalleNodo(n: Pick<NodoRutaCritica, 'horasEstimadas' | 'holguraHoras'>): string {
  return `${decimalEsCo(n.horasEstimadas)} h · holgura ${decimalEsCo(n.holguraHoras)} h`;
}

function trazo(a: NodoDispuesto, b: NodoDispuesto): string {
  const g = GEOMETRIA;
  const x1 = a.x + g.anchoNodo;
  const y1 = a.y + g.altoNodo / 2;
  const x2 = b.x;
  const y2 = b.y + g.altoNodo / 2;
  if (y1 === y2) {
    return `M ${x1} ${y1} L ${x2} ${y2}`;
  }
  const xm = (x1 + x2) / 2;
  return `M ${x1} ${y1} C ${xm} ${y1}, ${xm} ${y2}, ${x2} ${y2}`;
}

/**
 * Decisión 26c-6 (RNF1): red de nodos de P06 en O(V + E); el CPM ya lo hizo el núcleo. Columna = nivel (0 sin
 * prerrequisitos; si no, 1 + el mayor nivel de sus prerrequisitos), apoyado en que el núcleo manda los nodos en orden
 * topológico. En cada columna los nodos van en el orden recibido, en filas alternas y centrados respecto a la columna
 * más alta: fila = (máximo de filas − k) + 2·i. Con los datos de Figma da exactamente sus coordenadas.
 * Un prerrequisito que no está en la lista, o que llega después de su dependiente, se ignora (ni nivel ni flecha).
 */
export function disponerRed(nodos: NodoRutaCritica[]): Disposicion {
  const g = GEOMETRIA;
  const nivelDe = new Map<number, number>();
  const columnas: NodoRutaCritica[][] = [];
  for (const n of nodos) {
    let nivel = 0;
    for (const p of n.prerrequisitos) {
      const np = nivelDe.get(p);
      if (np !== undefined && np + 1 > nivel) {
        nivel = np + 1;
      }
    }
    nivelDe.set(n.idTarea, nivel);
    if (columnas.length === nivel) {
      columnas.push([]);
    }
    columnas[nivel].push(n);
  }
  const niveles = columnas.length;
  if (niveles === 0) {
    return { nodos: [], aristas: [], ancho: 0, alto: 0, niveles: 0 };
  }
  const maxFilas = columnas.reduce((m, c) => Math.max(m, c.length), 0);
  const porId = new Map<number, NodoDispuesto>();
  columnas.forEach((columna, nivel) => {
    columna.forEach((n, i) => {
      const fila = maxFilas - columna.length + 2 * i;
      porId.set(n.idTarea, {
        idTarea: n.idTarea,
        x: g.margenX + nivel * g.pasoX,
        y: g.margenSuperior + fila * g.pasoY,
        nivel,
        critica: n.critica,
        titulo: recortar(n.titulo, g.maxTitulo),
        tituloCompleto: n.titulo,
        detalle: detalleNodo(n),
      });
    });
  });
  const originales = new Map(nodos.map((n) => [n.idTarea, n] as const));
  const aristas: Arista[] = [];
  for (const n of nodos) {
    const hasta = porId.get(n.idTarea);
    for (const p of n.prerrequisitos) {
      const desde = porId.get(p);
      const previo = originales.get(p);
      if (hasta === undefined || desde === undefined || previo === undefined || desde.nivel >= hasta.nivel) {
        continue;
      }
      aristas.push({
        desde: p,
        hasta: n.idTarea,
        d: trazo(desde, hasta),
        critica: previo.critica && n.critica && previo.finTempranoHoras === n.inicioTempranoHoras,
      });
    }
  }
  return {
    nodos: nodos.map((n) => porId.get(n.idTarea)).filter((d): d is NodoDispuesto => d !== undefined),
    aristas,
    ancho: 2 * g.margenX + (niveles - 1) * g.pasoX + g.anchoNodo,
    alto: g.margenSuperior + (2 * maxFilas - 2) * g.pasoY + g.altoNodo + g.margenInferior,
    niveles,
  };
}
