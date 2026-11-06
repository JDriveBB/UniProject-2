import type { ReactNode } from 'react';

/** 'critico' (26c): insignia «Prerrequisito crítico» de Figma P04 (--color-critico-fondo y --color-critico). */
export type TonoInsignia = 'neutro' | 'exito' | 'aviso' | 'peligro' | 'info' | 'critico';

export function Insignia({ tono = 'neutro', children }: { tono?: TonoInsignia; children: ReactNode }) {
  return <span className={`insignia insignia--${tono}`}>{children}</span>;
}

/** Riesgo de la tarea (12a/17a) → tono. */
export function tonoDeRiesgo(nivel: 'BAJO' | 'MEDIO' | 'ALTO' | null): TonoInsignia {
  switch (nivel) {
    case 'ALTO':
      return 'peligro';
    case 'MEDIO':
      return 'aviso';
    case 'BAJO':
      return 'exito';
    default:
      return 'neutro';
  }
}
