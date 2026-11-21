import { useId, useState } from 'react';
import { solicitarApoyo } from '../api/tareas';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { CampoArea } from '../componentes/CampoArea';
import { Modal } from '../componentes/Modal';
import type { Mensaje } from '../proyectos/errores';
import { CAMPOS_APOYO, validarTexto } from './formularios';
import { mensajeErrorTarea, SIN_PERMISO_APOYO } from './vista';

export interface PropsModalApoyo {
  idTarea: number;
  nombre: string;
  alCerrar: () => void;
  alGuardar: (m: Mensaje) => void;
}

export function ModalApoyo({ idTarea, nombre, alCerrar, alGuardar }: PropsModalApoyo) {
  const [mensaje, setMensaje] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [errorApi, setErrorApi] = useState<string | null>(null);

  const idMensaje = useId();
  const errorTexto = intentado ? validarTexto(mensaje, 500, 'Cuenta qué necesitas.') : undefined;

  async function enviar() {
    setIntentado(true);
    const err = validarTexto(mensaje, 500, 'Cuenta qué necesitas.');
    if (err !== undefined) {
      document.getElementById(idMensaje)?.focus();
      return;
    }

    setGuardando(true);
    setErrorApi(null);
    try {
      await solicitarApoyo(idTarea, mensaje.trim());
      alGuardar({ tipo: 'exito', texto: 'Solicitud de apoyo registrada.' });
    } catch (errApi) {
      setErrorApi(mensajeErrorTarea(errApi, CAMPOS_APOYO, SIN_PERMISO_APOYO));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Modal
      ancho="normal"
      titulo="Solicitar apoyo"
      subtitulo={nombre}
      ocupado={guardando}
      alCerrar={alCerrar}
      alEnviar={() => void enviar()}
      acciones={(cerrar) => (
        <>
          <Boton type="button" variante="secundario" onClick={cerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" variante="primario" cargando={guardando}>
            Enviar solicitud
          </Boton>
        </>
      )}
    >
      {errorApi !== null && <Alerta tipo="error">{errorApi}</Alerta>}

      <CampoArea
        id={idMensaje}
        etiqueta="¿Qué necesitas?"
        className="campo--descripcion-tarea"
        value={mensaje}
        error={errorTexto}
        disabled={guardando}
        onChange={(e) => setMensaje(e.target.value)}
      />
      <p className="texto-ayuda">
        La solicitud queda registrada en el historial del equipo, donde tu líder puede verla.
      </p>
    </Modal>
  );
}
