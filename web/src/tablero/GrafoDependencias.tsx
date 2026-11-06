import { GEOMETRIA } from './disposicion';
import type { Disposicion } from './disposicion';

/**
 * Red de P06 (decisión 26c-6) en SVG a mano, sin librerías (25-4). El SVG es una imagen para los lectores de
 * pantalla; la lista oculta que sigue da los mismos datos en texto. Los ids de las puntas de flecha son fijos: hay un
 * solo grafo por página.
 */
export function GrafoDependencias({ red, codigos }: { red: Disposicion; codigos: Map<number, string> }) {
  const g = GEOMETRIA;
  const titulos = new Map(red.nodos.map((n) => [n.idTarea, n.tituloCompleto] as const));
  const previas = new Map<number, string[]>();
  for (const a of red.aristas) {
    const lista = previas.get(a.hasta) ?? [];
    lista.push(titulos.get(a.desde) ?? '');
    previas.set(a.hasta, lista);
  }
  const criticas = red.nodos.filter((n) => n.critica).length;
  return (
    <div className="lienzo">
      <svg
        className="grafo"
        width={red.ancho}
        height={red.alto}
        viewBox={`0 0 ${red.ancho} ${red.alto}`}
        role="img"
        aria-label={`Grafo de dependencias: ${red.nodos.length} tareas, ${criticas} en la ruta crítica. El detalle está en la lista que sigue.`}
      >
        <defs>
          <marker
            id="flecha"
            viewBox="0 0 10 10"
            refX={10}
            refY={5}
            markerWidth={8}
            markerHeight={8}
            markerUnits="userSpaceOnUse"
            orient="auto"
          >
            <path className="grafo__punta" d="M 0 0 L 10 5 L 0 10 z" />
          </marker>
          <marker
            id="flecha-critica"
            viewBox="0 0 10 10"
            refX={10}
            refY={5}
            markerWidth={10}
            markerHeight={10}
            markerUnits="userSpaceOnUse"
            orient="auto"
          >
            <path className="grafo__punta grafo__punta--critica" d="M 0 0 L 10 5 L 0 10 z" />
          </marker>
        </defs>
        {red.aristas.map((a) => (
          <path
            key={`${a.desde}-${a.hasta}`}
            className={a.critica ? 'grafo__arista grafo__arista--critica' : 'grafo__arista'}
            d={a.d}
            markerEnd={a.critica ? 'url(#flecha-critica)' : 'url(#flecha)'}
          />
        ))}
        {red.nodos.map((n) => (
          <g key={n.idTarea} className={n.critica ? 'grafo__nodo grafo__nodo--critico' : 'grafo__nodo'}>
            <title>{n.tituloCompleto}</title>
            <rect className="grafo__caja" x={n.x} y={n.y} width={g.anchoNodo} height={g.altoNodo} rx={g.radioNodo} />
            <text className="grafo__titulo" x={n.x + g.rellenoNodo} y={n.y + g.baseTitulo}>
              {n.titulo}
            </text>
            <text className="grafo__detalle" x={n.x + g.rellenoNodo} y={n.y + g.baseDetalle}>
              {n.detalle}
            </text>
          </g>
        ))}
      </svg>
      <ul className="solo-lectores">
        {red.nodos.map((n) => (
          <li key={n.idTarea}>
            {`${codigos.get(n.idTarea) ?? ''} ${n.tituloCompleto}: ${n.detalle}${n.critica ? ', en la ruta crítica' : ''}. Depende de: ${(previas.get(n.idTarea) ?? []).join(', ') || 'ninguna'}.`}
          </li>
        ))}
      </ul>
    </div>
  );
}
