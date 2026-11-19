import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { EstadoVacio } from '../componentes/EstadoVacio';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';
import { equiposDelUsuario, hitoActual, hitosDelEquipo } from '../tablero/ubicacion';
import type { ApiUbicacion, EquipoUbicado } from '../tablero/ubicacion';

const API_UBICACION: ApiUbicacion = { listarProyectos, detalleProyecto };

type Busqueda =
  { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; equipos: EquipoUbicado[] };

type Destino = 'tablero' | 'rutaCritica';

/** Enlace del equipo: su tablero, o la ruta crítica de su hito actual (null si el proyecto no tiene hitos). */
function enlaceDe(destino: Destino, u: EquipoUbicado): string | null {
  if (destino === 'tablero') {
    return rutas.tableroEquipo(u.equipo.idEquipo);
  }
  const hito = hitoActual(hitosDelEquipo(u));
  return hito === null ? null : rutas.rutaCritica(hito.idHitoEquipo);
}

/**
 * /tablero y /ruta-critica de la barra del estudiante (decisión 26c-1): con un equipo, lleva directo; sin equipo o
 * con varios (varios proyectos), lo dice o deja elegir (SIN FIGMA, provisional).
 */
function MiEquipo({ destino }: { destino: Destino }) {
  const usuario = useUsuario();
  const [busqueda, setBusqueda] = useState<Busqueda>({ estado: 'cargando' });

  const buscar = useCallback(async () => {
    try {
      setBusqueda({ estado: 'listo', equipos: await equiposDelUsuario(API_UBICACION, usuario.idUsuario) });
    } catch (e) {
      setBusqueda({ estado: 'error', mensaje: mensajeDeError(e) });
    }
  }, [usuario.idUsuario]);

  useEffect(() => {
    void buscar();
  }, [buscar]);

  function reintentar() {
    setBusqueda({ estado: 'cargando' });
    void buscar();
  }

  if (busqueda.estado === 'cargando') {
    return <Cargando />;
  }
  if (busqueda.estado === 'listo' && busqueda.equipos.length === 1) {
    const unico = enlaceDe(destino, busqueda.equipos[0]);
    if (unico !== null) {
      return <Navigate to={unico} replace />;
    }
  }
  const varios = busqueda.estado === 'listo' && busqueda.equipos.length > 1;
  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo={destino === 'tablero' ? 'Tablero' : 'Ruta crítica'}
        descripcion={varios ? 'Participas en varios equipos. Elige uno.' : undefined}
      />
      {busqueda.estado === 'error' && (
        <div className="pila">
          <Alerta tipo="error">{busqueda.mensaje}</Alerta>
          <div>
            <Boton variante="secundario" onClick={reintentar}>
              Reintentar
            </Boton>
          </div>
        </div>
      )}
      {busqueda.estado === 'listo' && busqueda.equipos.length === 0 && (
        <EstadoVacio titulo="Aún no tienes equipo">Tu docente te agrega a uno desde el proyecto.</EstadoVacio>
      )}
      {busqueda.estado === 'listo' && busqueda.equipos.length === 1 && (
        <EstadoVacio titulo="Tu proyecto aún no tiene hitos">
          Cuando tu docente los configure en el proyecto, los verás aquí.
        </EstadoVacio>
      )}
      {busqueda.estado === 'listo' && busqueda.equipos.length > 1 && (
        <ul className="lista-simple">
          {busqueda.equipos.map((u) => {
            const a = enlaceDe(destino, u);
            const texto = `${u.equipo.nombreEquipo} · ${u.proyecto.nombre}`;
            return (
              <li key={u.equipo.idEquipo}>
                {a === null ? (
                  <span className="texto-secundario">{`${texto} (sin hitos)`}</span>
                ) : (
                  <Link className="enlace-fuerte" to={a}>
                    {texto}
                  </Link>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

export function MiTablero() {
  return <MiEquipo destino="tablero" />;
}

export function MiRutaCritica() {
  return <MiEquipo destino="rutaCritica" />;
}
