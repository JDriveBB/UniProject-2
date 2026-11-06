import { useEffect, useId, useRef } from 'react';
import type { KeyboardEvent } from 'react';

/**
 * Confirmación en la propia pantalla (decisión 26b-15): sin window.confirm. El foco entra en "Cancelar" (la opción
 * segura) y Escape cancela. Quien la muestra devuelve el foco al cerrarla.
 */
export function ConfirmarQuitar({ pregunta, ocupado, alConfirmar, alCancelar }: {
  pregunta: string;
  ocupado: boolean;
  alConfirmar: () => void;
  alCancelar: () => void;
}) {
  const idPregunta = useId();
  const cancelar = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    cancelar.current?.focus();
  }, []);

  function alTeclear(evento: KeyboardEvent<HTMLDivElement>) {
    if (evento.key === 'Escape' && !ocupado) {
      evento.preventDefault();
      alCancelar();
    }
  }

  return (
    <div className="confirmacion" role="group" aria-labelledby={idPregunta} onKeyDown={alTeclear}>
      <p id={idPregunta} className="confirmacion__texto">
        {pregunta}
      </p>
      <button type="button" className="boton-texto boton-texto--peligro" disabled={ocupado} onClick={alConfirmar}>
        {ocupado ? 'Quitando…' : 'Sí, quitar'}
      </button>
      <button ref={cancelar} type="button" className="boton-texto" disabled={ocupado} onClick={alCancelar}>
        Cancelar
      </button>
    </div>
  );
}
