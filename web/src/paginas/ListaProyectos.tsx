import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { listarProyectos } from '../api/proyectos';
import type { ProyectoVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Tarjeta } from '../componentes/Tarjeta';
import { rangoFechas } from '../formato';
import { modoDeVista } from '../proyectos/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';

type EstadoLista =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; proyectos: ProyectoVista[] };

/** /proyectos. SIN FIGMA: diseño provisional con las tarjetas del sistema (decisión 26b-4). */
export function ListaProyectos() {
  const usuario = useUsuario();
  const [lista, setLista] = useState<EstadoLista>({ estado: 'cargando' });
  const esDocente = usuario.rol === 'DOCENTE';

  const cargar = useCallback(async () => {
    try {
      setLista({ estado: 'listo', proyectos: await listarProyectos() });
    } catch (e) {
      setLista({ estado: 'error', mensaje: mensajeDeError(e) });
    }
  }, []);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  function reintentar() {
    setLista({ estado: 'cargando' });
    void cargar();
  }

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo="Proyectos"
        descripcion={esDocente ? 'Crea un proyecto o abre uno para configurarlo.' : 'Proyectos en los que participas.'}
      >
        {esDocente && (
          <div className="pagina__acciones">
            <Link className="boton boton--primario" to={rutas.proyectoNuevo()}>
              Nuevo proyecto
            </Link>
          </div>
        )}
      </EncabezadoPagina>
      {lista.estado === 'cargando' && <Cargando />}
      {lista.estado === 'error' && (
        <div className="pila">
          <Alerta tipo="error">{lista.mensaje}</Alerta>
          <div>
            <Boton variante="secundario" onClick={reintentar}>
              Reintentar
            </Boton>
          </div>
        </div>
      )}
      {lista.estado === 'listo' && lista.proyectos.length === 0 && (
        <p className="pagina__vacio">
          {esDocente
            ? 'Aún no tienes proyectos. Crea el primero con «Nuevo proyecto».'
            : 'Aún no participas en ningún proyecto.'}
        </p>
      )}
      {lista.estado === 'listo' && lista.proyectos.length > 0 && (
        <div className="rejilla-proyectos">
          {lista.proyectos.map((p) => {
            const edita = modoDeVista(usuario, p) === 'edicion';
            return (
              <Tarjeta key={p.idProyecto} titulo={p.nombre}>
                <p className="texto-secundario">{rangoFechas(p.fechaInicio, p.fechaFin)}</p>
                {p.descripcion !== null && <p className="tarjeta-proyecto__descripcion">{p.descripcion}</p>}
                <div className="pagina__acciones">
                  <Link className="boton boton--secundario" to={rutas.proyecto(p.idProyecto)}>
                    {edita ? 'Configurar' : 'Ver proyecto'}
                  </Link>
                  {edita && (
                    <Link className="boton boton--secundario" to={rutas.panelProyecto(p.idProyecto)}>
                      Panel
                    </Link>
                  )}
                </div>
              </Tarjeta>
            );
          })}
        </div>
      )}
    </div>
  );
}
