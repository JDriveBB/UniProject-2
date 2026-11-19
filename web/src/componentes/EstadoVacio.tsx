import type { ReactNode } from 'react';

export interface PropsEstadoVacio {
  titulo: string;
  children: ReactNode;
  accion?: ReactNode;
  nivel?: 2 | 3;
}

/** Estado vacío centrado con título, explicación y una acción opcional para resolverlo. */
export function EstadoVacio({ titulo, children, accion, nivel = 2 }: PropsEstadoVacio) {
  const Encabezado = nivel === 3 ? 'h3' : 'h2';
  return (
    <div className="estado-vacio">
      <Encabezado className="estado-vacio__titulo">{titulo}</Encabezado>
      <p className="estado-vacio__texto">{children}</p>
      {accion !== undefined && accion !== null && <div className="estado-vacio__accion">{accion}</div>}
    </div>
  );
}
