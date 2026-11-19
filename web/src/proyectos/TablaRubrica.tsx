import { decimalEsCo } from '../formato';
import type { BorradorCriterio, CambiosCriterio } from './borrador';
import { MAXIMO_CRITERIO } from './validacion';
import type { ErroresCriterio } from './validacion';

const NINGUNO: ErroresCriterio = {};

function FilaEditable({
  criterio,
  numero,
  errores,
  alCambiar,
  alQuitar,
}: {
  criterio: BorradorCriterio;
  numero: number;
  errores: ErroresCriterio;
  alCambiar: (cambios: CambiosCriterio) => void;
  alQuitar: () => void;
}) {
  const base = 'criterio-' + criterio.clave;
  return (
    <tr>
      <th scope="row">
        <input
          className="tabla__entrada"
          aria-label={`Criterio ${numero}`}
          value={criterio.descripcion}
          maxLength={MAXIMO_CRITERIO}
          onChange={(e) => alCambiar({ descripcion: e.target.value })}
          aria-invalid={errores.descripcion !== undefined}
          aria-describedby={errores.descripcion !== undefined ? base + '-descripcion' : undefined}
        />
        {errores.descripcion !== undefined && (
          <p id={base + '-descripcion'} className="campo__error">
            {errores.descripcion}
          </p>
        )}
      </th>
      <td data-etiqueta="Peso (%)">
        <input
          className="tabla__entrada"
          type="number"
          inputMode="decimal"
          min={0.01}
          max={100}
          step={0.01}
          aria-label={`Peso del criterio ${numero} (%)`}
          value={criterio.peso}
          onChange={(e) => alCambiar({ peso: e.target.value })}
          aria-invalid={errores.peso !== undefined}
          aria-describedby={errores.peso !== undefined ? base + '-peso' : undefined}
        />
        {errores.peso !== undefined && (
          <p id={base + '-peso'} className="campo__error">
            {errores.peso}
          </p>
        )}
      </td>
      <td data-etiqueta="Puntaje máximo">
        <input
          className="tabla__entrada"
          type="number"
          inputMode="decimal"
          min={0.01}
          max={999.99}
          step={0.01}
          aria-label={`Puntaje máximo del criterio ${numero}`}
          value={criterio.puntaje}
          onChange={(e) => alCambiar({ puntaje: e.target.value })}
          aria-invalid={errores.puntaje !== undefined}
          aria-describedby={errores.puntaje !== undefined ? base + '-puntaje' : undefined}
        />
        {errores.puntaje !== undefined && (
          <p id={base + '-puntaje'} className="campo__error">
            {errores.puntaje}
          </p>
        )}
      </td>
      <td>
        <button type="button" className="boton-texto" onClick={alQuitar}>
          Quitar<span className="solo-lectores"> el criterio {numero}</span>
        </button>
      </td>
    </tr>
  );
}

/** Rúbrica de P03: Criterio · Peso (%) · Puntaje máximo; en edición, campos y "Quitar" (sin Figma). */
export function TablaRubrica({
  criterios,
  editable,
  errores,
  alCambiar,
  alQuitar,
}: {
  criterios: BorradorCriterio[];
  editable: boolean;
  errores: Record<string, ErroresCriterio>;
  alCambiar: (claveCriterio: string, cambios: CambiosCriterio) => void;
  alQuitar: (claveCriterio: string) => void;
}) {
  return (
    <div className="tabla-contenedor">
      <table className="tabla tabla--rubrica">
        <caption className="solo-lectores">Rúbrica de evaluación del hito</caption>
        <colgroup>
          <col />
          <col className="tabla__col-peso" />
          <col className="tabla__col-puntaje" />
          {editable && <col className="tabla__col-acciones" />}
        </colgroup>
        <thead>
          <tr>
            <th scope="col">Criterio</th>
            <th scope="col">Peso (%)</th>
            <th scope="col">Puntaje máximo</th>
            {editable && (
              <th scope="col">
                <span className="solo-lectores">Acciones</span>
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {criterios.length === 0 ? (
            <tr>
              <td className="tabla__vacia" colSpan={editable ? 4 : 3}>
                Aún no hay criterios.
              </td>
            </tr>
          ) : (
            criterios.map((c, i) =>
              editable ? (
                <FilaEditable
                  key={c.clave}
                  criterio={c}
                  numero={i + 1}
                  errores={errores[c.clave] ?? NINGUNO}
                  alCambiar={(cambios) => alCambiar(c.clave, cambios)}
                  alQuitar={() => alQuitar(c.clave)}
                />
              ) : (
                <tr key={c.clave}>
                  <th scope="row">{c.descripcion}</th>
                  <td data-etiqueta="Peso (%)">{decimalEsCo(Number(c.peso))}</td>
                  <td data-etiqueta="Puntaje máximo">{decimalEsCo(Number(c.puntaje))}</td>
                </tr>
              ),
            )
          )}
        </tbody>
      </table>
    </div>
  );
}
