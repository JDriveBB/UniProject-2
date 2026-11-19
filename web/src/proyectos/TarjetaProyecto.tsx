import { Link } from 'react-router';
import type { ProyectoVista } from '../api/tipos';
import { BarraAvance } from '../componentes/BarraAvance';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { rangoFechas } from '../formato';
import type { UrgenciaEntrega } from '../panel/vista';
import { rutas } from '../rutas';
import type { Marca } from '../tablero/marcas';
import { estadoProyecto, type ResumenDocente, type ResumenEstudiante, type ResumenProyecto } from './tarjetas';

function marcaCabecera(resumen: ResumenProyecto | undefined): Marca | null {
  if (resumen === undefined) {
    return null;
  }
  if (resumen.tipo === 'docente') {
    return estadoProyecto(resumen.resumen);
  }
  if (resumen.tipo === 'estudiante') {
    return resumen.resumen.hito?.marca ?? null;
  }
  return null;
}

function clasePlazo(urgencia: UrgenciaEntrega): string {
  if (urgencia === 'normal') {
    return 'entrega-hito__plazo';
  }
  return `entrega-hito__plazo entrega-hito__plazo--${urgencia}`;
}

function FilaAvance({ etiqueta, avance }: { etiqueta: string; avance: number | null }) {
  return (
    <div className="tarjeta-proyecto__fila">
      <span className="tarjeta-proyecto__etiqueta">{etiqueta}</span>
      {avance !== null ? <BarraAvance porcentaje={avance} /> : <span className="texto-secundario">Sin tareas</span>}
    </div>
  );
}

function CuerpoDocente({ resumen }: { resumen: ResumenDocente }) {
  return (
    <>
      <dl className="tarjeta-proyecto__cifras">
        <div className="tarjeta-proyecto__cifra">
          <dt className="tarjeta-proyecto__etiqueta">Equipos</dt>
          <dd className="tarjeta-proyecto__valor">{resumen.equipos}</dd>
        </div>
        <div className="tarjeta-proyecto__cifra">
          <dt className="tarjeta-proyecto__etiqueta">Hitos</dt>
          <dd className="tarjeta-proyecto__valor">{resumen.hitos}</dd>
        </div>
        <div className="tarjeta-proyecto__cifra">
          <dt className="tarjeta-proyecto__etiqueta">Por calificar</dt>
          <dd className="tarjeta-proyecto__valor">{resumen.porCalificar}</dd>
        </div>
      </dl>
      <FilaAvance etiqueta="Avance promedio" avance={resumen.avance} />
      <p className="tarjeta-proyecto__dato">
        <span className="tarjeta-proyecto__etiqueta">Próxima entrega:</span>{' '}
        {resumen.proxima !== null ? (
          <>
            <span>{resumen.proxima.etiqueta}</span>{' '}
            <span className={clasePlazo(resumen.proxima.urgencia)}>{`— ${resumen.proxima.plazo}`}</span>
          </>
        ) : (
          <span className="texto-secundario">Sin entregas pendientes</span>
        )}
      </p>
    </>
  );
}

function CuerpoEstudiante({ resumen }: { resumen: ResumenEstudiante }) {
  return (
    <>
      <div className="tarjeta-proyecto__equipo">
        <span className="tarjeta-proyecto__nombre-equipo">{resumen.equipo.nombreEquipo}</span>
        {resumen.esLider && <Insignia tono="info">Líder</Insignia>}
      </div>
      <p className="tarjeta-proyecto__dato">
        <span className="tarjeta-proyecto__etiqueta">Hito actual:</span>{' '}
        {resumen.hito !== null ? (
          <>
            <span>{resumen.hito.etiqueta}</span>
            {resumen.hito.plazo !== null && <span className="texto-secundario">{` — ${resumen.hito.plazo}`}</span>}
          </>
        ) : (
          <span className="texto-secundario">Sin hitos</span>
        )}
      </p>
      <FilaAvance etiqueta="Avance del equipo" avance={resumen.avance} />
    </>
  );
}

function AccionesTarjeta({
  proyecto,
  edita,
  resumen,
}: {
  proyecto: ProyectoVista;
  edita: boolean;
  resumen: ResumenProyecto | undefined;
}) {
  if (edita) {
    return (
      <div className="pagina__acciones tarjeta-proyecto__acciones">
        <Link
          className="boton boton--primario"
          to={rutas.panelProyecto(proyecto.idProyecto)}
          aria-label={`Abrir panel de ${proyecto.nombre}`}
        >
          Abrir panel
        </Link>
        <Link
          className="boton boton--secundario"
          to={rutas.proyecto(proyecto.idProyecto)}
          aria-label={`Configurar ${proyecto.nombre}`}
        >
          Configurar
        </Link>
      </div>
    );
  }

  return (
    <div className="pagina__acciones tarjeta-proyecto__acciones">
      {resumen?.tipo === 'estudiante' && (
        <Link
          className="boton boton--primario"
          to={
            resumen.resumen.hito !== null
              ? rutas.tableroEnHito(resumen.resumen.equipo.idEquipo, resumen.resumen.hito.idHitoEquipo)
              : rutas.tableroEquipo(resumen.resumen.equipo.idEquipo)
          }
          aria-label={`Ver tablero de ${proyecto.nombre}`}
        >
          Ver tablero
        </Link>
      )}
      <Link
        className="boton boton--secundario"
        to={rutas.proyecto(proyecto.idProyecto)}
        aria-label={`Ver proyecto ${proyecto.nombre}`}
      >
        Ver proyecto
      </Link>
    </div>
  );
}

/** Tarjeta de proyecto para /proyectos y el selector de /panel, sin envolver la tarjeta completa en enlace. */
export function TarjetaProyecto({
  proyecto,
  edita,
  resumen,
}: {
  proyecto: ProyectoVista;
  edita: boolean;
  resumen: ResumenProyecto | undefined;
}) {
  const insignia = marcaCabecera(resumen);

  return (
    <div className="tarjeta-proyecto">
      <Tarjeta>
        <header className="tarjeta-proyecto__cabecera">
          <div className="tarjeta-proyecto__encabezado">
            <h2 className="tarjeta__titulo tarjeta-proyecto__titulo">{proyecto.nombre}</h2>
            <p className="texto-secundario">{rangoFechas(proyecto.fechaInicio, proyecto.fechaFin)}</p>
          </div>
          {insignia !== null && <Insignia tono={insignia.tono}>{insignia.texto}</Insignia>}
        </header>
        {proyecto.descripcion !== null && proyecto.descripcion !== '' && (
          <p className="tarjeta-proyecto__descripcion">{proyecto.descripcion}</p>
        )}
        {resumen === undefined && <p className="texto-secundario">Cargando datos…</p>}
        {resumen?.tipo === 'error' && (
          <p className="texto-secundario">No se pudieron cargar los datos de este proyecto.</p>
        )}
        {resumen?.tipo === 'docente' && <CuerpoDocente resumen={resumen.resumen} />}
        {resumen?.tipo === 'estudiante' && <CuerpoEstudiante resumen={resumen.resumen} />}
        <AccionesTarjeta proyecto={proyecto} edita={edita} resumen={resumen} />
      </Tarjeta>
    </div>
  );
}
