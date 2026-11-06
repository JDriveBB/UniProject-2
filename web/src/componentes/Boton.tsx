import type { ButtonHTMLAttributes } from 'react';

type Variante = 'primario' | 'secundario' | 'peligro' | 'suave';

interface PropsBoton extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante;
  cargando?: boolean;
}

export function Boton({ variante = 'primario', cargando = false, disabled, className, children, type = 'button',
                        ...resto }: PropsBoton) {
  const clases = ['boton', `boton--${variante}`, className].filter(Boolean).join(' ');
  return (
    <button {...resto} type={type} className={clases} disabled={disabled === true || cargando} aria-busy={cargando}>
      {cargando ? 'Procesando…' : children}
    </button>
  );
}
