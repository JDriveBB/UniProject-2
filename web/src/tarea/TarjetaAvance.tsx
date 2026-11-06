import { useId, useState, type FormEvent } from 'react';
import { actualizarAvance } from '../api/tareas';
import type { TareaVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Tarjeta } from '../componentes/Tarjeta';
import type { Mensaje } from '../proyectos/errores';
import {
  CAMPOS_AVANCE,
  solicitudAvance,
  validarAvance,
  type BorradorAvance,
  type ErroresAvance,
} from './formularios';
import { mensajeErrorTarea, SIN_PERMISO_AVANCE } from './vista';

export interface PropsTarjetaAvance {
  tarea: TareaVista;
  alHecho: (m: Mensaje) => void;
}

export function TarjetaAvance({ tarea, alHecho }: PropsTarjetaAvance) {
  const [borrador, setBorrador] = useState<BorradorAvance>({
    porcentaje: String(tarea.porcentajeAvance),
    observacion: '',
  });
  const [intentado, setIntentado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const idPorcentaje = useId();
  const idErrorPorcentaje = idPorcentaje + '-error';
  const idObservacion = useId();

  const errores: ErroresAvance = intentado ? validarAvance(borrador) : {};
  const deshabilitado = guardando || tarea.bloqueada;

  async function guardar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setIntentado(true);
    const errs = validarAvance(borrador);
    if (errs.porcentaje !== undefined) {
      document.getElementById(idPorcentaje)?.focus();
      return;
    }
    if (errs.observacion !== undefined) {
      document.getElementById(idObservacion)?.focus();
      return;
    }

    setGuardando(true);
    setError(null);
    try {
      const t = await actualizarAvance(tarea.idTarea, solicitudAvance(borrador));
      setBorrador({ porcentaje: String(t.porcentajeAvance), observacion: '' });
      setIntentado(false);
      alHecho({ tipo: 'exito', texto: `Avance registrado: ${t.porcentajeAvance} %.` });
    } catch (err) {
      setError(mensajeErrorTarea(err, CAMPOS_AVANCE, SIN_PERMISO_AVANCE));
    } finally {
      setGuardando(false);
    }
  }

  const clasesSufijo = [
    'campo-sufijo',
    errores.porcentaje !== undefined ? 'campo-sufijo--error' : '',
    deshabilitado ? 'campo-sufijo--deshabilitado' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <Tarjeta titulo="Registrar avance">
      {error !== null && <Alerta tipo="error">{error}</Alerta>}
      {tarea.bloqueada && (
        <p className="texto-ayuda">
          La tarea está bloqueada: podrás registrar avance cuando terminen sus prerrequisitos.
        </p>
      )}
      <form className="formulario-avance" noValidate onSubmit={(e) => void guardar(e)}>
        <div className="campo campo--porcentaje">
          <label className="campo__etiqueta" htmlFor={idPorcentaje}>
            Porcentaje de avance
          </label>
          <div className={clasesSufijo}>
            <input
              id={idPorcentaje}
              type="text"
              inputMode="numeric"
              autoComplete="off"
              className="campo-sufijo__entrada"
              disabled={deshabilitado}
              value={borrador.porcentaje}
              aria-invalid={errores.porcentaje !== undefined}
              aria-describedby={errores.porcentaje !== undefined ? idErrorPorcentaje : undefined}
              onChange={(e) => setBorrador((b) => ({ ...b, porcentaje: e.target.value }))}
            />
            <span className="campo-sufijo__texto" aria-hidden="true">
              %
            </span>
          </div>
          {errores.porcentaje !== undefined && (
            <p id={idErrorPorcentaje} className="campo__error">
              {errores.porcentaje}
            </p>
          )}
        </div>

        <Campo
          id={idObservacion}
          etiqueta="Observación"
          className="campo--observacion"
          value={borrador.observacion}
          error={errores.observacion}
          disabled={deshabilitado}
          onChange={(e) => setBorrador((b) => ({ ...b, observacion: e.target.value }))}
        />

        <div className="formulario-avance__enviar">
          <Boton type="submit" variante="primario" cargando={guardando} disabled={deshabilitado}>
            Registrar avance
          </Boton>
        </div>
      </form>
    </Tarjeta>
  );
}
