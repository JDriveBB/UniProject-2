import type { EntradaHistorial } from '../api/tipos';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { fechaHora } from '../formato';
import { detalleLegible, type Visor } from '../historial/vista';
import { accionConEntidad } from './vista';

export interface PropsTablaHistorial {
  entradas: EntradaHistorial[];
  nombres: ReadonlyMap<number, string>;
  visor: Visor;
  vacio: string;
}

export function TablaHistorial({ entradas, nombres, visor, vacio }: PropsTablaHistorial) {
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
      celda: (f) => {
        const texto = detalleLegible(f, nombres, visor) ?? (f.detalle !== '' ? f.detalle : null);
        return texto !== null ? <p className="texto-largo">{texto}</p> : <span className="texto-secundario">—</span>;
      },
    },
  ];

  return (
    <Tabla
      titulo="Historial de cambios del equipo"
      clase="tabla--historial tabla--historial-reporte"
      claveFila={(f) => f.idLog}
      vacio={vacio}
      columnas={columnas}
      filas={entradas}
    />
  );
}
