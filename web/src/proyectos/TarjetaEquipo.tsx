import { useEffect, useId, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { agregarMiembro, cambiarLider, habilitarCorreos, quitarMiembro } from '../api/proyectos';
import type { EquipoVista, MiembroVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { CampoArea } from '../componentes/CampoArea';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { Insignia } from '../componentes/Insignia';
import { agregarEstudiante } from './equipos';
import type { ApiEquipos } from './equipos';
import { mensajeResultadoEstudiantes, separarCorreos, validarLoteCorreos } from './correos';
import { CAMPOS_CORREO, mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';
import {
  bloqueoParaQuitar,
  preguntaQuitarIntegrante,
  quitarIntegrante,
  sePuedeQuitar,
  textoIntegranteQuitado,
} from './quitar';
import type { ApiQuitar } from './quitar';

const API_EQUIPOS: ApiEquipos = { habilitarCorreos, agregarMiembro };
const API_QUITAR: Pick<ApiQuitar, 'quitarMiembro'> = { quitarMiembro };

/**
 * Una tarjeta de equipo de P03: integrantes, "Líder" o "Designar líder" y "Quitar" (Aclaración 2), y
 * "+ Agregar estudiante" (26b-7).
 */
export function TarjetaEquipo({ idProyecto, equipo, editable, alActualizar, alHabilitarCorreo }: {
  idProyecto: number;
  equipo: EquipoVista;
  editable: boolean;
  alActualizar: (equipo: EquipoVista) => void;
  alHabilitarCorreo?: () => void;
}) {
  const base = useId();
  const [abierto, setAbierto] = useState(false);
  const [textoCorreos, setTextoCorreos] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [porQuitar, setPorQuitar] = useState<MiembroVista | null>(null);
  const volverA = useRef<string | null>(null);
  const focoEnAlerta = useRef(false);
  const idAgregar = base + '-agregar';
  const idTitulo = base + '-titulo';
  const idArea = base + '-correos';
  const idMensaje = base + '-mensaje';
  const errorCorreos = intentado ? (validarLoteCorreos(textoCorreos) ?? undefined) : undefined;
  const idQuitar = (idUsuario: number) => `${base}-quitar-${idUsuario}`;
  /* Aclaración 3: con la confirmación abierta no se puede designar líder ni abrir otra confirmación. */
  const bloqueado = ocupado || porQuitar !== null;

  /* Al cerrar la confirmación, el foco va a donde corresponde (26b-15); si ya no existe, al título (Aclaración 3). */
  useEffect(() => {
    if (porQuitar === null && volverA.current !== null) {
      (document.getElementById(volverA.current) ?? document.getElementById(idTitulo))?.focus();
      volverA.current = null;
    }
  }, [porQuitar, idTitulo]);

  /* Tras agregar, el foco va al aviso de resultado (bloque 31). */
  useEffect(() => {
    if (mensaje !== null && focoEnAlerta.current) {
      document.getElementById(idMensaje)?.focus();
      focoEnAlerta.current = false;
    }
  }, [mensaje, idMensaje]);

  function cerrar() {
    setAbierto(false);
    setTextoCorreos('');
    setIntentado(false);
  }

  async function alAgregar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    setMensaje(null);
    const err = validarLoteCorreos(textoCorreos);
    if (err !== null) {
      document.getElementById(idArea)?.focus();
      return;
    }
    setOcupado(true);
    try {
      const correos = separarCorreos(textoCorreos);
      const r = await agregarEstudiante(API_EQUIPOS, idProyecto, equipo.idEquipo, correos);
      if (r.equipo) {
        alActualizar(r.equipo);
      }
      if (r.agregados.length > 0 || r.habilitados.length > 0) {
        alHabilitarCorreo?.();
      }
      const pendientes = [...r.errores.map((e) => e.email), ...r.conOtroRol];
      if (pendientes.length === 0) {
        setTextoCorreos('');
        setIntentado(false);
        setAbierto(false);
      } else {
        setTextoCorreos(pendientes.join('\n'));
        setIntentado(false);
      }
      setMensaje(mensajeResultadoEstudiantes(r));
      focoEnAlerta.current = true;
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, CAMPOS_CORREO) });
      focoEnAlerta.current = true;
    } finally {
      setOcupado(false);
    }
  }

  async function alDesignar(idUsuario: number) {
    setOcupado(true);
    setMensaje(null);
    try {
      alActualizar(await cambiarLider(equipo.idEquipo, idUsuario));
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeErrorApi(e, []) });
    } finally {
      setOcupado(false);
    }
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
      volverA.current = idAgregar;
      setMensaje({ tipo: 'exito', texto: textoIntegranteQuitado(m.nombre) });
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
      {equipo.miembros.length === 0 ? (
        <p className="texto-secundario">Sin integrantes todavía.</p>
      ) : (
        <ul className="equipo__miembros">
          {equipo.miembros.map((m) => (
            <li key={m.idUsuario} className="equipo__miembro">
              <span>{m.nombre}</span>
              {m.rolEquipo === 'LIDER' ? (
                <Insignia tono="info">Líder</Insignia>
              ) : editable ? (
                <span className="equipo__acciones">
                  <button
                    type="button"
                    className="boton-texto"
                    disabled={bloqueado}
                    onClick={() => void alDesignar(m.idUsuario)}
                  >
                    Designar líder<span className="solo-lectores"> a {m.nombre}</span>
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
      {editable && porQuitar !== null && (
        <ConfirmarQuitar
          key={porQuitar.idUsuario}
          pregunta={preguntaQuitarIntegrante(porQuitar.nombre)}
          ocupado={ocupado}
          alConfirmar={() => void alConfirmarQuitar(porQuitar)}
          alCancelar={() => alCancelarQuitar(porQuitar)}
        />
      )}
      {editable &&
        (abierto ? (
          <form className="pila" onSubmit={(ev) => void alAgregar(ev)} noValidate>
            <CampoArea
              id={idArea}
              etiqueta="Correo del estudiante"
              ayuda="Escribe o pega uno o varios correos, separados por espacio o salto de línea."
              placeholder="nombre@uninorte.edu.co"
              autoComplete="off"
              value={textoCorreos}
              onChange={(ev) => setTextoCorreos(ev.target.value)}
              error={errorCorreos}
              rows={3}
              required
            />
            <div className="pagina__acciones">
              <Boton type="submit" cargando={ocupado}>
                Agregar
              </Boton>
              <Boton variante="secundario" onClick={cerrar} disabled={ocupado}>
                Cancelar
              </Boton>
            </div>
          </form>
        ) : (
          <div>
            <Boton id={idAgregar} variante="secundario" onClick={() => setAbierto(true)}>
              + Agregar estudiante
            </Boton>
          </div>
        ))}
      {mensaje !== null && (
        <Alerta id={idMensaje} tabIndex={-1} tipo={mensaje.tipo}>
          {mensaje.texto}
        </Alerta>
      )}
    </section>
  );
}
