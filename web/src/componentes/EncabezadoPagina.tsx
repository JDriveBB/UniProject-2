import type { ReactNode } from 'react';

/** Título de 28 px de una página dentro del marco, con descripción y acciones opcionales a la derecha. */
export function EncabezadoPagina({ titulo, descripcion, children }: {
  titulo: string;
  descripcion?: string;
  children?: ReactNode;
}) {
  return (
    <header className="pagina__cabecera">
      <div>
        <h1 className="pagina__titulo">{titulo}</h1>
        {descripcion !== undefined && <p className="pagina__descripcion">{descripcion}</p>}
      </div>
      {children}
    </header>
  );
}
