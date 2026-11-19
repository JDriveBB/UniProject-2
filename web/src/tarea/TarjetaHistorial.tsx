import type { AvanceVista } from '../api/tipos';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaHora } from '../formato';

export interface PropsTarjetaHistorial {
  avances: AvanceVista[];
}

export function TarjetaHistorial({ avances }: PropsTarjetaHistorial) {
  const columnas: ColumnaTabla<AvanceVista>[] = [
    {
      titulo: 'Fecha',
      clase: 'tabla__col-fecha-hora',
      celda: (f) => fechaHora(f.fechaHora),
    },
    {
      titulo: 'Registrado por',
      clase: 'tabla__col-autor',
      celda: (f) => f.usuario,
    },
    {
      titulo: 'Avance',
      clase: 'tabla__col-avance-historial',
      celda: (f) => `${f.porcentajeAvance} %`,
    },
    {
      titulo: 'Observación',
      celda: (f) =>
        f.observacion !== null && f.observacion !== '' ? f.observacion : <span className="texto-secundario">—</span>,
    },
  ];

  return (
    <Tarjeta titulo="Historial de avances">
      <Tabla
        titulo="Historial de avances de la tarea"
        clase="tabla--historial tabla--avances"
        claveFila={(f) => f.idRegistro}
        vacio="Aún no hay avances registrados."
        columnas={columnas}
        filas={avances}
      />
    </Tarjeta>
  );
}
