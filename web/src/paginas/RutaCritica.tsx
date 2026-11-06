import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { detalleProyecto } from '../api/proyectos';
import { rutaCritica, tableroHito } from '../api/tareas';
import type { RutaCriticaVista, TableroHito } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Tarjeta } from '../componentes/Tarjeta';
import { decimalEsCo, fechaDia } from '../formato';
import { idDeRuta } from '../proyectos/vista';
import { rutas } from '../rutas';
import { disponerRed } from '../tablero/disposicion';
import { GrafoDependencias } from '../tablero/GrafoDependencias';
import { codigosDeTareas } from '../tablero/marcas';
import { etiquetaHito, mensajeErrorAbrir, SIN_ACCESO_HITO } from '../tablero/ubicacion';

interface DatosRuta {
  tablero: TableroHito;
  ruta: RutaCriticaVista;
  /** Orden del hito en el proyecto; null si no se pudo leer (el título queda sin «Hito N»). */
  orden: number | null;
}

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; datos: DatosRuta };

/** /hitos/:idHitoEquipo/ruta-critica: una instancia por hito (key). */
export function RutaCriticaDeRuta() {
  const { idHitoEquipo } = useParams();
  return <PaginaRutaCritica key={idHitoEquipo} idRuta={idHitoEquipo ?? ''} />;
}

/**
 * P06 Ruta crítica (Figma nodo 6:273): tres datos de resumen, la red de dependencias (26c-6) y su leyenda. El
 * tablero da nombres, fecha y códigos; el detalle del proyecto, el orden del hito.
 */
function PaginaRutaCritica({ idRuta }: { idRuta: string }) {
  const id = idDeRuta(idRuta);
  const [carga, setCarga] = useState<Carga>(() =>
    id === null ? { estado: 'error', mensaje: SIN_ACCESO_HITO } : { estado: 'cargando' },
  );

  const cargar = useCallback(async (idHitoEquipo: number) => {
    try {
      const [tablero, ruta] = await Promise.all([tableroHito(idHitoEquipo), rutaCritica(idHitoEquipo)]);
      const orden = await detalleProyecto(tablero.idProyecto).then(
        (d) => d.hitos.find((h) => h.idHito === tablero.idHito)?.orden ?? null,
        () => null,
      );
      setCarga({ estado: 'listo', datos: { tablero, ruta, orden } });
    } catch (e) {
      setCarga({ estado: 'error', mensaje: mensajeErrorAbrir(e, SIN_ACCESO_HITO) });
    }
  }, []);

  useEffect(() => {
    if (id !== null) {
      void cargar(id);
    }
  }, [cargar, id]);

  function reintentar() {
    if (id !== null) {
      setCarga({ estado: 'cargando' });
      void cargar(id);
    }
  }

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }
  if (carga.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Ruta crítica" />
        <div className="pila">
          <Alerta tipo="error">{carga.mensaje}</Alerta>
          {id !== null && (
            <div>
              <Boton variante="secundario" onClick={reintentar}>
                Reintentar
              </Boton>
            </div>
          )}
        </div>
      </div>
    );
  }
  const { tablero, ruta, orden } = carga.datos;
  const nombreHito = orden === null ? tablero.nombreHito : etiquetaHito(orden, tablero.nombreHito);
  const red = disponerRed(ruta.nodos);
  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo={`Ruta crítica · ${nombreHito}`}
        descripcion={`${tablero.nombreEquipo} · se recalcula al cambiar dependencias, estimaciones o fechas`}
      >
        <div className="pagina__acciones">
          <Link className="boton boton--secundario" to={rutas.tableroEnHito(tablero.idEquipo, tablero.idHitoEquipo)}>
            Volver al tablero
          </Link>
        </div>
      </EncabezadoPagina>
      <div className="resumen-ruta">
        <div className="dato-resumen">
          <p className="dato-resumen__etiqueta">Duración de la ruta crítica</p>
          <p className="dato-resumen__valor dato-resumen__valor--primario">{`${decimalEsCo(ruta.duracionTotalHoras)} h`}</p>
        </div>
        <div className="dato-resumen">
          <p className="dato-resumen__etiqueta">Fecha límite vigente</p>
          <p className="dato-resumen__valor">{fechaDia(tablero.fechaLimite)}</p>
        </div>
        <div className="dato-resumen">
          <p className="dato-resumen__etiqueta">Tareas en la ruta crítica</p>
          <p className="dato-resumen__valor">{`${ruta.criticas.length} de ${ruta.nodos.length}`}</p>
        </div>
      </div>
      <Tarjeta titulo="Grafo de dependencias">
        {red.nodos.length === 0 ? (
          <p className="pagina__vacio">Aún no hay tareas en este hito: la ruta crítica aparece cuando el líder crea tareas.</p>
        ) : (
          <GrafoDependencias red={red} codigos={codigosDeTareas(tablero.tareas)} />
        )}
        <div className="leyenda-ruta">
          <span className="leyenda-ruta__item">
            <span className="muestra muestra--critica" aria-hidden="true" />
            Ruta crítica (holgura 0 h)
          </span>
          <span className="leyenda-ruta__item">
            <span className="muestra muestra--holgura" aria-hidden="true" />
            Tarea con holgura
          </span>
        </div>
      </Tarjeta>
    </div>
  );
}
