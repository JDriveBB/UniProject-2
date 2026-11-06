import { Navigate, Route, Routes } from 'react-router';
import { Marco } from './componentes/Marco';
import { AdminDocentes } from './paginas/AdminDocentes';
import { NuevoProyecto, ProyectoDeRuta } from './paginas/ConfigurarProyecto';
import { DetalleTareaDeRuta } from './paginas/DetalleTarea';
import { EvaluacionHitoDeRuta } from './paginas/EvaluacionHito';
import { EvaluarHitoDeRuta } from './paginas/EvaluarHito';
import { Ingreso } from './paginas/Ingreso';
import { ListaProyectos } from './paginas/ListaProyectos';
import { MiRutaCritica, MiTablero } from './paginas/MiEquipo';
import { MisTareas } from './paginas/MisTareas';
import { NoEncontrada } from './paginas/NoEncontrada';
import { OlvideContrasena } from './paginas/OlvideContrasena';
import { PaginaPendiente } from './paginas/PaginaPendiente';
import { ElegirPanel, PanelProyectoDeRuta } from './paginas/PanelDocente';
import { Registro } from './paginas/Registro';
import { RestablecerContrasena } from './paginas/RestablecerContrasena';
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
      <Route path="/olvide-contrasena" element={<OlvideContrasena />} />
      <Route path="/restablecer-contrasena" element={<RestablecerContrasena />} />
      <Route element={<RutaProtegida />}>
        <Route element={<Marco />}>
          <Route path="/" element={<Inicio />} />
          <Route path="/proyectos" element={<ListaProyectos />} />
          <Route path="/proyectos/:idProyecto" element={<ProyectoDeRuta />} />
          <Route path="/equipos/:idEquipo/tablero" element={<TableroDeRuta />} />
          <Route path="/equipos/:idEquipo/historial" element={<PaginaPendiente titulo="Historial del equipo" bloque="26f" />} />
          <Route path="/hitos/:idHitoEquipo/ruta-critica" element={<RutaCriticaDeRuta />} />
          <Route path="/tareas/:idTarea" element={<DetalleTareaDeRuta />} />
          <Route path="/mis-tareas" element={<MisTareas />} />
          <Route path="/hitos/:idHitoEquipo/evaluacion" element={<EvaluacionHitoDeRuta />} />
          <Route path="/notificaciones" element={<PaginaPendiente titulo="Notificaciones" bloque="26f" />} />
          <Route path="/perfil" element={<PaginaPendiente titulo="Perfil" bloque="26f" />} />
          <Route element={<RutaProtegida roles={['DOCENTE']} />}>
            <Route path="/proyectos/nuevo" element={<NuevoProyecto />} />
            <Route path="/panel" element={<ElegirPanel />} />
            <Route path="/reportes" element={<PaginaPendiente titulo="Reportes" bloque="26f" />} />
            <Route path="/proyectos/:idProyecto/panel" element={<PanelProyectoDeRuta />} />
            <Route path="/hitos/:idHitoEquipo/evaluar" element={<EvaluarHitoDeRuta />} />
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
