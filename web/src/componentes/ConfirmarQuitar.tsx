import { useEffect, useId, useRef } from 'react';
import type { KeyboardEvent } from 'react';

/**
 * Confirmación en la propia pantalla (decisión 26b-15): sin window.confirm. El foco entra en "Cancelar" (la opción
 * segura) y Escape cancela. Quien la muestra devuelve el foco al cerrarla. 26c: también confirma «Enviar a revisión»
 * (textos propios en textoConfirmar y textoOcupado).
 */
export function ConfirmarQuitar({ pregunta, ocupado, alConfirmar, alCancelar, textoConfirmar = 'Sí, quitar',
                                  textoOcupado = 'Quitando…' }: {
  pregunta: string;
  ocupado: boolean;
  alConfirmar: () => void;
  alCancelar: () => void;
  textoConfirmar?: string;
  textoOcupado?: string;
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
        {ocupado ? textoOcupado : textoConfirmar}
      </button>
      <button ref={cancelar} type="button" className="boton-texto" disabled={ocupado} onClick={alCancelar}>
        Cancelar
      </button>
    </div>
  );
}
