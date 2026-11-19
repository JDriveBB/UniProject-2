import { Link } from 'react-router';
import type { PanelEquipo } from '../api/tipos';
import { BarraAvance } from '../componentes/BarraAvance';
import { Insignia } from '../componentes/Insignia';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { Tarjeta } from '../componentes/Tarjeta';
import { rutas } from '../rutas';
import { EXPLICACION_ESTADO, estadoEquipo } from './vista';

export function TarjetaEquiposPanel({ equipos }: { equipos: PanelEquipo[] }) {
  const columnas: ColumnaTabla<PanelEquipo>[] = [
    {
      titulo: 'Equipo',
      celda: (e) => (
        <Link className="enlace-fuerte" to={rutas.tableroEquipo(e.idEquipo)}>
          {e.nombreEquipo}
        </Link>
      ),
    },
    {
      titulo: 'Estado',
      clase: 'tabla__col-estado',
      celda: (e) => {
        const marca = estadoEquipo(e);
        return <Insignia tono={marca.tono}>{marca.texto}</Insignia>;
      },
    },
    {
      titulo: 'Avance',
      clase: 'tabla__col-avance',
      celda: (e) => <BarraAvance porcentaje={e.avancePromedio} />,
    },
    {
      titulo: 'Completadas',
      clase: 'tabla__col-numero',
      celda: (e) => `${e.tareasCompletadas} de ${e.tareasTotales}`,
    },
    {
      titulo: 'Riesgo alto',
      clase: 'tabla__col-numero',
      celda: (e) => String(e.tareasRiesgoAlto),
    },
    {
      titulo: 'Historial',
      clase: 'tabla__col-historial',
      celda: (e) => (
        <Link
          className="enlace-fuerte"
          to={rutas.historialEquipo(e.idEquipo)}
          aria-label={`Historial de ${e.nombreEquipo}`}
        >
          Ver historial
        </Link>
      ),
    },
  ];

  return (
    <Tarjeta titulo="Equipos">
      <Tabla
        titulo="Avance y riesgo por equipo"
        clase="tabla--panel tabla--panel-equipos"
        claveFila={(e) => e.idEquipo}
        vacio="El proyecto aún no tiene equipos."
        columnas={columnas}
        filas={equipos}
      />
      <p className="texto-ayuda">{EXPLICACION_ESTADO}</p>
    </Tarjeta>
  );
}
