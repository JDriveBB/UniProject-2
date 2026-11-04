import type { ReactNode } from 'react';

export function Tarjeta({ titulo, acciones, children }: { titulo?: string; acciones?: ReactNode; children: ReactNode }) {
  return (
    <section className="tarjeta">
      {(titulo !== undefined || acciones !== undefined) && (
        <header className="tarjeta__cabecera">
          {titulo !== undefined && <h2 className="tarjeta__titulo">{titulo}</h2>}
          {acciones}
        </header>
      )}
      {children}
    </section>
  );
}
