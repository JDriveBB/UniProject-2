import type { TareaVista } from '../api/tipos';
import { Boton } from '../componentes/Boton';
import { Tarjeta } from '../componentes/Tarjeta';
import type { PermisosTarea } from './permisos';

export type AccionTarea = 'apoyo' | 'reasignar' | 'correccion';

export interface PropsTarjetaAcciones {
  tarea: TareaVista;
  permisos: PermisosTarea;
  bloqueoCorreccion: string | null;
  ids: Record<AccionTarea, string>;
  alAbrir: (a: AccionTarea) => void;
}

export function TarjetaAcciones({
  tarea,
  permisos,
  bloqueoCorreccion,
  ids,
  alAbrir,
}: PropsTarjetaAcciones) {
  if (!permisos.apoyo && !permisos.gestionar) {
    return null;
  }

  const textoBotonAsignar = tarea.idEncargado === null ? 'Asignar encargado' : 'Reasignar tarea';
  const completada = tarea.estado === 'COMPLETADA';

  return (
    <Tarjeta titulo="Acciones">
      {permisos.apoyo && (
        <div className="accion-tarea">
          <p className="accion-tarea__subtitulo">Encargado de la tarea</p>
          <Boton id={ids.apoyo} variante="secundario" onClick={() => alAbrir('apoyo')}>
            Solicitar apoyo
          </Boton>
        </div>
      )}

      {permisos.gestionar && (
        <>
          <div className="accion-tarea">
            <p className="accion-tarea__subtitulo">Líder del equipo</p>
            <Boton
              id={ids.reasignar}
              variante="secundario"
              disabled={completada}
              onClick={() => alAbrir('reasignar')}
            >
              {textoBotonAsignar}
            </Boton>
            <p className="accion-tarea__ayuda">
              Permite cambiar el encargado o dividir el alcance en una nueva tarea. Cada reasignación
              queda en el registro de auditoría.
            </p>
            {completada && <p className="accion-tarea__ayuda">Una tarea completada no se reasigna.</p>}
          </div>

          <div className="accion-tarea">
            <Boton
              id={ids.correccion}
              variante="secundario"
              disabled={bloqueoCorreccion !== null}
              onClick={() => alAbrir('correccion')}
            >
              Solicitar corrección
            </Boton>
            <p className="accion-tarea__ayuda">
              Pide al integrante rehacer una evidencia indicando los puntos que debe corregir. La tarea
              vuelve a En progreso hasta que adjunte la nueva versión.
            </p>
            {bloqueoCorreccion !== null && (
              <p className="accion-tarea__ayuda">{bloqueoCorreccion}</p>
            )}
          </div>
        </>
      )}
    </Tarjeta>
  );
}
