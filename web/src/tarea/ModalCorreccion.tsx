import { useId, useState } from 'react';
import { solicitarCorreccion } from '../api/tareas';
import type { EvidenciaVista, TareaVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { CampoArea } from '../componentes/CampoArea';
import { Modal } from '../componentes/Modal';
import type { Mensaje } from '../proyectos/errores';
import {
  CAMPOS_CORRECCION,
  validarCorreccion,
  type BorradorCorreccion,
  type ErroresCorreccion,
} from './formularios';
import {
  mensajeErrorTarea,
  nombreDeEvidencia,
  nombreEvidencia,
  primerNombre,
  SIN_PERMISO_LIDER,
} from './vista';

export interface PropsModalCorreccion {
  tarea: TareaVista;
  nombre: string;
  evidencias: EvidenciaVista[];
  alCerrar: () => void;
  alGuardar: (m: Mensaje) => void;
}

export function ModalCorreccion({
  tarea,
  nombre,
  evidencias,
  alCerrar,
  alGuardar,
}: PropsModalCorreccion) {
  const [borrador, setBorrador] = useState<BorradorCorreccion>({
    idEvidencia: evidencias.length > 0 ? String(evidencias[0].idEvidencia) : '',
    observaciones: '',
  });
  const [intentado, setIntentado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const idSelect = useId();
  const idErrorSelect = idSelect + '-error';
  const idObservaciones = useId();

  const errores: ErroresCorreccion = intentado ? validarCorreccion(borrador) : {};

  async function enviar() {
    setIntentado(true);
    const errs = validarCorreccion(borrador);
    if (errs.idEvidencia !== undefined) {
      document.getElementById(idSelect)?.focus();
      return;
    }
    if (errs.observaciones !== undefined) {
      document.getElementById(idObservaciones)?.focus();
      return;
    }

    setGuardando(true);
    setError(null);
    try {
      const c = await solicitarCorreccion(tarea.idTarea, {
        idEvidencia: Number(borrador.idEvidencia),
        observaciones: borrador.observaciones.trim(),
      });
      alGuardar({
        tipo: 'exito',
        texto: `Se pidió a ${c.estudiante} rehacer «${nombreDeEvidencia(evidencias, c.idEvidencia)}».`,
      });
    } catch (err) {
      setError(mensajeErrorTarea(err, CAMPOS_CORRECCION, SIN_PERMISO_LIDER));
    } finally {
      setGuardando(false);
    }
  }

  const subtitulo = `${nombre} · Encargado: ${tarea.encargado ?? 'Sin asignar'}`;
  const aviso = `Al enviar la solicitud, la tarea vuelve a En progreso hasta que ${primerNombre(tarea.encargado ?? '')} adjunte la nueva versión, y el sistema le envía una notificación.`;

  return (
    <Modal
      ancho="tarea"
      titulo="Solicitar corrección"
      subtitulo={subtitulo}
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
      {error !== null && <Alerta tipo="error">{error}</Alerta>}

      <div className="campo">
        <label className="campo__etiqueta" htmlFor={idSelect}>
          Evidencia que debe rehacer
        </label>
        <select
          id={idSelect}
          className="campo__entrada"
          value={borrador.idEvidencia}
          disabled={guardando}
          aria-invalid={errores.idEvidencia !== undefined}
          aria-describedby={errores.idEvidencia !== undefined ? idErrorSelect : undefined}
          onChange={(e) => setBorrador((b) => ({ ...b, idEvidencia: e.target.value }))}
        >
          {evidencias.map((e) => (
            <option key={e.idEvidencia} value={String(e.idEvidencia)}>
              {nombreEvidencia(e)}
            </option>
          ))}
        </select>
        {errores.idEvidencia !== undefined && (
          <p id={idErrorSelect} className="campo__error">
            {errores.idEvidencia}
          </p>
        )}
      </div>

      <CampoArea
        id={idObservaciones}
        etiqueta="Puntos que debe corregir"
        className="campo--correccion"
        value={borrador.observaciones}
        error={errores.observaciones}
        disabled={guardando}
        onChange={(e) => setBorrador((b) => ({ ...b, observaciones: e.target.value }))}
      />

      <p className="aviso-modal aviso-modal--aviso">{aviso}</p>
    </Modal>
  );
}
