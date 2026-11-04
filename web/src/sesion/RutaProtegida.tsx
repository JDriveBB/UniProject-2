import { Navigate, Outlet, useLocation } from 'react-router';
import type { RolPlataforma } from '../api/tipos';
import { Cargando } from '../componentes/Cargando';
import { SinPermiso } from '../paginas/SinPermiso';
import { rutas } from '../rutas';
import { useSesion } from './ContextoSesion';

/** Sin sesión lleva a /ingresar recordando la ruta; con otro rol muestra "sin permiso" (la API igual responde 403). */
export function RutaProtegida({ roles }: { roles?: RolPlataforma[] }) {
  const { sesion } = useSesion();
  const ubicacion = useLocation();
  if (sesion.estado === 'cargando') {
    return <Cargando />;
  }
  if (sesion.estado === 'anonimo') {
    return <Navigate to={rutas.ingresar()} replace state={{ desde: ubicacion.pathname + ubicacion.search }} />;
  }
  if (roles !== undefined && !roles.includes(sesion.usuario.rol)) {
    return <SinPermiso />;
  }
  return <Outlet />;
}
