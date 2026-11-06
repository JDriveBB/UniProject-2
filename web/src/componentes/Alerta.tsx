import type { ReactNode } from 'react';

type TipoAlerta = 'error' | 'exito' | 'aviso' | 'info';

export function Alerta({
  tipo,
  id,
  tabIndex,
  children,
}: {
  tipo: TipoAlerta;
  id?: string;
  tabIndex?: number;
  children: ReactNode;
}) {
  return (
    <div id={id} tabIndex={tabIndex} className={`alerta alerta--${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}
