import { useEffect, useId, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { agregarMiembro, cambiarLider, habilitarCorreos, quitarMiembro } from '../api/proyectos';
import type { EquipoVista, MiembroVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { Insignia } from '../componentes/Insignia';
import { validarCorreo } from '../cuentas/validacion';
import { agregarEstudiante } from './equipos';
import type { ApiEquipos } from './equipos';
import { mensajeErrorApi } from './errores';
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
export function TarjetaEquipo({ idProyecto, equipo, editable, alActualizar }: {
  idProyecto: number;
  equipo: EquipoVista;
  editable: boolean;
  alActualizar: (equipo: EquipoVista) => void;
}) {
  const base = useId();
  const [abierto, setAbierto] = useState(false);
  const [email, setEmail] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [porQuitar, setPorQuitar] = useState<MiembroVista | null>(null);
  const volverA = useRef<string | null>(null);
  const errorEmail = intentado ? (validarCorreo(email) ?? undefined) : undefined;
  const idAgregar = base + '-agregar';
  const idTitulo = base + '-titulo';
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

  function cerrar() {
    setAbierto(false);
    setEmail('');
    setIntentado(false);
  }

  async function alAgregar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    setMensaje(null);
    if (validarCorreo(email) !== null) {
      return;
    }
    setOcupado(true);
    const r = await agregarEstudiante(API_EQUIPOS, idProyecto, equipo.idEquipo, email);
    setOcupado(false);
    if (r.tipo === 'agregado') {
      alActualizar(r.equipo);
      cerrar();
      setMensaje({ tipo: 'exito', texto: 'Estudiante agregado al equipo.' });
    } else if (r.tipo === 'pendiente') {
      cerrar();
      setMensaje({ tipo: 'aviso', texto: r.mensaje });
    } else {
      setMensaje({ tipo: 'error', texto: r.mensaje });
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
            <Campo
              etiqueta="Correo del estudiante"
              type="email"
              placeholder="nombre@uninorte.edu.co"
              autoComplete="off"
              maxLength={254}
              value={email}
              onChange={(ev) => setEmail(ev.target.value)}
              error={errorEmail}
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
      {mensaje !== null && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
    </section>
  );
}
