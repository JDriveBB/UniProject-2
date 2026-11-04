import { NavLink, Outlet, useNavigate } from 'react-router';
import type { RolPlataforma } from '../api/tipos';
import { rutas } from '../rutas';
import { useSesion } from '../sesion/ContextoSesion';
import { Boton } from './Boton';

interface Enlace {
  a: string;
  texto: string;
  roles: RolPlataforma[];
}

const TODOS: RolPlataforma[] = ['ADMINISTRADOR', 'DOCENTE', 'ESTUDIANTE', 'MONITOR'];

const ENLACES: Enlace[] = [
  { a: rutas.proyectos(), texto: 'Proyectos', roles: ['DOCENTE', 'ESTUDIANTE', 'MONITOR'] },
  { a: rutas.misTareas(), texto: 'Mis tareas', roles: ['ESTUDIANTE'] },
  { a: rutas.adminDocentes(), texto: 'Docentes', roles: ['ADMINISTRADOR'] },
  { a: rutas.notificaciones(), texto: 'Notificaciones', roles: TODOS },
  { a: rutas.perfil(), texto: 'Perfil', roles: TODOS },
];

/** Barra superior con la navegación del rol y el contenido de la ruta. */
export function Marco() {
  const { sesion, salir } = useSesion();
  const navegar = useNavigate();
  if (sesion.estado !== 'autenticado') {
    return null;
  }
  const { usuario } = sesion;

  async function alSalir() {
    await salir();
    navegar(rutas.ingresar(), { replace: true });
  }

  return (
    <div className="marco">
      <header className="marco__barra">
        <span className="marco__marca">UniProject</span>
        <nav aria-label="Principal" className="marco__nav">
          {ENLACES.filter((e) => e.roles.includes(usuario.rol)).map((e) => (
            <NavLink
              key={e.a}
              to={e.a}
              className={({ isActive }) => (isActive ? 'marco__enlace marco__enlace--activo' : 'marco__enlace')}
            >
              {e.texto}
            </NavLink>
          ))}
        </nav>
        <span className="marco__usuario">{usuario.nombre}</span>
        <Boton variante="secundario" onClick={() => void alSalir()}>
          Salir
        </Boton>
      </header>
      <main className="marco__contenido">
        <Outlet />
      </main>
    </div>
  );
}
