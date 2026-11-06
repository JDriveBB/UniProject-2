/**
 * Modal reutilizable (patrón 26c-3): dialog nativo con showModal, foco atrapado por el navegador, Escape cierra salvo mientras se guarda.
 */
import { useEffect, useId, useRef, type FormEvent, type ReactNode, type SyntheticEvent } from 'react';

export interface PropsModal {
  titulo: string;
  subtitulo?: string;
  ancho?: 'normal' | 'tarea' | 'dialogo';
  tonoTitulo?: 'peligro' | 'exito';
  ocupado?: boolean;
  alCerrar: () => void;
  alEnviar?: () => void;
  acciones: (cerrar: () => void) => ReactNode;
  children?: ReactNode;
}

export function Modal({
  titulo,
  subtitulo,
  ancho = 'normal',
  tonoTitulo,
  ocupado = false,
  alCerrar,
  alEnviar,
  acciones,
  children,
}: PropsModal) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const idTitulo = useId();
  const idSubtitulo = useId();

  useEffect(() => {
    const d = dialogo.current;
    if (d !== null && !d.open) {
      d.showModal();
    }
  }, []);

  function cerrar() {
    dialogo.current?.close();
  }

  function alCancelar(evento: SyntheticEvent<HTMLDialogElement>) {
    if (ocupado) {
      evento.preventDefault();
    }
  }

  function alSubmit(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (ocupado) {
      return;
    }
    alEnviar?.();
  }

  const clasesDialogo = ['modal', ancho !== 'normal' ? `modal--${ancho}` : ''].filter(Boolean).join(' ');
  const clasesTitulo = ['modal__titulo', tonoTitulo !== undefined ? `modal__titulo--${tonoTitulo}` : '']
    .filter(Boolean)
    .join(' ');

  return (
    <dialog
      ref={dialogo}
      className={clasesDialogo}
      aria-labelledby={idTitulo}
      aria-describedby={subtitulo !== undefined ? idSubtitulo : undefined}
      onCancel={alCancelar}
      onClose={alCerrar}
    >
      <form className="modal__formulario" noValidate onSubmit={alSubmit}>
        <header className="modal__cabecera">
          <h2 id={idTitulo} className={clasesTitulo}>
            {titulo}
          </h2>
          {subtitulo !== undefined && (
            <p id={idSubtitulo} className="modal__subtitulo">
              {subtitulo}
            </p>
          )}
        </header>
        {children}
        <div className="modal__acciones">{acciones(cerrar)}</div>
      </form>
    </dialog>
  );
}
