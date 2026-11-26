import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { EstadoVacio } from '../componentes/EstadoVacio';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';
import { equiposDelUsuario, hitoActual, hitosDelEquipo, rolEnEquipo } from '../tablero/ubicacion';
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
        <EstadoVacio
          titulo="Aún no tienes equipo"
          accion={
            <Link className="boton boton--primario" to={rutas.misCursos()}>
              Ir a Mis cursos
            </Link>
          }
        >
          Elige uno desde Mis cursos mientras la inscripción esté abierta, o espera a que tu docente te asigne.
        </EstadoVacio>
      )}
      {busqueda.estado === 'listo' && busqueda.equipos.length === 1 && (
        <EstadoVacio titulo="Tu proyecto aún no tiene hitos">
          Cuando tu docente los configure en el proyecto, los verás aquí.
        </EstadoVacio>
      )}
      {busqueda.estado === 'listo' && busqueda.equipos.length > 1 && (
        <div className="rejilla-proyectos">
          {busqueda.equipos.map((u) => {
            const a = enlaceDe(destino, u);
            const esLider = rolEnEquipo(u.equipo, usuario.idUsuario) === 'LIDER';
            const textoBoton = destino === 'tablero' ? 'Abrir tablero' : 'Abrir ruta crítica';
            return (
              <div key={u.equipo.idEquipo} className="tarjeta-proyecto">
                <Tarjeta>
                  <header className="tarjeta-proyecto__cabecera">
                    <div className="tarjeta-proyecto__encabezado">
                      <h2 className="tarjeta__titulo tarjeta-proyecto__titulo">{u.equipo.nombreEquipo}</h2>
                      <p className="texto-secundario tarjeta-proyecto__dato">{u.proyecto.nombre}</p>
                    </div>
                    {esLider && <Insignia tono="info">Líder</Insignia>}
                  </header>
                  {a === null ? (
                    <p className="texto-secundario">El proyecto aún no tiene hitos.</p>
                  ) : (
                    <div className="pagina__acciones tarjeta-proyecto__acciones">
                      <Link
                        className="boton boton--primario"
                        to={a}
                        aria-label={`${textoBoton} de ${u.equipo.nombreEquipo}, ${u.proyecto.nombre}`}
                      >
                        {textoBoton}
                      </Link>
                    </div>
                  )}
                </Tarjeta>
              </div>
            );
          })}
        </div>
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
