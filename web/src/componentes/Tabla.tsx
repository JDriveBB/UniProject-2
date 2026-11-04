import type { ReactNode } from 'react';

export interface ColumnaTabla<T> {
  titulo: string;
  celda: (fila: T) => ReactNode;
}

interface PropsTabla<T> {
  /** Lo leen los lectores de pantalla (caption oculto a la vista). */
  titulo: string;
  columnas: ColumnaTabla<T>[];
  filas: T[];
  claveFila: (fila: T) => string | number;
  /** Texto cuando no hay filas. */
  vacio: string;
}

/** Tabla del sistema de diseño (P04): encabezado gris de 12 px, filas de 14 px, borde y radio de tabla. */
export function Tabla<T>({ titulo, columnas, filas, claveFila, vacio }: PropsTabla<T>) {
  return (
    <div className="tabla-contenedor">
      <table className="tabla">
        <caption className="solo-lectores">{titulo}</caption>
        <thead>
          <tr>
            {columnas.map((c) => (
              <th key={c.titulo} scope="col">
                {c.titulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.length === 0 ? (
            <tr>
              <td className="tabla__vacia" colSpan={columnas.length}>
                {vacio}
              </td>
            </tr>
          ) : (
            filas.map((f) => (
              <tr key={claveFila(f)}>
                {columnas.map((c) => (
                  <td key={c.titulo}>{c.celda(f)}</td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
