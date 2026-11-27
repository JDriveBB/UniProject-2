import { useEffect, useId, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router';
import { abrirRonda, vistaVotacion } from '../api/votacion';
import {
  agregarMiembro,
  cambiarCupo,
  cambiarLider,
  destituirLider,
  quitarMiembro,
} from '../api/proyectos';
import type { CursoResumen, EquipoVista, EstudianteDisponibleVista, MiembroVista, VotacionVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { Insignia } from '../componentes/Insignia';
import { rutas } from '../rutas';
import { estadoRonda, resumenRonda } from '../votacion/vista';
import { CampoCorreos } from './CampoCorreos';
import { cupoDeTexto, pasadoDeCupo, preguntaPasarCupo, textoCupo, tieneLider, validarCupo } from './cupos';
import { agregarEstudiantes, sugerenciasDe } from './equipos';
import { CAMPOS_CORREO, mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';
import { aplicarResultadoEquipo, validas } from './fichas';
import type { Ficha } from './fichas';
import {
  bloqueoParaQuitar,
  preguntaDestituir,
  preguntaNombrarConRonda,
  preguntaQuitarIntegrante,
  quitarIntegrante,
  sePuedeQuitar,
  textoIntegranteQuitado,
} from './quitar';
import type { ApiQuitar } from './quitar';

const API_QUITAR: Pick<ApiQuitar, 'quitarMiembro'> = { quitarMiembro };

/**
 * Una tarjeta de equipo de P03: cupo, curso, integrantes, "Líder" con "Destituir" o "Nombrar líder" y "Quitar",
 * estado de la votación de líder, y "+ Agregar estudiante" con sugerencias de estudiantes sin equipo.
 */
export function TarjetaEquipo({
  equipo,
  disponibles = [],
  cursos = [],
  horasRonda,
  editable,
  alActualizar,
}: {
  idProyecto: number;
  equipo: EquipoVista;
  /** Estudiantes participantes sin equipo en el proyecto (solo para el docente dueño); vacío si no se cargaron. */
  disponibles?: EstudianteDisponibleVista[];
  /** Cursos del proyecto (para mostrar el curso del equipo cuando son dos o más). */
  cursos?: CursoResumen[];
  /** Duración de la votación de líder, para avisar al destituir. */
  horasRonda?: number;
  editable: boolean;
  alActualizar: (equipo: EquipoVista) => void;
}) {
  const avisar = useAvisos();
  const base = useId();
  const [abierto, setAbierto] = useState(false);
  const [fichas, setFichas] = useState<Ficha[]>([]);
  const [ocupado, setOcupado] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [porQuitar, setPorQuitar] = useState<MiembroVista | null>(null);
  const [porDestituir, setPorDestituir] = useState<MiembroVista | null>(null);
  const [porNombrar, setPorNombrar] = useState<MiembroVista | null>(null);
  const [porPasarCupo, setPorPasarCupo] = useState<string[] | null>(null);
  const [votacion, setVotacion] = useState<VotacionVista | null>(null);
  const [versionVotacion, setVersionVotacion] = useState(0);
  const [editandoCupo, setEditandoCupo] = useState(false);
  const [cupoTexto, setCupoTexto] = useState('');
  const [intentoCupo, setIntentoCupo] = useState(false);
  const volverA = useRef<string | null>(null);
  const focoEnAlerta = useRef(false);
  const enfocarEn = useRef<'campo' | 'agregar' | null>(null);
  const idAgregar = base + '-agregar';
  const idTitulo = base + '-titulo';
  const idArea = base + '-correos';
  const idMensaje = base + '-mensaje';
  const correosValidos = validas(fichas);
  const hayErrorFormato = fichas.some((f) => f.error !== null);
  const puedeEnviar = correosValidos.length > 0 && !hayErrorFormato;
  const textoBoton =
    correosValidos.length === 0
      ? 'Agregar estudiantes'
      : correosValidos.length === 1
        ? 'Agregar 1 estudiante'
        : `Agregar ${correosValidos.length} estudiantes`;
  const idQuitar = (idUsuario: number) => `${base}-quitar-${idUsuario}`;
  const idLider = (idUsuario: number) => `${base}-lider-${idUsuario}`;
  const idCambiarCupo = base + '-cupo';
  /* Aclaración 3 y bloque 51: con una confirmación abierta no se puede abrir otra ni cambiar al líder. */
  const hayConfirmacion = porQuitar !== null || porDestituir !== null || porNombrar !== null || porPasarCupo !== null;
  const bloqueado = ocupado || hayConfirmacion;
  const resumen = votacion === null ? null : resumenRonda(votacion);
  const rondaAbierta = votacion !== null && estadoRonda(votacion.ronda) === 'abierta';
  const nombreCurso =
    equipo.idCurso !== null && cursos.length >= 2
      ? cursos.find((c) => c.idCurso === equipo.idCurso)?.nombre
      : undefined;
  const errorCupo = intentoCupo ? (validarCupo(cupoTexto, false) ?? undefined) : undefined;
  const nuevoCupo = validarCupo(cupoTexto, false) === null ? cupoDeTexto(cupoTexto) : null;

  /* Al cerrar la confirmación, el foco va a donde corresponde (26b-15); si ya no existe, al título (Aclaración 3). */
  useEffect(() => {
    if (!hayConfirmacion && volverA.current !== null) {
      (document.getElementById(volverA.current) ?? document.getElementById(idTitulo))?.focus();
      volverA.current = null;
    }
  }, [hayConfirmacion, idTitulo]);

  const claveMiembros = equipo.miembros.map((m) => m.idUsuario).join(',');

  /* Estado de la votación de líder: solo para el docente; si falla, no se muestra nada. */
  useEffect(() => {
    if (!editable) {
      return;
    }
    let vigente = true;
    vistaVotacion(equipo.idEquipo)
      .then((v) => {
        if (vigente) {
          setVotacion(v);
        }
      })
      .catch(() => {
        if (vigente) {
          setVotacion(null);
        }
      });
    return () => {
      vigente = false;
    };
  }, [editable, equipo.idEquipo, versionVotacion, claveMiembros]);

  function recargarVotacion() {
    setVersionVotacion((v) => v + 1);
  }

  /* Tras agregar, el foco va al aviso de resultado (bloque 31). */
  useEffect(() => {
    if (mensaje !== null && focoEnAlerta.current) {
      document.getElementById(idMensaje)?.focus();
      focoEnAlerta.current = false;
    }
  }, [mensaje, idMensaje]);

  useEffect(() => {
    if (abierto && enfocarEn.current === 'campo') {
      document.getElementById(idArea)?.focus();
      enfocarEn.current = null;
    } else if (!abierto && enfocarEn.current === 'agregar') {
      document.getElementById(idAgregar)?.focus();
      enfocarEn.current = null;
    }
  }, [abierto, idArea, idAgregar]);

  function abrir() {
    setMensaje(null);
    enfocarEn.current = 'campo';
    setAbierto(true);
  }

  function cerrar() {
    enfocarEn.current = 'agregar';
    setAbierto(false);
    setFichas([]);
  }

  async function alAgregar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!puedeEnviar || ocupado) {
      return;
    }
    setMensaje(null);
    setOcupado(true);
    try {
      const r = await agregarEstudiantes(
        { agregarMiembro },
        equipo.idEquipo,
        correosValidos.map((f) => f.correo),
      );
      aplicarAgregado(r);
      if (r.cupoLleno.length > 0) {
        setPorPasarCupo(r.cupoLleno);
      }
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, CAMPOS_CORREO) });
      focoEnAlerta.current = true;
    } finally {
      setOcupado(false);
    }
  }

  /** Aplica el resultado de agregar a las fichas y recarga la votación (el equipo cambió de integrantes). */
  function aplicarAgregado(r: Awaited<ReturnType<typeof agregarEstudiantes>>) {
    if (r.equipo) {
      alActualizar(r.equipo);
      recargarVotacion();
    }
    const aplicado = aplicarResultadoEquipo(fichas, r);
    setFichas(aplicado.fichas);
    if (aplicado.fichas.length === 0) {
      setAbierto(false);
    }
    setMensaje({
      tipo: aplicado.fichas.length === 0 ? 'exito' : 'aviso',
      texto: aplicado.resumen,
    });
    focoEnAlerta.current = true;
  }

  async function alConfirmarPasarCupo(correos: string[]) {
    setOcupado(true);
    setMensaje(null);
    try {
      aplicarAgregado(await agregarEstudiantes({ agregarMiembro }, equipo.idEquipo, correos, true));
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, CAMPOS_CORREO) });
      focoEnAlerta.current = true;
    } finally {
      setOcupado(false);
      setPorPasarCupo(null);
    }
  }

  async function nombrar(m: MiembroVista) {
    setOcupado(true);
    setMensaje(null);
    try {
      alActualizar(await cambiarLider(equipo.idEquipo, m.idUsuario));
      recargarVotacion();
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, []) });
    } finally {
      setOcupado(false);
      setPorNombrar(null);
    }
  }

  function alNombrar(m: MiembroVista) {
    if (rondaAbierta) {
      setPorNombrar(m);
    } else {
      void nombrar(m);
    }
  }

  async function alConfirmarDestituir(m: MiembroVista) {
    setOcupado(true);
    setMensaje(null);
    try {
      alActualizar(await destituirLider(equipo.idEquipo));
      recargarVotacion();
      avisar(`${m.nombre} ya no es líder. Se abrió una votación en ${equipo.nombreEquipo}.`);
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, []) });
    } finally {
      setOcupado(false);
      setPorDestituir(null);
    }
  }

  async function alAbrirVotacion() {
    setOcupado(true);
    setMensaje(null);
    try {
      setVotacion(await abrirRonda(equipo.idEquipo));
      avisar(`Se abrió la votación de ${equipo.nombreEquipo}.`);
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, []) });
    } finally {
      setOcupado(false);
    }
  }

  function abrirCupo() {
    setMensaje(null);
    setCupoTexto(equipo.cupo === null ? '' : String(equipo.cupo));
    setIntentoCupo(false);
    setEditandoCupo(true);
  }

  async function guardarCupo(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentoCupo(true);
    if (validarCupo(cupoTexto, false) !== null) {
      return;
    }
    setOcupado(true);
    setMensaje(null);
    try {
      alActualizar(await cambiarCupo(equipo.idEquipo, cupoDeTexto(cupoTexto)));
      setEditandoCupo(false);
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, [['cupo', 'Cupo']]) });
    } finally {
      setOcupado(false);
    }
  }

  /** Cierra una confirmación y devuelve el foco al botón que la abrió (null: el de agregar). */
  function cancelarCon(idFoco: string | null, cerrar: () => void) {
    volverA.current = idFoco ?? idAgregar;
    cerrar();
  }

  function alCancelarQuitar(m: MiembroVista) {
    volverA.current = idQuitar(m.idUsuario);
    setPorQuitar(null);
  }

  async function alConfirmarQuitar(m: MiembroVista) {
    const bloqueo = bloqueoParaQuitar(equipo.miembros, m.idUsuario);
    if (bloqueo !== null) {
      volverA.current = idQuitar(m.idUsuario);
      setMensaje({ tipo: 'error', texto: bloqueo });
      setPorQuitar(null);
      return;
    }
    setOcupado(true);
    setMensaje(null);
    const r = await quitarIntegrante(API_QUITAR, equipo.idEquipo, m.idUsuario);
    setOcupado(false);
    if (r.tipo === 'quitado') {
      alActualizar(r.equipo);
      recargarVotacion();
      volverA.current = idAgregar;
      avisar(textoIntegranteQuitado(m.nombre));
    } else {
      volverA.current = idQuitar(m.idUsuario);
      setMensaje({ tipo: 'error', texto: r.mensaje });
    }
    setPorQuitar(null);
  }

  return (
    <section className="equipo" aria-label={equipo.nombreEquipo}>
      <h3 id={idTitulo} className="equipo__titulo" tabIndex={-1}>
        {equipo.nombreEquipo}
      </h3>
      <div className="curso__insignias">
        {nombreCurso !== undefined && <Insignia>{nombreCurso}</Insignia>}
        <Insignia tono={pasadoDeCupo(equipo) ? 'aviso' : 'neutro'}>{textoCupo(equipo)}</Insignia>
        {editable && equipo.miembros.length > 0 && !tieneLider(equipo) && <Insignia tono="aviso">Sin líder</Insignia>}
      </div>
      {editable &&
        (editandoCupo ? (
          <form className="pila" onSubmit={(ev) => void guardarCupo(ev)} noValidate>
            <Campo
              id={idCambiarCupo + '-campo'}
              etiqueta="Cupo"
              type="number"
              min={1}
              max={100}
              value={cupoTexto}
              onChange={(ev) => setCupoTexto(ev.target.value)}
              error={errorCupo}
              disabled={ocupado}
            />
            <p className="texto-secundario">Vacío: lo armas tú.</p>
            {nuevoCupo !== null && nuevoCupo < equipo.miembros.length && (
              <p className="texto-secundario">Quedará pasado de cupo; nadie sale del equipo.</p>
            )}
            <div className="pagina__acciones">
              <Boton type="submit" cargando={ocupado}>
                Guardar
              </Boton>
              <Boton variante="secundario" onClick={() => setEditandoCupo(false)} disabled={ocupado}>
                Cancelar
              </Boton>
            </div>
          </form>
        ) : (
          <div>
            <button
              id={idCambiarCupo}
              type="button"
              className="boton-texto"
              disabled={bloqueado}
              onClick={abrirCupo}
            >
              Cambiar cupo<span className="solo-lectores"> de {equipo.nombreEquipo}</span>
            </button>
          </div>
        ))}
      {equipo.miembros.length === 0 ? (
        <p className="texto-secundario">Sin integrantes todavía.</p>
      ) : (
        <ul className="equipo__miembros">
          {equipo.miembros.map((m) => (
            <li key={m.idUsuario} className="equipo__miembro">
              <span>{m.nombre}</span>
              {m.rolEquipo === 'LIDER' ? (
                <span className="equipo__acciones">
                  <Insignia tono="info">Líder</Insignia>
                  {editable && (
                    <button
                      id={idLider(m.idUsuario)}
                      type="button"
                      className="boton-texto boton-texto--peligro"
                      disabled={bloqueado}
                      aria-label={`Destituir a ${m.nombre} como líder`}
                      onClick={() => setPorDestituir(m)}
                    >
                      Destituir
                    </button>
                  )}
                </span>
              ) : editable ? (
                <span className="equipo__acciones">
                  <button
                    id={idLider(m.idUsuario)}
                    type="button"
                    className="boton-texto"
                    disabled={bloqueado}
                    onClick={() => alNombrar(m)}
                  >
                    Nombrar líder<span className="solo-lectores"> a {m.nombre}</span>
                  </button>
                  {sePuedeQuitar(m) && (
                    <button
                      id={idQuitar(m.idUsuario)}
                      type="button"
                      className="boton-texto boton-texto--peligro"
                      disabled={bloqueado}
                      onClick={() => setPorQuitar(m)}
                    >
                      Quitar<span className="solo-lectores"> a {m.nombre} del equipo</span>
                    </button>
                  )}
                </span>
              ) : null}
            </li>
          ))}
        </ul>
      )}
      {editable && resumen !== null && votacion !== null && (
        <div className="pila">
          <p className={`equipo__votacion equipo__votacion--${resumen.tono}`}>{resumen.texto}</p>
          <div>
            <Link className="enlace-fuerte" to={rutas.votacionEquipo(equipo.idEquipo)}>
              {resumen.desempatar ? 'Desempatar' : 'Ver votación'}
            </Link>
          </div>
        </div>
      )}
      {editable && votacion !== null && votacion.puedeAbrir && (
        <div>
          <button
            type="button"
            className="boton-texto"
            disabled={bloqueado}
            onClick={() => void alAbrirVotacion()}
          >
            Abrir votación<span className="solo-lectores"> de {equipo.nombreEquipo}</span>
          </button>
        </div>
      )}
      {editable && porQuitar !== null && (
        <ConfirmarQuitar
          key={porQuitar.idUsuario}
          pregunta={preguntaQuitarIntegrante(porQuitar.nombre)}
          ocupado={ocupado}
          alConfirmar={() => void alConfirmarQuitar(porQuitar)}
          alCancelar={() => alCancelarQuitar(porQuitar)}
        />
      )}
      {editable && porDestituir !== null && (
        <ConfirmarQuitar
          key={'d' + porDestituir.idUsuario}
          pregunta={preguntaDestituir(porDestituir.nombre, horasRonda ?? 48)}
          ocupado={ocupado}
          textoConfirmar="Sí, destituir"
          textoOcupado="Destituyendo…"
          alConfirmar={() => void alConfirmarDestituir(porDestituir)}
          alCancelar={() => cancelarCon(idLider(porDestituir.idUsuario), () => setPorDestituir(null))}
        />
      )}
      {editable && porNombrar !== null && (
        <ConfirmarQuitar
          key={'n' + porNombrar.idUsuario}
          pregunta={preguntaNombrarConRonda(porNombrar.nombre)}
          ocupado={ocupado}
          textoConfirmar="Sí, nombrar"
          textoOcupado="Nombrando…"
          alConfirmar={() => void nombrar(porNombrar)}
          alCancelar={() => cancelarCon(idLider(porNombrar.idUsuario), () => setPorNombrar(null))}
        />
      )}
      {editable && porPasarCupo !== null && (
        <ConfirmarQuitar
          pregunta={preguntaPasarCupo(equipo.nombreEquipo, porPasarCupo)}
          ocupado={ocupado}
          textoConfirmar="Sí, pasar el cupo"
          textoOcupado="Agregando…"
          alConfirmar={() => void alConfirmarPasarCupo(porPasarCupo)}
          alCancelar={() => cancelarCon(null, () => setPorPasarCupo(null))}
        />
      )}
      {mensaje !== null && (
        <Alerta id={idMensaje} tabIndex={-1} tipo={mensaje.tipo}>
          {mensaje.texto}
        </Alerta>
      )}
      {editable &&
        (abierto ? (
          <form className="pila" onSubmit={(ev) => void alAgregar(ev)} noValidate>
            <CampoCorreos
              id={idArea}
              etiqueta="Correo del estudiante"
              fichas={fichas}
              alCambiar={setFichas}
              deshabilitado={ocupado}
              sugerencias={sugerenciasDe(disponibles, equipo.idCurso)}
            />
            <div className="pagina__acciones">
              <Boton type="submit" cargando={ocupado} disabled={!puedeEnviar || bloqueado}>
                {textoBoton}
              </Boton>
              <Boton variante="secundario" onClick={cerrar} disabled={ocupado}>
                Cancelar
              </Boton>
            </div>
          </form>
        ) : (
          <div>
            <Boton id={idAgregar} variante="secundario" onClick={abrir}>
              + Agregar estudiante
            </Boton>
          </div>
        ))}
    </section>
  );
}
