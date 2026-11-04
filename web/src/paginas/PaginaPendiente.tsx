import { useParams } from 'react-router';
import { Tarjeta } from '../componentes/Tarjeta';

/** Marcador de las pantallas que construyen los bloques 26a–26f. */
export function PaginaPendiente({ titulo, bloque }: { titulo: string; bloque: string }) {
  const parametros = useParams();
  const ids = Object.entries(parametros)
    .map(([clave, valor]) => `${clave} = ${valor ?? ''}`)
    .join(', ');
  return (
    <Tarjeta titulo={titulo}>
      <p>Pantalla en construcción (bloque {bloque}).</p>
      {ids !== '' && <p className="texto-secundario">{ids}</p>}
    </Tarjeta>
  );
}
