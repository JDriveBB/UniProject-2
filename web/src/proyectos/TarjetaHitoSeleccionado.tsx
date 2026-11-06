import { useId } from 'react';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Casilla } from '../componentes/Casilla';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import type { BorradorHito, CambiosCriterio, CambiosHito } from './borrador';
import { TablaRubrica } from './TablaRubrica';
import { estadoSuma, MAXIMO_DIAS, MAXIMO_NOMBRE_HITO, MINIMO_DIAS, sumaPesos, textoSumaPesos } from './validacion';
import type { ErroresBorrador, ErroresHito } from './validacion';

interface PropsHitoSeleccionado {
  hito: BorradorHito | null;
  errores: ErroresBorrador;
  editable: boolean;
  alCambiarHito: (claveHito: string, cambios: CambiosHito) => void;
  alCambiarCriterio: (claveHito: string, claveCriterio: string, cambios: CambiosCriterio) => void;
  alAgregarCriterio: (claveHito: string) => void;
  alQuitarCriterio: (claveHito: string, claveCriterio: string) => void;
}

/** P03 derecha 1: "Hito 2 · Diseño" con fechas, alerta de inactividad (26b-10), casilla y rúbrica (26b-9). */
export function TarjetaHitoSeleccionado({
  hito,
  errores,
  editable,
  alCambiarHito,
  alCambiarCriterio,
  alAgregarCriterio,
  alQuitarCriterio,
}: PropsHitoSeleccionado) {
  const idDias = useId();
  if (hito === null) {
    return (
      <Tarjeta titulo="Hito">
        <p className="texto-secundario">
          {editable
            ? 'Agrega un hito para definir sus fechas, su alerta de inactividad y su rúbrica.'
            : 'Este proyecto aún no tiene hitos.'}
        </p>
      </Tarjeta>
    );
  }
  const h = hito;
  const e: ErroresHito = errores.hitos[h.clave] ?? {};
  const errorRubrica: string | undefined = errores.rubricas[h.clave];
  const suma = sumaPesos(h.criterios);
  const idErrorDias = idDias + '-error';
  const nombre = h.nombre.trim() === '' ? 'Sin nombre' : h.nombre.trim();
  return (
    <Tarjeta titulo={`Hito ${h.orden} · ${nombre}`}>
      <div className="fila-campos fila-campos--hito">
        <Campo
          etiqueta="Nombre del hito"
          value={h.nombre}
          maxLength={MAXIMO_NOMBRE_HITO}
          readOnly={!editable}
          onChange={(ev) => alCambiarHito(h.clave, { nombre: ev.target.value })}
          error={e.nombre}
          required
        />
        <Campo
          etiqueta="Fecha de inicio"
          type="date"
          value={h.fechaInicio}
          readOnly={!editable}
          onChange={(ev) => alCambiarHito(h.clave, { fechaInicio: ev.target.value })}
          error={e.fechaInicio}
          required
        />
        <Campo
          etiqueta="Fecha límite"
          type="date"
          value={h.fechaLimite}
          readOnly={!editable}
          onChange={(ev) => alCambiarHito(h.clave, { fechaLimite: ev.target.value })}
          error={e.fechaLimite}
          required
        />
      </div>
      <div className="campo">
        <label className="campo__etiqueta" htmlFor={idDias}>
          Alerta de inactividad
        </label>
        <div className="campo-con-texto">
          <input
            id={idDias}
            className="campo__entrada campo__entrada--dias"
            type="number"
            inputMode="numeric"
            min={MINIMO_DIAS}
            max={MAXIMO_DIAS}
            step={1}
            value={h.diasInactividad}
            readOnly={!editable}
            onChange={(ev) => alCambiarHito(h.clave, { diasInactividad: ev.target.value })}
            aria-invalid={e.diasInactividad !== undefined}
            aria-describedby={e.diasInactividad !== undefined ? idErrorDias : undefined}
            required
          />
          <span className="texto-ayuda">días sin actividad en sus tareas (de 1 a 60)</span>
        </div>
        {e.diasInactividad !== undefined && (
          <p id={idErrorDias} className="campo__error">
            {e.diasInactividad}
          </p>
        )}
      </div>
      <Casilla
        checked={h.notificarDocente}
        disabled={!editable}
        onChange={(ev) => alCambiarHito(h.clave, { notificarDocente: ev.target.checked })}
      >
        Notificar también al docente. La alerta siempre le llega al líder del equipo.
      </Casilla>
      <h3 className="tarjeta__subtitulo">Rúbrica de evaluación</h3>
      <TablaRubrica
        criterios={h.criterios}
        editable={editable}
        errores={errores.criterios}
        alCambiar={(claveCriterio, cambios) => alCambiarCriterio(h.clave, claveCriterio, cambios)}
        alQuitar={(claveCriterio) => alQuitarCriterio(h.clave, claveCriterio)}
      />
      <div className="pie-rubrica">
        {editable && (
          <Boton variante="secundario" onClick={() => alAgregarCriterio(h.clave)}>
            + Agregar criterio
          </Boton>
        )}
        <Insignia tono={estadoSuma(suma) === 'completa' ? 'exito' : 'aviso'}>{textoSumaPesos(suma)}</Insignia>
      </div>
      {errorRubrica !== undefined && <p className="campo__error">{errorRubrica}</p>}
    </Tarjeta>
  );
}
