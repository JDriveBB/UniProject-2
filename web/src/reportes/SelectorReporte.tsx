import { useId } from 'react';
import type { EquipoVista, ProyectoVista } from '../api/tipos';
import { Tarjeta } from '../componentes/Tarjeta';
import { etiquetaHito, type HitoDelEquipo } from '../tablero/ubicacion';
import type { Seleccion } from './seleccion';
import { AYUDA_PDF, TODOS_LOS_HITOS } from './vista';

export interface PropsSelectorReporte {
  proyectos: ProyectoVista[];
  idProyecto: number;
  equipos: EquipoVista[] | null;
  idEquipo: number | null;
  hitos: HitoDelEquipo[];
  idHitoEquipo: number | null;
  deshabilitado: boolean;
  idAyuda: string;
  alCambiar: (sel: Seleccion) => void;
}

export function SelectorReporte({
  proyectos,
  idProyecto,
  equipos,
  idEquipo,
  hitos,
  idHitoEquipo,
  deshabilitado,
  idAyuda,
  alCambiar,
}: PropsSelectorReporte) {
  const idSelectProyecto = useId();
  const idSelectEquipo = useId();
  const idSelectHito = useId();

  return (
    <Tarjeta titulo="Equipo y hito">
      <div className="fila-campos fila-campos--reporte">
        <div className="campo">
          <label className="campo__etiqueta" htmlFor={idSelectProyecto}>
            Proyecto
          </label>
          <select
            id={idSelectProyecto}
            className="campo__entrada"
            value={String(idProyecto)}
            disabled={deshabilitado}
            onChange={(e) =>
              alCambiar({
                idProyecto: Number(e.target.value),
                idEquipo: null,
                idHitoEquipo: null,
              })
            }
          >
            {proyectos.map((p) => (
              <option key={p.idProyecto} value={String(p.idProyecto)}>
                {p.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className="campo">
          <label className="campo__etiqueta" htmlFor={idSelectEquipo}>
            Equipo
          </label>
          {equipos === null ? (
            <select id={idSelectEquipo} className="campo__entrada" value="" disabled>
              <option value="">Cargando…</option>
            </select>
          ) : equipos.length === 0 ? (
            <select id={idSelectEquipo} className="campo__entrada" value="" disabled>
              <option value="">Sin equipos</option>
            </select>
          ) : (
            <select
              id={idSelectEquipo}
              className="campo__entrada"
              value={idEquipo !== null ? String(idEquipo) : ''}
              disabled={deshabilitado}
              onChange={(e) =>
                alCambiar({
                  idProyecto,
                  idEquipo: Number(e.target.value),
                  idHitoEquipo: null,
                })
              }
            >
              {equipos.map((eq) => (
                <option key={eq.idEquipo} value={String(eq.idEquipo)}>
                  {eq.nombreEquipo}
                </option>
              ))}
            </select>
          )}
        </div>

        <div className="campo">
          <label className="campo__etiqueta" htmlFor={idSelectHito}>
            Hito
          </label>
          <select
            id={idSelectHito}
            className="campo__entrada"
            value={idHitoEquipo === null ? '' : String(idHitoEquipo)}
            disabled={deshabilitado || idEquipo === null}
            onChange={(e) =>
              alCambiar({
                idProyecto,
                idEquipo,
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
      </div>
      <p id={idAyuda} className="texto-ayuda">
        {AYUDA_PDF}
      </p>
    </Tarjeta>
  );
}
