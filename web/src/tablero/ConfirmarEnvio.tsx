import { Boton } from '../componentes/Boton';
import { Modal } from '../componentes/Modal';
import { mensajeConfirmacionEnvio } from '../tarea/envio';

export interface PropsConfirmarEnvio {
  nombreHito: string;
  enviando: boolean;
  alCancelar: () => void;
  alConfirmar: () => void;
}

/**
 * P08 Confirmación previa al envío del hito (bloque 26g).
 * Modal provisional con estilo estándar: pide confirmación antes de llamar al flujo existente de envío.
 */
export function ConfirmarEnvio({
  nombreHito,
  enviando,
  alCancelar,
  alConfirmar,
}: PropsConfirmarEnvio) {
  return (
    <Modal
      ancho="dialogo"
      titulo="¿Enviar el hito a revisión?"
      ocupado={enviando}
      alCerrar={alCancelar}
      alEnviar={alConfirmar}
      acciones={(cerrar) => (
        <>
          <Boton type="button" variante="secundario" disabled={enviando} onClick={cerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" variante="primario" cargando={enviando}>
            Enviar a revisión
          </Boton>
        </>
      )}
    >
      <p className="modal__texto">{mensajeConfirmacionEnvio(nombreHito)}</p>
    </Modal>
  );
}
