import { useId } from 'react';
import type { TextareaHTMLAttributes } from 'react';

interface PropsCampoArea extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  etiqueta: string;
  error?: string;
  ayuda?: string;
}

/** Texto de varias líneas con el estilo de Campo (descripción de P03: 80 px de alto). */
export function CampoArea({ etiqueta, error, ayuda, id, className, ...resto }: PropsCampoArea) {
  const generado = useId();
  const idCampo = id ?? generado;
  const idError = idCampo + '-error';
  const idAyuda = idCampo + '-ayuda';
  const descritos = [
    ayuda !== undefined ? idAyuda : null,
    error !== undefined ? idError : null,
  ].filter(Boolean).join(' ');

  return (
    <div className={['campo', className].filter(Boolean).join(' ')}>
      <label className="campo__etiqueta" htmlFor={idCampo}>
        {etiqueta}
      </label>
      <textarea
        {...resto}
        id={idCampo}
        className="campo__entrada campo__entrada--area"
        aria-invalid={error !== undefined}
        aria-describedby={descritos.length > 0 ? descritos : undefined}
      />
      {ayuda !== undefined && (
        <p id={idAyuda} className="texto-ayuda">
          {ayuda}
        </p>
      )}
      {error !== undefined && (
        <p id={idError} className="campo__error">
          {error}
        </p>
      )}
    </div>
  );
}
