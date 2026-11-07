import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router';
import {
  actualizarCriterio,
  actualizarHito,
  actualizarProyecto,
  crearCriterio,
  crearHito,
  crearProyecto,
  detalleProyecto,
  eliminarCriterio,
} from '../api/proyectos';
import type { EquipoVista, ProyectoDetalle } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import {
  asignarIds,
  BORRADOR_VACIO,
  borradorDe,
  claveDeHito,
  claveSeleccionada,
  conCriterio,
  conCriterioNuevo,
  conHito,
  nuevoHito,
  sinCriterio,
} from '../proyectos/borrador';
import type { BorradorProyecto } from '../proyectos/borrador';
import { reemplazarEquipo } from '../proyectos/equipos';
import { mensajeErrorCarga, textoErrorGuardado } from '../proyectos/errores';
import type { Mensaje } from '../proyectos/errores';
import { ejecutarPlan } from '../proyectos/guardado';
import type { ApiGuardado } from '../proyectos/guardado';
import { planDeGuardado } from '../proyectos/plan';
import { TarjetaDatosProyecto } from '../proyectos/TarjetaDatosProyecto';
import { TarjetaEquipos } from '../proyectos/TarjetaEquipos';
import { TarjetaHitos } from '../proyectos/TarjetaHitos';
import { TarjetaHitoSeleccionado } from '../proyectos/TarjetaHitoSeleccionado';
import { borradorValido, primerHitoConErrores, SIN_ERRORES, validarBorrador } from '../proyectos/validacion';
import { idDeRuta, modoDeVista, vieneDeCreacion } from '../proyectos/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';

const API_GUARDADO: ApiGuardado = {
  crearProyecto,
  actualizarProyecto,
  crearHito,
  actualizarHito,
  crearCriterio,
  actualizarCriterio,
  eliminarCriterio,
};

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo' };

function cargaInicial(esNuevo: boolean, id: number | null): Carga {
  if (esNuevo) {
    return { estado: 'listo' };
  }
  return id === null ? { estado: 'error', mensaje: 'Este proyecto no existe.' } : { estado: 'cargando' };
}

/** /proyectos/:idProyecto: una instancia por id (key), así cambiar de proyecto no mezcla borradores. */
export function ProyectoDeRuta() {
  const { idProyecto } = useParams();
  return <ConfigurarProyecto key={idProyecto} idRuta={idProyecto ?? ''} />;
}

/** /proyectos/nuevo (solo DOCENTE). */
export function NuevoProyecto() {
  return <ConfigurarProyecto idRuta={null} />;
}

/**
 * P03 Configurar proyecto (Figma nodo 4:161). idRuta null = proyecto nuevo. Un solo "Guardar proyecto" para datos,
 * hitos y rúbricas (decisión 26b-5); equipos, líder, estudiantes y monitores se guardan al momento (26b-7).
 */
