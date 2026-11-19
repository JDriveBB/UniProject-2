import { useCallback, useEffect, useId, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { habilitarCorreos, listarMonitores, quitarMonitor } from '../api/proyectos';
import type { MonitorVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { Insignia } from '../componentes/Insignia';
import { CampoCorreos } from './CampoCorreos';
import { agregarMonitor } from './equipos';
import { CAMPOS_CORREO, mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';
import { aplicarResultadoMonitores, validas } from './fichas';
import type { Ficha } from './fichas';
import { preguntaQuitarMonitor, quitarMonitorDelProyecto } from './quitar';
import type { ApiQuitar } from './quitar';

const API_QUITAR: Pick<ApiQuitar, 'quitarMonitor'> = { quitarMonitor };

type EstadoMonitores =
  { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; monitores: MonitorVista[] };

/** Fila "Monitores:" de P03 (RF14), con quitar en cada insignia (Aclaración 2). Un monitor quitado se puede volver a agregar con su correo (bloque 30). Solo la ve el docente dueño. */
export function FilaMonitores({
  idProyecto,
  version,
  alHabilitarCorreo,
}: {
  idProyecto: number;
  version?: number;
  alHabilitarCorreo?: () => void;
}) {
  const base = useId();
  const [lista, setLista] = useState<EstadoMonitores>({ estado: 'cargando' });
  const [abierto, setAbierto] = useState(false);
  const [fichas, setFichas] = useState<Ficha[]>([]);
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [porQuitar, setPorQuitar] = useState<MonitorVista | null>(null);
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
      ? 'Agregar monitores'
      : correosValidos.length === 1
        ? 'Agregar 1 monitor'
        : `Agregar ${correosValidos.length} monitores`;
  const idQuitar = (idUsuario: number) => `${base}-quitar-${idUsuario}`;

  const cargar = useCallback(async () => {
    try {
      setLista({ estado: 'listo', monitores: await listarMonitores(idProyecto) });
    } catch (e) {
      setLista({ estado: 'error', mensaje: mensajeErrorApi(e, []) });
    }
  }, [idProyecto]);

  useEffect(() => {
    void cargar();
  }, [cargar, version]);

  /* Al cerrar la confirmación, el foco va a donde corresponde (26b-15); si ya no existe, al título (Aclaración 3). */
  useEffect(() => {
    if (porQuitar === null && volverA.current !== null) {
      (document.getElementById(volverA.current) ?? document.getElementById(idTitulo))?.focus();
      volverA.current = null;
    }
  }, [porQuitar, lista, idTitulo]);

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
    if (!puedeEnviar || enviando) {
      return;
    }
    setMensaje(null);
    setEnviando(true);
    try {
      const r = await agregarMonitor(
        { habilitarCorreos },
        idProyecto,
        correosValidos.map((f) => f.correo),
      );
      if (r.asignados.length > 0 || r.habilitados.length > 0) {
        alHabilitarCorreo?.();
      }
      await cargar();
      const aplicado = aplicarResultadoMonitores(fichas, r);
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
      setEnviando(false);
    }
  }

  function alCancelarQuitar(m: MonitorVista) {
    volverA.current = idQuitar(m.idUsuario);
    setPorQuitar(null);
  }

  async function alConfirmarQuitar(m: MonitorVista) {
    setEnviando(true);
    setMensaje(null);
    const r = await quitarMonitorDelProyecto(API_QUITAR, idProyecto, m.idUsuario, m.nombre);
    if (r.tipo !== 'error') {
      await cargar();
    }
    setEnviando(false);
    setMensaje(r);
    volverA.current = r.tipo === 'error' ? idQuitar(m.idUsuario) : idAgregar;
    setPorQuitar(null);
  }

  return (
    <div className="pila">
      <div className="monitores">
        <span id={idTitulo} className="monitores__titulo" tabIndex={-1}>
          Monitores:
        </span>
        {lista.estado === 'cargando' && <span className="texto-secundario">Cargando…</span>}
        {lista.estado === 'listo' && lista.monitores.length === 0 && (
          <span className="texto-secundario">Ninguno todavía.</span>
        )}
        {lista.estado === 'listo' &&
          lista.monitores.map((m) => (
            <Insignia key={m.idUsuario}>
              {m.nombre}
              <button
                id={idQuitar(m.idUsuario)}
                type="button"
                className="insignia__quitar"
                aria-label={`Quitar a ${m.nombre} de los monitores`}
                disabled={enviando}
                onClick={() => setPorQuitar(m)}
              >
                <span aria-hidden="true">×</span>
              </button>
            </Insignia>
          ))}
        {!abierto && (
          <Boton id={idAgregar} variante="secundario" onClick={abrir}>
            + Agregar monitor
          </Boton>
        )}
      </div>
      {lista.estado === 'error' && <Alerta tipo="error">{lista.mensaje}</Alerta>}
      {porQuitar !== null && (
        <ConfirmarQuitar
          key={porQuitar.idUsuario}
          pregunta={preguntaQuitarMonitor(porQuitar.nombre)}
          ocupado={enviando}
          alConfirmar={() => void alConfirmarQuitar(porQuitar)}
          alCancelar={() => alCancelarQuitar(porQuitar)}
        />
      )}
      {mensaje !== null && (
        <Alerta id={idMensaje} tabIndex={-1} tipo={mensaje.tipo}>
          {mensaje.texto}
        </Alerta>
      )}
      {abierto && (
        <form className="pila" onSubmit={(ev) => void alAgregar(ev)} noValidate>
          <CampoCorreos
            id={idArea}
            etiqueta="Correo del monitor"
            fichas={fichas}
            alCambiar={setFichas}
            deshabilitado={enviando}
          />
          <div className="pagina__acciones">
            <Boton type="submit" cargando={enviando} disabled={!puedeEnviar}>
              {textoBoton}
            </Boton>
            <Boton variante="secundario" onClick={cerrar} disabled={enviando}>
              Cancelar
            </Boton>
          </div>
        </form>
      )}
    </div>
  );
}
