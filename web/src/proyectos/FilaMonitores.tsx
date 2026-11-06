import { useCallback, useEffect, useId, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { habilitarCorreos, listarMonitores, quitarMonitor } from '../api/proyectos';
import type { MonitorVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { Insignia } from '../componentes/Insignia';
import { validarCorreo } from '../cuentas/validacion';
import { agregarMonitor } from './equipos';
import { mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';
import { preguntaQuitarMonitor, quitarMonitorDelProyecto } from './quitar';
import type { ApiQuitar } from './quitar';

const API_QUITAR: Pick<ApiQuitar, 'quitarMonitor'> = { quitarMonitor };

type EstadoMonitores =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; monitores: MonitorVista[] };

/** Fila "Monitores:" de P03 (RF14), con quitar en cada insignia (Aclaración 2). Solo la ve el docente dueño. */
export function FilaMonitores({ idProyecto }: { idProyecto: number }) {
  const base = useId();
  const [lista, setLista] = useState<EstadoMonitores>({ estado: 'cargando' });
  const [abierto, setAbierto] = useState(false);
  const [email, setEmail] = useState('');
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [porQuitar, setPorQuitar] = useState<MonitorVista | null>(null);
  const volverA = useRef<string | null>(null);
  const errorEmail = intentado ? (validarCorreo(email) ?? undefined) : undefined;
  const idAgregar = base + '-agregar';
  const idTitulo = base + '-titulo';
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
  }, [cargar]);

  /* Al cerrar la confirmación, el foco va a donde corresponde (26b-15); si ya no existe, al título (Aclaración 3). */
  useEffect(() => {
    if (porQuitar === null && volverA.current !== null) {
      (document.getElementById(volverA.current) ?? document.getElementById(idTitulo))?.focus();
      volverA.current = null;
    }
  }, [porQuitar, lista, idTitulo]);

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
    setEnviando(true);
    const r = await agregarMonitor({ habilitarCorreos }, idProyecto, email);
    setEnviando(false);
    setMensaje(r);
    if (r.tipo !== 'error') {
      cerrar();
      await cargar();
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
          <Boton id={idAgregar} variante="secundario" onClick={() => setAbierto(true)}>
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
      {abierto && (
        <form className="formulario-en-linea" onSubmit={(ev) => void alAgregar(ev)} noValidate>
          <Campo
            etiqueta="Correo del monitor"
            type="email"
            placeholder="nombre@uninorte.edu.co"
            autoComplete="off"
            maxLength={254}
            value={email}
            onChange={(ev) => setEmail(ev.target.value)}
            error={errorEmail}
            required
          />
          <Boton type="submit" cargando={enviando}>
            Agregar
          </Boton>
          <Boton variante="secundario" onClick={cerrar} disabled={enviando}>
            Cancelar
          </Boton>
        </form>
      )}
      {mensaje !== null && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
    </div>
  );
}
