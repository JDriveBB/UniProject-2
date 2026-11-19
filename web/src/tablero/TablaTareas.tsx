import { Link } from 'react-router';
import type { TareaVista } from '../api/tipos';
import { BarraAvance } from '../componentes/BarraAvance';
import { Insignia } from '../componentes/Insignia';
import { Tabla } from '../componentes/Tabla';
import type { ColumnaTabla } from '../componentes/Tabla';
import { fechaDia } from '../formato';
import { rutas } from '../rutas';
import { estadoTarea, marcasDe } from './marcas';

/** Tabla de P04 (decisión 26c-10): columnas y anchos de Figma. El título abre el detalle de la tarea (P07, 26d). */
export function TablaTareas({
  tareas,
  codigos,
  puedeCrear,
}: {
  tareas: TareaVista[];
  codigos: Map<number, string>;
  puedeCrear: boolean;
}) {
  const mensaje = puedeCrear
    ? 'Aún no hay tareas en este hito. Crea la primera con «Nueva tarea».'
    : 'Aún no hay tareas en este hito.';
  if (tareas.length === 0) {
    return (
      <div className="tabla-contenedor">
        <p className="tabla__vacia vacio-tareas">{mensaje}</p>
      </div>
    );
  }
  const columnas: ColumnaTabla<TareaVista>[] = [
    { titulo: 'ID', clase: 'tabla__col-id', celda: (t) => codigos.get(t.idTarea) ?? '' },
    {
      titulo: 'Tarea',
      clase: 'tabla__col-tarea',
      celda: (t) => (
        <Link className="enlace-tarea" to={rutas.tarea(t.idTarea)}>
          {t.titulo}
        </Link>
      ),
    },
    {
      titulo: 'Encargado',
      clase: 'tabla__col-encargado',
      celda: (t) => t.encargado ?? <span className="texto-secundario">Sin asignar</span>,
    },
    {
      titulo: 'Estado',
      clase: 'tabla__col-estado',
      celda: (t) => {
        const estado = estadoTarea(t.estado);
        return <Insignia tono={estado.tono}>{estado.texto}</Insignia>;
      },
    },
    { titulo: 'Avance', clase: 'tabla__col-avance', celda: (t) => <BarraAvance porcentaje={t.porcentajeAvance} /> },
    { titulo: 'Fecha límite', clase: 'tabla__col-fecha', celda: (t) => fechaDia(t.fechaLimiteEfectiva) },
    {
      titulo: 'Marcas',
      celda: (t) => (
        <span className="marcas">
          {marcasDe(t).map((m) => (
            <Insignia key={m.texto} tono={m.tono}>
              {m.texto}
            </Insignia>
          ))}
        </span>
      ),
    },
  ];
  return (
    <Tabla
      titulo="Tareas del hito"
      clase="tabla--tareas"
      columnas={columnas}
      filas={tareas}
      claveFila={(t) => t.idTarea}
      vacio={mensaje}
    />
  );
}
