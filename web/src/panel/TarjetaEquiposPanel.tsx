import { Link } from 'react-router';
import type { PanelEquipo } from '../api/tipos';
import { Ayuda } from '../componentes/Ayuda';
import { EXPLICACION_ESTADO } from '../componentes/ayudas';
import { BarraAvance } from '../componentes/BarraAvance';
import { Insignia } from '../componentes/Insignia';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { Tarjeta } from '../componentes/Tarjeta';
import { rutas } from '../rutas';
import { etiquetaHito } from '../tablero/ubicacion';
import { EXPLICACION_AVANCE_HITO, estadoEquipo } from './vista';

/** D-12: avance del hito en curso (ponderado por horas) con la etiqueta del hito debajo. */
function AvanceDelHito({ equipo }: { equipo: PanelEquipo }) {
  if (equipo.todoEnviado === true) {
    return <span className="texto-secundario">Todo enviado</span>;
  }
  const hito = equipo.hitoActual ?? null;
  const etiqueta =
    hito === null ? null : <div className="texto-secundario">{etiquetaHito(hito.orden, hito.nombreHito)}</div>;
  if (equipo.avanceHitoActual == null) {
    return (
      <>
        <div className="texto-secundario">Sin tareas</div>
        {etiqueta}
      </>
    );
  }
  return (
    <>
      <div>
        <BarraAvance porcentaje={equipo.avanceHitoActual} />
      </div>
      {etiqueta}
    </>
  );
}

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
      titulo: 'Avance del hito',
      clase: 'tabla__col-avance',
      celda: (e) => <AvanceDelHito equipo={e} />,
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
    <Tarjeta
      titulo="Equipos"
      acciones={<Ayuda termino="Equipos">{`${EXPLICACION_ESTADO} ${EXPLICACION_AVANCE_HITO}`}</Ayuda>}
    >
      <Tabla
        titulo="Avance y riesgo por equipo"
        clase="tabla--panel tabla--panel-equipos"
        claveFila={(e) => e.idEquipo}
        vacio="El proyecto aún no tiene equipos."
        columnas={columnas}
        filas={equipos}
      />
    </Tarjeta>
  );
}
