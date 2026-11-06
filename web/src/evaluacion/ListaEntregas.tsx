import { Link } from 'react-router';
import type { TareaVista } from '../api/tipos';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { rutas } from '../rutas';
import { codigosDeTareas, estadoTarea, nombreConCodigo } from '../tablero/marcas';

export interface PropsListaEntregas {
  tareas: TareaVista[];
}

export function ListaEntregas({ tareas }: PropsListaEntregas) {
  const codigos = codigosDeTareas(tareas);

  return (
    <Tarjeta titulo="Entregas del equipo">
      <p className="texto-secundario">Abre cada tarea para revisar sus evidencias.</p>
      {tareas.length === 0 ? (
        <p className="pagina__vacio">El hito no tiene tareas.</p>
      ) : (
        <ul className="lista-simple">
          {tareas.map((t) => {
            const estado = estadoTarea(t.estado);
            return (
              <li key={t.idTarea} className="entrega">
                <Link to={rutas.tarea(t.idTarea)} className="enlace-tarea">
                  {nombreConCodigo(codigos, t)}
                </Link>
                <Insignia tono={estado.tono}>{estado.texto}</Insignia>
                <span className="texto-secundario">{`${t.porcentajeAvance} %`}</span>
              </li>
            );
          })}
        </ul>
      )}
    </Tarjeta>
  );
}
