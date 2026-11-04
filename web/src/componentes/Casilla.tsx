import { useId } from 'react';
import type { InputHTMLAttributes, ReactNode } from 'react';

interface PropsCasilla extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'children'> {
  children: ReactNode;
  error?: string;
}

/** Casilla de 18 px (decisión 26a-9): input real oculto a la vista y marca dibujada en CSS. */
export function Casilla({ children, error, id, className, ...resto }: PropsCasilla) {
  const generado = useId();
  const idCasilla = id ?? generado;
  const idError = idCasilla + '-error';
  return (
    <div className={['casilla', className].filter(Boolean).join(' ')}>
      <label className="casilla__etiqueta" htmlFor={idCasilla}>
        <input
          {...resto}
          id={idCasilla}
          type="checkbox"
          className="casilla__entrada solo-lectores"
          aria-invalid={error !== undefined}
          aria-describedby={error !== undefined ? idError : undefined}
        />
        <span className="casilla__marca" aria-hidden="true" />
        <span className="casilla__texto">{children}</span>
      </label>
      {error !== undefined && (
        <p id={idError} className="campo__error">
          {error}
        </p>
      )}
    </div>
  );
}
