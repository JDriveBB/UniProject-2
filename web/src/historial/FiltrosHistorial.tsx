import { useId } from 'react';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Tarjeta } from '../componentes/Tarjeta';
import { TODOS_LOS_HITOS } from '../reportes/vista';
import { etiquetaHito, type HitoDelEquipo } from '../tablero/ubicacion';
import { FILTROS_VACIOS, hayFiltros, validarRango, type Filtros, type OpcionAccion } from './filtros';
import { TODAS_LAS_ACCIONES } from './vista';

export interface PropsFiltrosHistorial {
  filtros: Filtros;
  acciones: OpcionAccion[];
  hitos: HitoDelEquipo[];
  alCambiar: (f: Filtros) => void;
}

export function FiltrosHistorial({ filtros, acciones, hitos, alCambiar }: PropsFiltrosHistorial) {
  const idAccion = useId();
  const idHito = useId();

  return (
    <Tarjeta titulo="Filtros">
      <div className="fila-campos fila-campos--historial">
        <div className="campo">
          <label className="campo__etiqueta" htmlFor={idAccion}>
            Acción
          </label>
          <select
            id={idAccion}
            className="campo__entrada"
            value={filtros.accion ?? ''}
            disabled={acciones.length === 0}
            onChange={(e) => alCambiar({ ...filtros, accion: e.target.value === '' ? null : e.target.value })}
          >
            <option value="">{TODAS_LAS_ACCIONES}</option>
            {acciones.map((a) => (
              <option key={a.valor} value={a.valor}>
                {a.texto}
              </option>
            ))}
          </select>
        </div>

        <div className="campo">
          <label className="campo__etiqueta" htmlFor={idHito}>
            Hito
          </label>
          <select
            id={idHito}
            className="campo__entrada"
            value={filtros.idHitoEquipo === null ? '' : String(filtros.idHitoEquipo)}
            disabled={hitos.length === 0}
            onChange={(e) =>
              alCambiar({
                ...filtros,
                idHitoEquipo: e.target.value === '' ? null : Number(e.target.value),
              })
            }
          >
            <option value="">{TODOS_LOS_HITOS}</option>
            {hitos.map((h) => (
              <option key={h.idHitoEquipo} value={String(h.idHitoEquipo)}>
                {etiquetaHito(h.orden, h.nombreHito)}
              </option>
            ))}
          </select>
        </div>

        <Campo
          etiqueta="Desde"
          type="date"
          value={filtros.desde ?? ''}
          onChange={(e) => alCambiar({ ...filtros, desde: e.target.value === '' ? null : e.target.value })}
        />

        <Campo
          etiqueta="Hasta"
          type="date"
          value={filtros.hasta ?? ''}
          error={validarRango(filtros.desde, filtros.hasta)}
          onChange={(e) => alCambiar({ ...filtros, hasta: e.target.value === '' ? null : e.target.value })}
        />
      </div>

      <div className="pagina__acciones">
        <Boton
          variante="secundario"
          disabled={!hayFiltros(filtros)}
          onClick={() => alCambiar(FILTROS_VACIOS)}
        >
          Limpiar filtros
        </Boton>
      </div>
    </Tarjeta>
  );
}
