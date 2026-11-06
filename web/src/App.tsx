import { Navigate, Route, Routes } from 'react-router';
import { Marco } from './componentes/Marco';
import { AdminDocentes } from './paginas/AdminDocentes';
import { NuevoProyecto, ProyectoDeRuta } from './paginas/ConfigurarProyecto';
import { Ingreso } from './paginas/Ingreso';
import { ListaProyectos } from './paginas/ListaProyectos';
import { MiRutaCritica, MiTablero } from './paginas/MiEquipo';
import { NoEncontrada } from './paginas/NoEncontrada';
import { PaginaPendiente } from './paginas/PaginaPendiente';
import { Registro } from './paginas/Registro';
import { RutaCriticaDeRuta } from './paginas/RutaCritica';
import { TableroDeRuta } from './paginas/Tablero';
import { rutas } from './rutas';
import { useSesion } from './sesion/ContextoSesion';
import { RutaProtegida } from './sesion/RutaProtegida';

/** "/" lleva a la pantalla principal de cada rol. */
function Inicio() {
  const { sesion } = useSesion();
  if (sesion.estado !== 'autenticado') {
    return null;
  }
  switch (sesion.usuario.rol) {
    case 'ADMINISTRADOR':
      return <Navigate to={rutas.adminDocentes()} replace />;
    case 'ESTUDIANTE':
      return <Navigate to={rutas.misTareas()} replace />;
    default:
      return <Navigate to={rutas.proyectos()} replace />;
  }
}

export function App() {
  return (
    <Routes>
      <Route path="/ingresar" element={<Ingreso />} />
      <Route path="/registro" element={<Registro />} />
      <Route element={<RutaProtegida />}>
        <Route element={<Marco />}>
          <Route path="/" element={<Inicio />} />
          <Route path="/proyectos" element={<ListaProyectos />} />
          <Route path="/proyectos/:idProyecto" element={<ProyectoDeRuta />} />
          <Route path="/equipos/:idEquipo/tablero" element={<TableroDeRuta />} />
          <Route path="/equipos/:idEquipo/historial" element={<PaginaPendiente titulo="Historial del equipo" bloque="26f" />} />
          <Route path="/hitos/:idHitoEquipo/ruta-critica" element={<RutaCriticaDeRuta />} />
          <Route path="/tareas/:idTarea" element={<PaginaPendiente titulo="Tarea" bloque="26d" />} />
          <Route path="/mis-tareas" element={<PaginaPendiente titulo="Mis tareas" bloque="26d" />} />
          <Route path="/hitos/:idHitoEquipo/evaluacion" element={<PaginaPendiente titulo="Evaluación del hito" bloque="26e" />} />
          <Route path="/notificaciones" element={<PaginaPendiente titulo="Notificaciones" bloque="26f" />} />
          <Route path="/perfil" element={<PaginaPendiente titulo="Perfil" bloque="26f" />} />
          <Route element={<RutaProtegida roles={['DOCENTE']} />}>
            <Route path="/proyectos/nuevo" element={<NuevoProyecto />} />
            <Route path="/panel" element={<PaginaPendiente titulo="Panel del docente" bloque="26f" />} />
            <Route path="/reportes" element={<PaginaPendiente titulo="Reportes" bloque="26f" />} />
            <Route path="/proyectos/:idProyecto/panel" element={<PaginaPendiente titulo="Panel del docente" bloque="26f" />} />
            <Route path="/hitos/:idHitoEquipo/evaluar" element={<PaginaPendiente titulo="Evaluar hito" bloque="26e" />} />
          </Route>
          <Route element={<RutaProtegida roles={['ESTUDIANTE']} />}>
            <Route path="/tablero" element={<MiTablero />} />
            <Route path="/ruta-critica" element={<MiRutaCritica />} />
          </Route>
          <Route element={<RutaProtegida roles={['ADMINISTRADOR']} />}>
            <Route path="/admin/docentes" element={<AdminDocentes />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}
