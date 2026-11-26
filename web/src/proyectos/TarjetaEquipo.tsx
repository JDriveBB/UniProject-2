import { useEffect, useId, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { agregarMiembro, cambiarLider, quitarMiembro } from '../api/proyectos';
import type { EquipoVista, EstudianteDisponibleVista, MiembroVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { Insignia } from '../componentes/Insignia';
import { CampoCorreos } from './CampoCorreos';
import { agregarEstudiantes, sugerenciasDe } from './equipos';
import { CAMPOS_CORREO, mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';
import { aplicarResultadoEquipo, validas } from './fichas';
import type { Ficha } from './fichas';
import {
  bloqueoParaQuitar,
  preguntaQuitarIntegrante,
  quitarIntegrante,
  sePuedeQuitar,
  textoIntegranteQuitado,
} from './quitar';
import type { ApiQuitar } from './quitar';

const API_QUITAR: Pick<ApiQuitar, 'quitarMiembro'> = { quitarMiembro };

/**
 * Una tarjeta de equipo de P03: integrantes, "Líder" o "Designar líder" y "Quitar", y "+ Agregar estudiante"
 * con sugerencias de estudiantes participantes sin equipo en el proyecto.
 */
export function TarjetaEquipo({
  equipo,
  disponibles = [],
  editable,
  alActualizar,
}: {
  equipo: EquipoVista;
  /** Estudiantes participantes sin equipo en el proyecto (solo para el docente dueño); vacío si no se cargaron. */
  disponibles?: EstudianteDisponibleVista[];
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
      if (r.equipo) {
        alActualizar(r.equipo);
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
              <Boton type="submit" cargando={ocupado} disabled={!puedeEnviar}>
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
