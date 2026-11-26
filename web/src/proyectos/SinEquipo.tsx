import { useState } from 'react';
import { ErrorApi } from '../api/cliente';
import { agregarMiembro, repartirEquipos } from '../api/proyectos';
import type { CursoResumen, EquipoVista, EstudianteDisponibleVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { equiposParaAsignar, etiquetaEquipoAsignar, preguntaPasarCupo, preguntaRepartir, textoReparto } from './cupos';
import { CAMPOS_CORREO, mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';

type Confirmacion =
  | { tipo: 'cupo'; estudiante: EstudianteDisponibleVista; equipo: EquipoVista }
  | { tipo: 'repartir' };

function esCupoLleno(e: unknown): boolean {
  return e instanceof ErrorApi && e.estado === 409 && e.codigo === 'CUPO_LLENO';
}

/** Estudiantes del proyecto sin equipo: asignarlos a mano a un equipo o repartirlos automáticamente. */
export function SinEquipo({
  idProyecto,
  disponibles,
  equipos,
  cursos,
  alCambio,
}: {
  idProyecto: number;
  disponibles: EstudianteDisponibleVista[];
  equipos: EquipoVista[];
  cursos: CursoResumen[];
  alCambio: () => void;
}) {
  const avisar = useAvisos();
  const [elegidos, setElegidos] = useState<Record<string, string>>({});
  const [confirmacion, setConfirmacion] = useState<Confirmacion | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const hayEquiposConCupo = equipos.some((e) => e.cupo !== null);
  const variosCursos = cursos.length >= 2;
  const bloqueado = ocupado || confirmacion !== null;

  async function asignar(estudiante: EstudianteDisponibleVista, equipo: EquipoVista, pasarCupo: boolean) {
    setOcupado(true);
    setMensaje(null);
    try {
      await agregarMiembro(equipo.idEquipo, estudiante.email, pasarCupo);
      setConfirmacion(null);
      avisar(`${estudiante.nombre} entró a ${equipo.nombreEquipo}.`);
      alCambio();
    } catch (e) {
      if (!pasarCupo && esCupoLleno(e)) {
        setConfirmacion({ tipo: 'cupo', estudiante, equipo });
      } else {
        setConfirmacion(null);
        setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, CAMPOS_CORREO) });
      }
    } finally {
      setOcupado(false);
    }
  }

  async function repartir() {
    setOcupado(true);
    setMensaje(null);
    try {
      setMensaje(textoReparto(await repartirEquipos(idProyecto)));
      setConfirmacion(null);
      alCambio();
    } catch (e) {
      setConfirmacion(null);
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, []) });
    } finally {
      setOcupado(false);
    }
  }

  function alElegirAsignar(estudiante: EstudianteDisponibleVista) {
    const equipo = equipos.find((e) => String(e.idEquipo) === elegidos[estudiante.email]);
    if (equipo !== undefined) {
      void asignar(estudiante, equipo, false);
    }
  }

  return (
    <div className="pila">
      <h3 className="tarjeta__subtitulo">{`Estudiantes sin equipo (${disponibles.length})`}</h3>
      {disponibles.length === 0 ? (
        <p className="texto-secundario">Todos los estudiantes del proyecto tienen equipo.</p>
      ) : (
        <ul className="lista-simple">
          {disponibles.map((d) => (
            <li key={d.email} className="equipo__miembro">
              <span>
                {d.nombre}
                {variosCursos && <span className="texto-secundario">{` · ${d.nombreCurso}`}</span>}
              </span>
              <span className="equipo__acciones">
                <select
                  className="campo__entrada"
                  aria-label={`Equipo para ${d.nombre}`}
                  value={elegidos[d.email] ?? ''}
                  disabled={bloqueado}
                  onChange={(ev) => setElegidos((m) => ({ ...m, [d.email]: ev.target.value }))}
                >
                  <option value="">Elige un equipo</option>
                  {equiposParaAsignar(equipos, d.idCurso).map((e) => (
                    <option key={e.idEquipo} value={String(e.idEquipo)}>
                      {etiquetaEquipoAsignar(e)}
                    </option>
                  ))}
                </select>
                <Boton
                  variante="secundario"
                  disabled={bloqueado || (elegidos[d.email] ?? '') === ''}
                  onClick={() => alElegirAsignar(d)}
                  aria-label={`Asignar a ${d.nombre}`}
                >
                  Asignar
                </Boton>
              </span>
            </li>
          ))}
        </ul>
      )}
      {confirmacion?.tipo === 'cupo' && (
        <ConfirmarQuitar
          pregunta={preguntaPasarCupo(confirmacion.equipo.nombreEquipo, [confirmacion.estudiante.nombre])}
          ocupado={ocupado}
          textoConfirmar="Sí, pasar el cupo"
          textoOcupado="Agregando…"
          alConfirmar={() => void asignar(confirmacion.estudiante, confirmacion.equipo, true)}
          alCancelar={() => setConfirmacion(null)}
        />
      )}
      {confirmacion?.tipo === 'repartir' && (
        <ConfirmarQuitar
          pregunta={preguntaRepartir(disponibles.length)}
          ocupado={ocupado}
          textoConfirmar="Sí, repartir"
          textoOcupado="Repartiendo…"
          alConfirmar={() => void repartir()}
          alCancelar={() => setConfirmacion(null)}
        />
      )}
      {mensaje !== null && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
      <div>
        <Boton
          variante="secundario"
          disabled={bloqueado || disponibles.length === 0 || !hayEquiposConCupo}
          onClick={() => setConfirmacion({ tipo: 'repartir' })}
        >
          Repartir automáticamente
        </Boton>
      </div>
    </div>
  );
}
