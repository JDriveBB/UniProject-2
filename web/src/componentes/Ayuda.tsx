import { useEffect, useId, useLayoutEffect, useRef, useState, type ReactNode } from 'react';

/** Botón de revelar «?» accesible: muestra una explicación al pulsarlo y cierra con Escape o clic fuera. */
export function Ayuda({ termino, children }: { termino: string; children: ReactNode }) {
  const [abierto, setAbierto] = useState(false);
  const idPanel = useId();
  const contenedorRef = useRef<HTMLSpanElement>(null);
  const botonRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!abierto) {
      return;
    }

    function alPulsarFuera(evento: Event) {
      if (
        evento.target instanceof Node &&
        contenedorRef.current !== null &&
        !contenedorRef.current.contains(evento.target)
      ) {
        setAbierto(false);
      }
    }

    function alTecla(evento: KeyboardEvent) {
      if (evento.key === 'Escape') {
        if (document.querySelector('dialog[open]') !== null) {
          return;
        }
        evento.stopPropagation();
        setAbierto(false);
        botonRef.current?.focus();
      }
    }

    document.addEventListener('pointerdown', alPulsarFuera);
    document.addEventListener('click', alPulsarFuera);
    document.addEventListener('keydown', alTecla);
    return () => {
      document.removeEventListener('pointerdown', alPulsarFuera);
      document.removeEventListener('click', alPulsarFuera);
      document.removeEventListener('keydown', alTecla);
    };
  }, [abierto]);

  useLayoutEffect(() => {
    const panel = panelRef.current;
    const contenedor = contenedorRef.current;
    if (!abierto || panel === null || contenedor === null) {
      return;
    }

    function ajustarPosicion() {
      if (panel === null || contenedor === null) {
        return;
      }
      panel.style.setProperty('--ayuda-x', '0px');
      const rectContenedor = contenedor.getBoundingClientRect();
      const rectPanel = panel.getBoundingClientRect();
      const anchoViewport = document.documentElement.clientWidth;
      const margen = Math.min(16, Math.max(0, (anchoViewport - rectPanel.width) / 2));
      const preferidoIzquierda =
        rectContenedor.left + rectPanel.width <= anchoViewport - margen
          ? rectContenedor.left
          : rectContenedor.right - rectPanel.width;
      const xClamped = Math.max(margen, Math.min(preferidoIzquierda, anchoViewport - margen - rectPanel.width));
      panel.style.setProperty('--ayuda-x', `${Math.round(xClamped - rectContenedor.left)}px`);
    }

    ajustarPosicion();
    window.addEventListener('resize', ajustarPosicion);
    return () => {
      window.removeEventListener('resize', ajustarPosicion);
    };
  }, [abierto]);

  return (
    <span ref={contenedorRef} className="ayuda">
      <button
        ref={botonRef}
        type="button"
        className="ayuda__boton"
        aria-expanded={abierto}
        aria-controls={idPanel}
        aria-label={`Ayuda: ${termino}`}
        onClick={() => setAbierto((v) => !v)}
      >
        <span className="ayuda__icono" aria-hidden="true">
          ?
        </span>
      </button>
      <div ref={panelRef} id={idPanel} className="ayuda__panel" hidden={!abierto}>
        {children}
      </div>
    </span>
  );
}
