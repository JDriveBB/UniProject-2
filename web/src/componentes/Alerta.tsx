import type { ReactNode } from 'react';

type TipoAlerta = 'error' | 'exito' | 'aviso' | 'info';

export function Alerta({ tipo, children }: { tipo: TipoAlerta; children: ReactNode }) {
  return (
    <div className={`alerta alerta--${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}