function ConfigurarProyecto({ idRuta }: { idRuta: string | null }) {
  const usuario = useUsuario();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const esNuevo = idRuta === null;
  const idInicial = idRuta === null ? null : idDeRuta(idRuta);
  const [idProyecto, setIdProyecto] = useState<number | null>(idInicial);
  const [carga, setCarga] = useState<Carga>(() => cargaInicial(esNuevo, idInicial));
  const [servidor, setServidor] = useState<ProyectoDetalle | null>(null);
  const [borrador, setBorrador] = useState<BorradorProyecto>(BORRADOR_VACIO);
  const [seleccion, setSeleccion] = useState<string | null>(null);
  const [intentado, setIntentado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [desincronizado, setDesincronizado] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(() =>
    vieneDeCreacion(ubicacion.state)
      ? { tipo: 'exito', texto: 'Proyecto creado. Ahora puedes crear equipos y agregar estudiantes.' }
      : null,
  );
  const contador = useRef(0);

  const cargar = useCallback(async (id: number) => {
    try {
      const detalle = await detalleProyecto(id);
      const nuevo = borradorDe(detalle);
      setServidor(detalle);
      setBorrador(nuevo);
      setSeleccion((actual) => claveSeleccionada(actual, nuevo));
      setCarga({ estado: 'listo' });
    } catch (e) {
      setCarga({ estado: 'error', mensaje: mensajeErrorCarga(e) });
    }
  }, []);

  useEffect(() => {
    if (idInicial !== null) {
      void cargar(idInicial);
    }
  }, [cargar, idInicial]);

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }
  if (carga.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Proyecto" />
        <Alerta tipo="error">{carga.mensaje}</Alerta>
      </div>
    );
  }

  const editable = modoDeVista(usuario, servidor === null ? null : servidor.proyecto) === 'edicion';
  const errores = intentado ? validarBorrador(borrador) : SIN_ERRORES;
  const hito = borrador.hitos.find((h) => h.clave === seleccion) ?? null;

  function siguienteClave(): string {
    contador.current += 1;
    return 'n' + contador.current;
  }

  function alAgregarHito() {
    const clave = siguienteClave();
    setBorrador((b) => ({ ...b, hitos: [...b.hitos, nuevoHito(b, clave)] }));
    setSeleccion(clave);
  }

  function alAgregarCriterio(claveHito: string) {
    const clave = siguienteClave();
    setBorrador((b) => conCriterioNuevo(b, claveHito, clave));
  }

  // estable: TarjetaEquipos lo usa como dependencia de un efecto
  const alActualizarEquipo = useCallback((equipo: EquipoVista) => {
    setServidor((s) => (s === null ? s : { ...s, equipos: reemplazarEquipo(s.equipos, equipo) }));
  }, []);

  function alCancelar() {
    setMensaje(null);
    setIntentado(false);
    if (servidor === null) {
      navegar(rutas.proyectos());
      return;
    }
    const original = borradorDe(servidor);
    setBorrador(original);
    setSeleccion((actual) => claveSeleccionada(actual, original));
  }

  async function alGuardar() {
    setIntentado(true);
    setMensaje(null);
    const encontrados = validarBorrador(borrador);
    if (!borradorValido(encontrados)) {
      const clave = primerHitoConErrores(borrador, encontrados);
      if (clave !== null) {
        setSeleccion(clave);
      }
      setMensaje({ tipo: 'error', texto: 'Revisa los campos marcados en rojo antes de guardar.' });
      return;
    }
    const plan = planDeGuardado(servidor, borrador);
    if (plan.length === 0) {
      setMensaje({ tipo: 'aviso', texto: 'No hay cambios por guardar.' });
      return;
    }
    setGuardando(true);
    const resultado = await ejecutarPlan(plan, idProyecto, API_GUARDADO);
    const id = resultado.idProyecto;
    const fallo = resultado.fallo;
    if (fallo === null && id !== null && esNuevo) {
      navegar(rutas.proyecto(id), { replace: true, state: { proyectoCreado: true } });
      return;
    }
    const conIds = asignarIds(borrador, resultado.creados);
    if (fallo !== null) {
      setBorrador(conIds);
      setMensaje({ tipo: 'error', texto: textoErrorGuardado(fallo.operacion, fallo.causa, borrador, fallo.indice) });
    }
    if (id === null) {
      /* Falló crear el proyecto (primer paso): no se guardó nada y no hay nada que recargar. */
      setGuardando(false);
      return;
    }
    setIdProyecto(id);
    try {
      const detalle = await detalleProyecto(id);
      setServidor(detalle);
      if (fallo === null) {
        const recargado = borradorDe(detalle);
        const elegido = conIds.hitos.find((h) => h.clave === seleccion);
        const claveElegida = elegido !== undefined && elegido.idHito !== null ? claveDeHito(elegido.idHito) : null;
        setBorrador(recargado);
        setSeleccion(claveSeleccionada(claveElegida, recargado));
        setIntentado(false);
        setMensaje({ tipo: 'exito', texto: 'Cambios guardados.' });
      }
    } catch {
      /* Aclaración 3: se conserva el mensaje del paso fallido; con datos viejos no se deja volver a guardar. */
      setDesincronizado(true);
      if (fallo === null) {
        setMensaje({ tipo: 'exito', texto: 'Cambios guardados.' });
      }
    } finally {
      setGuardando(false);
    }
  }

  const titulo = esNuevo ? 'Nuevo proyecto' : editable ? 'Configurar proyecto' : 'Proyecto';
  const descripcion = editable
    ? 'Define los datos del proyecto, sus hitos con rúbrica y los equipos.'
    : 'Consulta los datos del proyecto, sus hitos con rúbrica y los equipos. Solo su docente puede cambiarlos.';

  return (
    <div className="pagina">
      <EncabezadoPagina titulo={titulo} descripcion={descripcion}>
        {editable && (
          <div className="pagina__acciones">
            <Boton variante="secundario" onClick={alCancelar} disabled={guardando}>
              Cancelar
            </Boton>
            <Boton onClick={() => void alGuardar()} cargando={guardando} disabled={desincronizado}>
              Guardar proyecto
            </Boton>
          </div>
        )}
      </EncabezadoPagina>
      {mensaje !== null && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
      {desincronizado && (
        <Alerta tipo="aviso">No se pudo recargar el proyecto. Recarga la página antes de volver a guardar.</Alerta>
      )}
      <div className="configuracion">
        <div className="configuracion__columna">
          <TarjetaDatosProyecto
            borrador={borrador}
            errores={errores.proyecto}
            editable={editable}
            alCambiar={(cambios) => setBorrador((b) => ({ ...b, ...cambios }))}
          />
          <TarjetaHitos
            hitos={borrador.hitos}
            seleccion={seleccion}
            errores={errores}
            editable={editable}
            alSeleccionar={setSeleccion}
            alAgregar={alAgregarHito}
          />
        </div>
        <div className="configuracion__columna">
          <TarjetaHitoSeleccionado
            hito={hito}
            errores={errores}
            editable={editable}
            alCambiarHito={(claveHito, cambios) => setBorrador((b) => conHito(b, claveHito, cambios))}
            alCambiarCriterio={(claveHito, claveCriterio, cambios) =>
              setBorrador((b) => conCriterio(b, claveHito, claveCriterio, cambios))
            }
            alAgregarCriterio={alAgregarCriterio}
            alQuitarCriterio={(claveHito, claveCriterio) => setBorrador((b) => sinCriterio(b, claveHito, claveCriterio))}
          />
          <TarjetaEquipos
            idProyecto={idProyecto}
            equipos={servidor === null ? [] : servidor.equipos}
            editable={editable}
            usaMonitores={borrador.usaMonitores}
            usaMonitoresGuardado={servidor !== null && servidor.proyecto.usaMonitores}
            alCambiarUsaMonitores={(valor) => setBorrador((b) => ({ ...b, usaMonitores: valor }))}
            alActualizarEquipo={alActualizarEquipo}
          />
        </div>
      </div>
    </div>
  );
}
