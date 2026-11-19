import type { ReactNode } from 'react';

export interface ColumnaTabla<T> {
  titulo: string;
  celda: (fila: T) => ReactNode;
  /** Clase del encabezado (p. ej. el ancho de la columna con table-layout: fixed). */
  clase?: string;
  /** Celda principal de la fila (<th scope="row"> y cabecera en modo tarjeta). Por defecto, la primera. */
  principal?: boolean;
}

interface PropsTabla<T> {
  /** Lo leen los lectores de pantalla (caption oculto a la vista). */
  titulo: string;
  columnas: ColumnaTabla<T>[];
  filas: T[];
  claveFila: (fila: T) => string | number;
  /** Clase CSS opcional por fila (<tr>). */
  claseFila?: (fila: T) => string | undefined;
  /** Texto cuando no hay filas. */
  vacio: string;
  /** Clase extra de la tabla (p. ej. tabla--tareas). */
  clase?: string;
}

/** Tabla del sistema de diseño (P04): encabezado gris de 12 px, filas de 14 px, borde y radio de tabla. */
export function Tabla<T>({ titulo, columnas, filas, claveFila, claseFila, vacio, clase }: PropsTabla<T>) {
  const indicePrincipal = columnas.findIndex((c) => c.principal === true);
  const colPrincipal = indicePrincipal >= 0 ? indicePrincipal : 0;

  return (
    <div className="tabla-contenedor">
      <table className={['tabla', clase].filter(Boolean).join(' ')}>
        <caption className="solo-lectores">{titulo}</caption>
        <thead>
          <tr>
            {columnas.map((c) => (
              <th key={c.titulo} scope="col" className={c.clase}>
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
            filas.map((f) => {
              const extra = claseFila?.(f);
              return (
                <tr key={claveFila(f)} className={extra || undefined}>
                  {columnas.map((c, i) =>
                    i === colPrincipal ? (
                      <th key={c.titulo} scope="row">
                        {c.celda(f)}
                      </th>
                    ) : (
                      <td key={c.titulo} data-etiqueta={c.titulo}>
                        {c.celda(f)}
                      </td>
                    ),
                  )}
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}
