import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { detalleProyecto, salirseEquipo, unirseEquipo, vistaInscripcion } from '../api/proyectos';
import type { EquipoInscripcionVista, InscripcionVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { EstadoVacio } from '../componentes/EstadoVacio';
import { Insignia } from '../componentes/Insignia';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaHora } from '../formato';
import { mensajeErrorApi, mensajeErrorCarga } from '../proyectos/errores';
import { idDeRuta } from '../proyectos/vista';
import { rutas } from '../rutas';
import { useUsuario } from '../sesion/ContextoSesion';

type Carga =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; inscripcion: InscripcionVista; nombreProyecto: string };

/** /proyectos/:idProyecto/inscripcion: una instancia por proyecto (key). Destino de los avisos del bloque 50. */
export function InscripcionDeRuta() {
  const { idProyecto } = useParams();
  return <Inscripcion key={idProyecto} idRuta={idProyecto ?? ''} />;
}

function textoEstado(v: InscripcionVista): string {
  if (v.cierreEquipos === null) {
    return 'La inscripción aún no está abierta. Tu docente fijará la fecha.';
  }
  return v.abierta
    ? `La inscripción está abierta hasta el ${fechaHora(v.cierreEquipos)}. Después, el docente asigna a quienes no tengan equipo.`
    : `La inscripción cerró el ${fechaHora(v.cierreEquipos)}. Si aún no tienes equipo, tu docente te asignará uno.`;
}

function textoIntegrantes(e: EquipoInscripcionVista): string {
  return e.cupo === null ? 'Lo arma el docente' : `${e.integrantes} de ${e.cupo} integrantes`;
}

export function Inscripcion({ idRuta }: { idRuta: string }) {
  const usuario = useUsuario();
  const avisar = useAvisos();
  const id = idDeRuta(idRuta);
  const [carga, setCarga] = useState<Carga>(() =>
    id === null ? { estado: 'error', mensaje: 'Este proyecto no existe.' } : { estado: 'cargando' },
  );
  const [ocupado, setOcupado] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [saliendo, setSaliendo] = useState(false);

  const cargar = useCallback(async (idProyecto: number) => {
    try {
      const [inscripcion, detalle] = await Promise.all([vistaInscripcion(idProyecto), detalleProyecto(idProyecto)]);
      setCarga({ estado: 'listo', inscripcion, nombreProyecto: detalle.proyecto.nombre });
    } catch (e) {
      setCarga({ estado: 'error', mensaje: mensajeErrorCarga(e) });
    }
  }, []);

  useEffect(() => {
    if (id !== null) {
      void cargar(id);
    }
  }, [id, cargar]);

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }
  if (carga.estado === 'error' || id === null) {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Elegir equipo" />
        <Alerta tipo="error">{carga.estado === 'error' ? carga.mensaje : 'Este proyecto no existe.'}</Alerta>
      </div>
    );
  }

  const idProyecto: number = id;
  const { inscripcion: v, nombreProyecto } = carga;
  const esEstudiante = usuario.rol === 'ESTUDIANTE';
  const miEquipo = v.equipos.find((e) => e.idEquipo === v.idMiEquipo);
  const puedeSalir = miEquipo !== undefined && v.abierta && miEquipo.cupo !== null;

  async function unirme(e: EquipoInscripcionVista) {
    setOcupado(true);
    setMensaje(null);
    try {
      await unirseEquipo(e.idEquipo);
      avisar(`Te uniste a ${e.nombreEquipo}.`);
    } catch (error) {
      setMensaje(mensajeErrorApi(error, []));
    } finally {
      await cargar(idProyecto);
      setOcupado(false);
    }
  }

  async function salir(e: EquipoInscripcionVista) {
    setOcupado(true);
    setMensaje(null);
    try {
      await salirseEquipo(e.idEquipo);
      avisar(`Saliste de ${e.nombreEquipo}.`);
    } catch (error) {
      setMensaje(mensajeErrorApi(error, []));
    } finally {
      setSaliendo(false);
      await cargar(idProyecto);
      setOcupado(false);
    }
  }

  return (
    <div className="pagina">
      <EncabezadoPagina titulo="Elegir equipo" descripcion={nombreProyecto}>
        <div className="pagina__acciones">
          <Link className="boton boton--secundario" to={esEstudiante ? rutas.misCursos() : rutas.proyecto(idProyecto)}>
            {esEstudiante ? 'Volver a Mis cursos' : 'Volver al proyecto'}
          </Link>
        </div>
      </EncabezadoPagina>

      <Alerta tipo="info">{textoEstado(v)}</Alerta>
      {mensaje !== null && <Alerta tipo="error">{mensaje}</Alerta>}

      {miEquipo !== undefined && (
        <Tarjeta titulo={`Tu equipo: ${miEquipo.nombreEquipo}`}>
          {miEquipo.nombres.length > 0 && <p>{miEquipo.nombres.join(', ')}</p>}
          <div className="pagina__acciones">
            <Link className="boton boton--primario" to={rutas.tableroEquipo(miEquipo.idEquipo)}>
              Abrir tablero
            </Link>
            <Link className="boton boton--secundario" to={rutas.votacionEquipo(miEquipo.idEquipo)}>
              Votación de líder
            </Link>
            {puedeSalir && (
              <Boton variante="secundario" disabled={ocupado || saliendo} onClick={() => setSaliendo(true)}>
                Salir del equipo
              </Boton>
            )}
          </div>
          {puedeSalir && saliendo && (
            <ConfirmarQuitar
              pregunta={`¿Salir de ${miEquipo.nombreEquipo}? Podrás unirte a otro equipo mientras la inscripción siga abierta.`}
              ocupado={ocupado}
              textoConfirmar="Sí, salir"
              textoOcupado="Saliendo…"
              alConfirmar={() => void salir(miEquipo)}
              alCancelar={() => setSaliendo(false)}
            />
          )}
        </Tarjeta>
      )}

      {v.equipos.length === 0 ? (
        <EstadoVacio titulo="Aún no hay equipos">Tu docente todavía no ha creado los equipos de este proyecto.</EstadoVacio>
      ) : (
        <div className="rejilla-equipos">
          {v.equipos.map((e) => (
            <section key={e.idEquipo} className="equipo" aria-label={e.nombreEquipo}>
              <h2 className="equipo__titulo">{e.nombreEquipo}</h2>
              {e.nombreCurso !== null && <p className="texto-secundario">{e.nombreCurso}</p>}
              <div className="curso__insignias">
                <Insignia>{textoIntegrantes(e)}</Insignia>
                {e.cupo !== null && e.integrantes >= e.cupo && <Insignia tono="aviso">Completo</Insignia>}
                {e.idEquipo === v.idMiEquipo && <Insignia tono="info">Tu equipo</Insignia>}
              </div>
              {e.nombres.length === 0 ? (
                <p className="texto-secundario">Aún sin integrantes</p>
              ) : (
                <ul className="equipo__miembros">
                  {e.nombres.map((n, i) => (
                    <li key={`${i}-${n}`}>{n}</li>
                  ))}
                </ul>
              )}
              {e.puedeUnirse && (
                <div>
                  <Boton
                    variante="secundario"
                    aria-label={`Unirme a ${e.nombreEquipo}`}
                    disabled={ocupado}
                    onClick={() => void unirme(e)}
                  >
                    Unirme
                  </Boton>
                </div>
              )}
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
