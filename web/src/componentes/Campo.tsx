import { useId } from 'react';
import type { InputHTMLAttributes } from 'react';

interface PropsCampo extends InputHTMLAttributes<HTMLInputElement> {
  etiqueta: string;
  error?: string;
}

export function Campo({ etiqueta, error, id, className, ...resto }: PropsCampo) {
  const generado = useId();
  const idCampo = id ?? generado;
  const idError = idCampo + '-error';
  return (
    <div className={['campo', className].filter(Boolean).join(' ')}>
      <label className="campo__etiqueta" htmlFor={idCampo}>
        {etiqueta}
      </label>
      <input
        {...resto}
        id={idCampo}
        className="campo__entrada"
        aria-invalid={error !== undefined}
        aria-describedby={error !== undefined ? idError : undefined}
      />
      {error !== undefined && (
        <p id={idError} className="campo__error">
          {error}
        </p>
      )}
    </div>
  );
}
