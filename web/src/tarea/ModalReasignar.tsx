import { useId, useState } from 'react';
import { editarTarea, reasignarTarea } from '../api/tareas';
import type { MiembroVista, TareaVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { CampoArea } from '../componentes/CampoArea';
import { Modal } from '../componentes/Modal';
import { decimalEsCo } from '../formato';
import type { Mensaje } from '../proyectos/errores';
import { CAMPOS_TAREA } from '../tablero/nuevaTarea';
import {
  CAMPOS_REASIGNACION,
  primerErrorReasignacion,
  REASIGNACION_VACIA,
  solicitudAsignar,
  solicitudReasignacion,
  validarReasignacion,
  type BorradorReasignacion,
  type CampoReasignacion,
  type ErroresReasignacion,
} from './formularios';
import { mensajeErrorTarea, SIN_PERMISO_LIDER } from './vista';

export interface PropsModalReasignar {
  tarea: TareaVista;
  nombre: string;
  miembros: MiembroVista[];
  alCerrar: () => void;
  alGuardar: (m: Mensaje) => void;
}

export function ModalReasignar({
  tarea,
  nombre,
  miembros,
  alCerrar,
  alGuardar,
}: PropsModalReasignar) {
  const modoAsignar = tarea.idEncargado === null;
  const [borrador, setBorrador] = useState<BorradorReasignacion>(REASIGNACION_VACIA);
  const [intentado, setIntentado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [errorApi, setErrorApi] = useState<string | null>(null);

  const idSelect = useId();
  const idErrorSelect = idSelect + '-error';
  const nombreRadio = useId();
  const idTituloParte = useId();
  const idHorasParte = useId();
  const idMotivo = useId();

  const idCampos: Record<CampoReasignacion, string> = {
    idNuevo: idSelect,
    titulo: idTituloParte,
    horas: idHorasParte,
    motivo: idMotivo,
  };

  const errores: ErroresReasignacion = intentado
    ? modoAsignar
      ? borrador.idNuevo.trim() === ''
        ? { idNuevo: 'Elige el nuevo encargado.' }
        : {}
      : validarReasignacion(borrador, tarea.horasEstimadas)
    : {};

  const miembrosDisponibles = miembros.filter((m) => m.idUsuario !== tarea.idEncargado);

  async function enviar() {
    setIntentado(true);
    if (modoAsignar) {
      if (borrador.idNuevo.trim() === '') {
        document.getElementById(idSelect)?.focus();
        return;
      }
    } else {
      const errs = validarReasignacion(borrador, tarea.horasEstimadas);
      const primer = primerErrorReasignacion(errs);
      if (primer !== null) {
        document.getElementById(idCampos[primer])?.focus();
        return;
      }
    }

    setGuardando(true);
    setErrorApi(null);
    const miembro = miembros.find((m) => m.idUsuario === Number(borrador.idNuevo));
    const nombreMiembro = miembro?.nombre ?? '';

    if (modoAsignar) {
      try {
        await editarTarea(tarea.idTarea, solicitudAsignar(tarea, Number(borrador.idNuevo)));
        alGuardar({
          tipo: 'exito',
          texto: `La tarea quedó a cargo de ${nombreMiembro}.`,
        });
      } catch (err) {
        setErrorApi(mensajeErrorTarea(err, CAMPOS_TAREA, SIN_PERMISO_LIDER));
      } finally {
        setGuardando(false);
      }
    } else {
      try {
        const res = await reasignarTarea(tarea.idTarea, solicitudReasignacion(borrador));
        const texto =
          res.tareaNueva === null
            ? `La tarea se reasignó a ${nombreMiembro}.`
            : `Se creó la tarea «${res.tareaNueva.titulo}» a cargo de ${nombreMiembro}.`;
        alGuardar({ tipo: 'exito', texto });
      } catch (err) {
        setErrorApi(mensajeErrorTarea(err, CAMPOS_REASIGNACION, SIN_PERMISO_LIDER));
      } finally {
        setGuardando(false);
      }
    }
  }

  const titulo = modoAsignar ? 'Asignar encargado' : 'Reasignar tarea';
  const subtitulo = modoAsignar ? nombre : `${nombre} · Encargado: ${tarea.encargado ?? 'Sin asignar'}`;
  const textoBoton = modoAsignar ? 'Asignar' : 'Reasignar';

  return (
    <Modal
      ancho="normal"
      titulo={titulo}
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
            {textoBoton}
          </Boton>
        </>
      )}
    >
      {errorApi !== null && <Alerta tipo="error">{errorApi}</Alerta>}

      <div className="campo">
        <label className="campo__etiqueta" htmlFor={idSelect}>
          {modoAsignar ? 'Encargado' : 'Nuevo encargado'}
        </label>
        <select
          id={idSelect}
          className="campo__entrada"
          value={borrador.idNuevo}
          disabled={guardando}
          aria-invalid={errores.idNuevo !== undefined}
          aria-describedby={errores.idNuevo !== undefined ? idErrorSelect : undefined}
          onChange={(e) => setBorrador((b) => ({ ...b, idNuevo: e.target.value }))}
        >
          <option value="">Elige un integrante</option>
          {miembrosDisponibles.map((m) => (
            <option key={m.idUsuario} value={String(m.idUsuario)}>
              {m.nombre}
            </option>
          ))}
        </select>
        {errores.idNuevo !== undefined && (
          <p id={idErrorSelect} className="campo__error">
            {errores.idNuevo}
          </p>
        )}
      </div>

      {!modoAsignar && (
        <>
          <fieldset className="opciones-radio">
            <legend className="campo__etiqueta opciones-radio__titulo">
              Qué pasa al nuevo encargado
            </legend>
            <label className="opcion-radio">
              <input
                type="radio"
                className="opcion-radio__entrada"
                name={nombreRadio}
                value="TOTAL"
                checked={borrador.tipo === 'TOTAL'}
                disabled={guardando}
                onChange={() => setBorrador((b) => ({ ...b, tipo: 'TOTAL' }))}
              />
              <span>Toda la tarea</span>
            </label>
            <label className="opcion-radio">
              <input
                type="radio"
                className="opcion-radio__entrada"
                name={nombreRadio}
                value="PARCIAL"
                checked={borrador.tipo === 'PARCIAL'}
                disabled={guardando}
                onChange={() => setBorrador((b) => ({ ...b, tipo: 'PARCIAL' }))}
              />
              <span>Solo una parte (se crea una tarea nueva)</span>
            </label>
          </fieldset>

          {borrador.tipo === 'PARCIAL' && (
            <>
              <div className="fila-campos">
                <Campo
                  id={idTituloParte}
                  etiqueta="Título de la parte"
                  value={borrador.titulo}
                  error={errores.titulo}
                  disabled={guardando}
                  onChange={(e) => setBorrador((b) => ({ ...b, titulo: e.target.value }))}
                />
                <Campo
                  id={idHorasParte}
                  etiqueta="Horas de la parte"
                  inputMode="decimal"
                  value={borrador.horas}
                  error={errores.horas}
                  disabled={guardando}
                  onChange={(e) => setBorrador((b) => ({ ...b, horas: e.target.value }))}
                />
              </div>
              <p className="texto-ayuda">
                {`La tarea tiene ${decimalEsCo(tarea.horasEstimadas)} h. Las horas de la parte se le restan, y la parte hereda sus dependencias.`}
              </p>
            </>
          )}

          <CampoArea
            id={idMotivo}
            etiqueta="Motivo (opcional)"
            className="campo--descripcion-tarea"
            value={borrador.motivo}
            error={errores.motivo}
            disabled={guardando}
            onChange={(e) => setBorrador((b) => ({ ...b, motivo: e.target.value }))}
          />
          <p className="texto-ayuda">Cada reasignación queda en el registro de auditoría.</p>
        </>
      )}
    </Modal>
  );
}
