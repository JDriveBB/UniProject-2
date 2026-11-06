import { Link } from 'react-router';
import { Insignia } from '../componentes/Insignia';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { Tarjeta } from '../componentes/Tarjeta';
import { rutas } from '../rutas';
import { EXPLICACION_VENCIDO, type FilaHitos, type MatrizHitos } from './vista';

export function TarjetaHitosPanel({ matriz }: { matriz: MatrizHitos }) {
  if (matriz.columnas.length === 0) {
    return (
      <Tarjeta titulo="Hitos por equipo">
        <p className="pagina__vacio">El proyecto aún no tiene hitos.</p>
      </Tarjeta>
    );
  }

  const columnas: ColumnaTabla<FilaHitos>[] = [
    {
      titulo: 'Equipo',
      celda: (f) => (
        <Link className="enlace-fuerte" to={rutas.tableroEquipo(f.idEquipo)}>
          {f.nombreEquipo}
        </Link>
      ),
    },
    ...matriz.columnas.map((col, i): ColumnaTabla<FilaHitos> => ({
      titulo: col.etiqueta,
      celda: (f) => {
        const celda = f.celdas[i];
        if (celda === null) {
          return <span className="texto-secundario">—</span>;
        }
        return <Insignia tono={celda.tono}>{celda.texto}</Insignia>;
      },
    })),
  ];

  return (
    <Tarjeta titulo="Hitos por equipo">
      <Tabla
        titulo="Estado de cada hito por equipo"
        clase="tabla--panel"
        claveFila={(f) => f.idEquipo}
        vacio="El proyecto aún no tiene equipos."
        columnas={columnas}
        filas={matriz.filas}
      />
      <p className="texto-ayuda">{EXPLICACION_VENCIDO}</p>
    </Tarjeta>
  );
}
