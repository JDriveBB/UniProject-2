import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { panelProyecto } from '../api/panel';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import { tableroHito } from '../api/tareas';
import type { ProyectoVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { fechaIsoBogota } from '../formato';
import { TarjetaProyecto } from '../proyectos/TarjetaProyecto';
import { cargarResumenes, type ApiResumenes, type ResumenProyecto } from '../proyectos/tarjetas';
import { modoDeVista } from '../proyectos/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';

const API_RESUMENES: ApiResumenes = { panelProyecto, detalleProyecto, tableroHito };

type EstadoLista =
  { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; proyectos: ProyectoVista[] };

/** /proyectos. SIN FIGMA: tarjetas de proyecto con detalle para docente y estudiante. */
export function ListaProyectos() {
  const usuario = useUsuario();
  const [lista, setLista] = useState<EstadoLista>({ estado: 'cargando' });
  const [resumenes, setResumenes] = useState<Map<number, ResumenProyecto> | null>(null);
  const [vuelta, setVuelta] = useState(0);
  const esDocente = usuario.rol === 'DOCENTE';

  useEffect(() => {
    let vigente = true;
    async function cargar() {
      let proyectos: ProyectoVista[];
      try {
        proyectos = await listarProyectos();
      } catch (e) {
        if (vigente) {
          setLista({ estado: 'error', mensaje: mensajeDeError(e) });
        }
        return;
      }
      if (!vigente) {
        return;
      }
      setLista({ estado: 'listo', proyectos });
      if (proyectos.length === 0) {
        setResumenes(new Map());
        return;
      }
      const mapa = await cargarResumenes(
        API_RESUMENES,
        proyectos,
        { idUsuario: usuario.idUsuario, rol: usuario.rol },
        fechaIsoBogota(new Date()),
      );
      if (vigente) {
        setResumenes(mapa);
      }
    }
    void cargar();
    return () => {
      vigente = false;
    };
  }, [usuario.idUsuario, usuario.rol, vuelta]);

  function reintentar() {
    setLista({ estado: 'cargando' });
    setResumenes(null);
    setVuelta((v) => v + 1);
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
          {lista.proyectos.map((p) => (
            <TarjetaProyecto
              key={p.idProyecto}
              proyecto={p}
              edita={modoDeVista(usuario, p) === 'edicion'}
              resumen={resumenes?.get(p.idProyecto)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
