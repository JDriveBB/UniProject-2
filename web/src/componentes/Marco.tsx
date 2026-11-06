import { useEffect, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router';
import { bandeja } from '../api/notificaciones';
import { detalleProyecto, listarProyectos } from '../api/proyectos';
import { enlacesDe, esLiderDeAlgunEquipo, etiquetaRol, textoContador } from '../marco/navegacion';
import { rutas } from '../rutas';
import { useSesion } from '../sesion/ContextoSesion';
import { equiposDelUsuario } from '../tablero/ubicacion';

/**
 * Barra superior azul marino de Figma (P02/P03/P04, decisión 26b-1) y el contenido de la ruta. El contador sale de
 * GET /api/notificaciones (Bandeja.noLeidas) al abrir cada pantalla (26b-3); si la llamada falla, no se muestra.
 * El rol "Líder de equipo" sale de los equipos del estudiante, una vez por sesión (26d-12).
 */
export function Marco() {
  const { sesion, salir } = useSesion();
  const navegar = useNavigate();
  const { pathname } = useLocation();
  const [noLeidas, setNoLeidas] = useState(0);
  const [liderazgo, setLiderazgo] = useState<{ idUsuario: number; lider: boolean } | null>(null);
  const autenticado = sesion.estado === 'autenticado';
  const idEstudiante = autenticado && sesion.usuario.rol === 'ESTUDIANTE' ? sesion.usuario.idUsuario : null;

  useEffect(() => {
    if (!autenticado) {
      return;
    }
    let vigente = true;
    bandeja()
      .then((b) => {
        if (vigente) {
          setNoLeidas(b.noLeidas);
        }
      })
      .catch(() => {
        if (vigente) {
          setNoLeidas(0);
        }
      });
    return () => {
      vigente = false;
    };
  }, [autenticado, pathname]);

  useEffect(() => {
    if (idEstudiante === null) {
      return;
    }
    let vigente = true;
    equiposDelUsuario({ listarProyectos, detalleProyecto }, idEstudiante)
      .then((lista) => {
        if (vigente) {
          setLiderazgo({
            idUsuario: idEstudiante,
            lider: esLiderDeAlgunEquipo(lista.map((u) => u.equipo), idEstudiante),
          });
        }
      })
      .catch(() => {
        if (vigente) {
          setLiderazgo({ idUsuario: idEstudiante, lider: false });
        }
      });
    return () => {
      vigente = false;
    };
  }, [idEstudiante]);

  if (sesion.estado !== 'autenticado') {
    return null;
  }
  const { usuario } = sesion;
  const contador = textoContador(noLeidas);
  const enNotificaciones = pathname === rutas.notificaciones();

  async function alSalir() {
    await salir();
    navegar(rutas.ingresar(), { replace: true });
  }

  return (
    <div className="marco">
      <header className="barra">
        <div className="barra__izquierda">
          <Link to="/" className="barra__marca">
            UniProject
          </Link>
          <nav aria-label="Principal" className="barra__nav">
            {enlacesDe(usuario.rol, pathname).map((e) => (
              <Link
                key={e.texto}
                to={e.a}
                className={e.activo ? 'barra__enlace barra__enlace--activo' : 'barra__enlace'}
                aria-current={e.activo ? 'page' : undefined}
              >
                {e.texto}
              </Link>
            ))}
          </nav>
        </div>
        <div className="barra__derecha">
          <Link
            to={rutas.notificaciones()}
            className={enNotificaciones ? 'barra__enlace barra__enlace--activo' : 'barra__enlace'}
            aria-current={enNotificaciones ? 'page' : undefined}
          >
            Notificaciones
            {contador !== null && (
              <>
                <span className="barra__contador" aria-hidden="true">
                  {contador}
                </span>
                <span className="solo-lectores">, {noLeidas} sin leer</span>
              </>
            )}
          </Link>
          <Link to={rutas.perfil()} className="barra__usuario">
            <span className="barra__nombre">{usuario.nombre}</span>
            <span className="barra__rol">
              {etiquetaRol(usuario.rol, liderazgo !== null && liderazgo.idUsuario === usuario.idUsuario && liderazgo.lider)}
            </span>
          </Link>
          <button type="button" className="barra__salir" onClick={() => void alSalir()}>
            Salir
          </button>
        </div>
      </header>
      <main className="marco__contenido">
        <Outlet />
      </main>
    </div>
  );
}
