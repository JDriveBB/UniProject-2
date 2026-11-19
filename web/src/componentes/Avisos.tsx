import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type FocusEvent,
  type ReactNode,
} from 'react';
import { crearColaAvisos, DURACION_AVISO_MS, type Aviso } from './colaAvisos';

const ContextoAvisos = createContext<((texto: string) => void) | null>(null);

interface ControlTemporizador {
  restanteMs: number;
  iniciadoEn: number | null;
  temporizador: number | null;
  mouseDentro: boolean;
  focoDentro: boolean;
}

type DestinoFoco = { tipo: 'aviso'; id: number } | { tipo: 'main' };

function hayDialogoAbierto(): boolean {
  return typeof document !== 'undefined' && document.querySelector('dialog[open]') !== null;
}

/**
 * Proveedor de avisos breves de éxito. Mantiene la región y su zona aria-live
 * presentes desde que carga la aplicación y espera a que no haya diálogos abiertos
 * antes de mostrar cada aviso.
 */
export function ProveedorAvisos({ children }: { children: ReactNode }) {
  const [cola] = useState(crearColaAvisos);
  const [avisos, setAvisos] = useState<readonly Aviso[]>([]);
  const [secuencias, setSecuencias] = useState<Readonly<Record<number, number>>>({});
  const [esperandoDialogo, setEsperandoDialogo] = useState(false);

  const pendientesRef = useRef<string[]>([]);
  const temporizadoresRef = useRef(new Map<number, ControlTemporizador>());
  const secuenciasRef = useRef(new Map<number, number>());
  const botonesRef = useRef(new Map<number, HTMLButtonElement>());
  const focoDespuesRef = useRef<DestinoFoco | null>(null);

  const limpiarRecursosId = useCallback((id: number) => {
    const ctrl = temporizadoresRef.current.get(id);
    if (ctrl !== undefined && ctrl.temporizador !== null) {
      window.clearTimeout(ctrl.temporizador);
    }
    temporizadoresRef.current.delete(id);
    secuenciasRef.current.delete(id);
    botonesRef.current.delete(id);
  }, []);

  const sincronizarCola = useCallback(() => {
    const listaActual = cola.lista();
    const vigentes = new Set(listaActual.map((a) => a.id));
    for (const id of [...temporizadoresRef.current.keys()]) {
      if (!vigentes.has(id)) {
        limpiarRecursosId(id);
      }
    }
    const mapaSecuencias: Record<number, number> = {};
    for (const aviso of listaActual) {
      mapaSecuencias[aviso.id] = secuenciasRef.current.get(aviso.id) ?? 1;
    }
    setAvisos(listaActual);
    setSecuencias(mapaSecuencias);
  }, [cola, limpiarRecursosId]);

  const cerrarPorId = useCallback(
    (id: number) => {
      limpiarRecursosId(id);
      cola.cerrar(id);
      sincronizarCola();
    },
    [cola, limpiarRecursosId, sincronizarCola],
  );

  const programar = useCallback(
    (id: number, ctrl: ControlTemporizador) => {
      if (ctrl.temporizador !== null) {
        window.clearTimeout(ctrl.temporizador);
        ctrl.temporizador = null;
      }
      if (ctrl.mouseDentro || ctrl.focoDentro) {
        ctrl.iniciadoEn = null;
        return;
      }
      ctrl.iniciadoEn = Date.now();
      ctrl.temporizador = window.setTimeout(() => {
        cerrarPorId(id);
      }, ctrl.restanteMs);
    },
    [cerrarPorId],
  );

  const pausar = useCallback((ctrl: ControlTemporizador) => {
    if (ctrl.temporizador !== null) {
      window.clearTimeout(ctrl.temporizador);
      ctrl.temporizador = null;
    }
    if (ctrl.iniciadoEn !== null) {
      const transcurrido = Date.now() - ctrl.iniciadoEn;
      ctrl.restanteMs = Math.max(0, ctrl.restanteMs - transcurrido);
      ctrl.iniciadoEn = null;
    }
  }, []);

  const mostrarTexto = useCallback(
    (texto: string) => {
      const id = cola.agregar(texto);
      secuenciasRef.current.set(id, (secuenciasRef.current.get(id) ?? 0) + 1);
      const existente = temporizadoresRef.current.get(id);
      if (existente !== undefined) {
        existente.restanteMs = DURACION_AVISO_MS;
        programar(id, existente);
      } else {
        const ctrl: ControlTemporizador = {
          restanteMs: DURACION_AVISO_MS,
          iniciadoEn: null,
          temporizador: null,
          mouseDentro: false,
          focoDentro: false,
        };
        temporizadoresRef.current.set(id, ctrl);
        programar(id, ctrl);
      }
      sincronizarCola();
    },
    [cola, programar, sincronizarCola],
  );

  const vaciarPendientes = useCallback(() => {
    const pendientes = pendientesRef.current;
    if (pendientes.length === 0) {
      setEsperandoDialogo(false);
      return;
    }
    pendientesRef.current = [];
    setEsperandoDialogo(false);
    for (const texto of pendientes) {
      mostrarTexto(texto);
    }
  }, [mostrarTexto]);

  const avisar = useCallback(
    (texto: string) => {
      if (texto.trim() === '') {
        return;
      }
      if (hayDialogoAbierto()) {
        pendientesRef.current.push(texto);
        setEsperandoDialogo(true);
        return;
      }
      mostrarTexto(texto);
    },
    [mostrarTexto],
  );

  useEffect(() => {
    if (!esperandoDialogo) {
      return;
    }
    function revisar() {
      if (!hayDialogoAbierto()) {
        vaciarPendientes();
      }
    }
    revisar();
    if (pendientesRef.current.length === 0) {
      return;
    }
    const observador = new MutationObserver(revisar);
    observador.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['open'],
    });
    document.addEventListener('close', revisar, true);
    const respaldo = window.setInterval(revisar, 150);
    return () => {
      observador.disconnect();
      document.removeEventListener('close', revisar, true);
      window.clearInterval(respaldo);
    };
  }, [esperandoDialogo, vaciarPendientes]);

  useEffect(() => {
    const destino = focoDespuesRef.current;
    if (destino === null) {
      return;
    }
    focoDespuesRef.current = null;
    if (destino.tipo === 'aviso') {
      const boton = botonesRef.current.get(destino.id);
      if (boton !== undefined) {
        boton.focus();
        return;
      }
    }
    const main = document.querySelector('main');
    if (main instanceof HTMLElement) {
      main.focus();
    }
  }, [avisos]);

  useEffect(() => {
    const mapa = temporizadoresRef.current;
    return () => {
      for (const ctrl of mapa.values()) {
        if (ctrl.temporizador !== null) {
          window.clearTimeout(ctrl.temporizador);
        }
      }
      mapa.clear();
    };
  }, []);

  function cerrarConBoton(id: number, porTeclado: boolean) {
    if (porTeclado) {
      const indice = avisos.findIndex((a) => a.id === id);
      const siguiente = indice >= 0 ? avisos[indice + 1] : undefined;
      focoDespuesRef.current = siguiente !== undefined ? { tipo: 'aviso', id: siguiente.id } : { tipo: 'main' };
    }
    cerrarPorId(id);
  }

  function alEntrarMouse(id: number) {
    const ctrl = temporizadoresRef.current.get(id);
    if (ctrl !== undefined) {
      ctrl.mouseDentro = true;
      pausar(ctrl);
    }
  }

  function alSalirMouse(id: number) {
    const ctrl = temporizadoresRef.current.get(id);
    if (ctrl !== undefined) {
      ctrl.mouseDentro = false;
      programar(id, ctrl);
    }
  }

  function alEntrarFoco(id: number) {
    const ctrl = temporizadoresRef.current.get(id);
    if (ctrl !== undefined) {
      ctrl.focoDentro = true;
      pausar(ctrl);
    }
  }

  function alSalirFoco(id: number, evento: FocusEvent<HTMLDivElement>) {
    if (evento.currentTarget.contains(evento.relatedTarget)) {
      return;
    }
    const ctrl = temporizadoresRef.current.get(id);
    if (ctrl !== undefined) {
      ctrl.focoDentro = false;
      programar(id, ctrl);
    }
  }

  return (
    <ContextoAvisos value={avisar}>
      {children}
      <div className="avisos" role="region" aria-label="Avisos">
        <div className="solo-lectores" aria-live="polite" aria-relevant="additions">
          {avisos.map((aviso) => (
            <p key={`${aviso.id}-${secuencias[aviso.id] ?? 1}`}>{aviso.texto}</p>
          ))}
        </div>
        {avisos.map((aviso) => (
          <div
            key={aviso.id}
            className="aviso"
            onMouseEnter={() => alEntrarMouse(aviso.id)}
            onMouseLeave={() => alSalirMouse(aviso.id)}
            onFocus={() => alEntrarFoco(aviso.id)}
            onBlur={(e) => alSalirFoco(aviso.id, e)}
            onKeyDown={(e) => {
              if (e.key === 'Escape') {
                e.preventDefault();
                e.stopPropagation();
                cerrarConBoton(aviso.id, true);
              }
            }}
          >
            <p className="aviso__texto">{aviso.texto}</p>
            <button
              ref={(el) => {
                if (el !== null) {
                  botonesRef.current.set(aviso.id, el);
                } else {
                  botonesRef.current.delete(aviso.id);
                }
              }}
              type="button"
              className="aviso__cerrar"
              aria-label="Cerrar aviso"
              onClick={(e) => cerrarConBoton(aviso.id, e.detail === 0)}
            >
              <span aria-hidden="true">×</span>
            </button>
          </div>
        ))}
      </div>
    </ContextoAvisos>
  );
}

export function useAvisos(): (texto: string) => void {
  const valor = useContext(ContextoAvisos);
  if (valor === null) {
    throw new Error('useAvisos se usó fuera de ProveedorAvisos');
  }
  return valor;
}
