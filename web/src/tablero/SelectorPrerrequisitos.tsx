import { useId } from 'react';
import type { TareaVista } from '../api/tipos';
import { Casilla } from '../componentes/Casilla';
import { Insignia } from '../componentes/Insignia';
import { nombreConCodigo } from './marcas';

/**
 * «Bloqueada por» de P05 (decisión 26c-5): selector propio, no un select nativo. La etiqueta y el campo abren la
 * lista (un clic) y cada casilla elige una tarea (un clic); la lista sigue abierta para elegir más. Las elegidas
 * quedan como chips con ×. Quien lo usa controla si la lista está abierta (Escape la cierra desde el modal).
 */
export function SelectorPrerrequisitos({ id, tareas, codigos, elegidas, abierto, error, deshabilitado, alAbrir,
                                         alCambiar }: {
  id: string;
  tareas: TareaVista[];
  codigos: Map<number, string>;
  elegidas: number[];
  abierto: boolean;
  error?: string;
  deshabilitado: boolean;
  alAbrir: (abierto: boolean) => void;
  alCambiar: (elegidas: number[]) => void;
}) {
  const idLista = useId();
  const idError = useId();
  const sinTareas = tareas.length === 0;
  const chips = tareas.filter((t) => elegidas.includes(t.idTarea));
  const clases = ['selector', abierto ? 'selector--abierto' : '', error !== undefined ? 'selector--error' : '']
    .filter(Boolean)
    .join(' ');

  function alternar(idTarea: number, marcada: boolean) {
    alCambiar(marcada ? [...elegidas, idTarea] : elegidas.filter((x) => x !== idTarea));
  }

  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={id}>
        Bloqueada por
      </label>
      <div className={clases}>
        {chips.map((t) => {
          const texto = nombreConCodigo(codigos, t);
          return (
            <Insignia key={t.idTarea} tono="info">
              {texto}
              <button
                type="button"
                className="insignia__quitar"
                aria-label={`Quitar ${texto}`}
                disabled={deshabilitado}
                onClick={() => alternar(t.idTarea, false)}
              >
                <span aria-hidden="true">×</span>
              </button>
            </Insignia>
          );
        })}
        <button
          id={id}
          type="button"
          className="selector__abrir"
          aria-expanded={abierto}
          aria-controls={idLista}
          aria-describedby={error !== undefined ? idError : undefined}
          disabled={deshabilitado || sinTareas}
          onClick={() => alAbrir(!abierto)}
        >
          {chips.length === 0 && <span>{sinTareas ? 'No hay otras tareas en este hito' : 'Ninguna'}</span>}
          <span className="selector__flecha" aria-hidden="true">
            ▾
          </span>
        </button>
      </div>
      {abierto && (
        <div id={idLista} className="selector__lista" role="group" aria-label="Tareas que deben terminar antes">
          {tareas.map((t) => (
            <Casilla
              key={t.idTarea}
              checked={elegidas.includes(t.idTarea)}
              disabled={deshabilitado}
              onChange={(e) => alternar(t.idTarea, e.target.checked)}
            >
              {nombreConCodigo(codigos, t)}
            </Casilla>
          ))}
        </div>
      )}
      {error !== undefined && (
        <p id={idError} className="campo__error">
          {error}
        </p>
      )}
    </div>
  );
}
