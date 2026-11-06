import type { EntradaHistorial } from '../api/tipos';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { fechaHora } from '../formato';
import { accionConEntidad } from './vista';

export interface PropsTablaHistorial {
  entradas: EntradaHistorial[];
  vacio: string;
}

export function TablaHistorial({ entradas, vacio }: PropsTablaHistorial) {
  const columnas: ColumnaTabla<EntradaHistorial>[] = [
    {
      titulo: 'Fecha y hora',
      clase: 'tabla__col-fecha-hora',
      celda: (f) => fechaHora(f.fechaHora),
    },
    {
      titulo: 'Autor',
      clase: 'tabla__col-autor',
      celda: (f) => f.autor,
    },
    {
      titulo: 'Acción',
      clase: 'tabla__col-accion',
      celda: (f) => accionConEntidad(f),
    },
    {
      titulo: 'Detalle',
      celda: (f) =>
        f.detalle !== '' ? (
          <p className="texto-largo">{f.detalle}</p>
        ) : (
          <span className="texto-secundario">—</span>
        ),
    },
  ];

  return (
    <Tabla
      titulo="Historial de cambios del equipo"
      clase="tabla--historial"
      claveFila={(f) => f.idLog}
      vacio={vacio}
      columnas={columnas}
      filas={entradas}
    />
  );
}
