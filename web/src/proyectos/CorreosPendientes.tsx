import { useCallback, useEffect, useId, useRef, useState } from 'react';
import type { CorreoHabilitadoVista, EnlaceRegistro } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { ConfirmarQuitar } from '../componentes/ConfirmarQuitar';
import { Insignia } from '../componentes/Insignia';
import { prepararEnlaceInvitacion } from '../cuentas/enlace';
import type { EnlaceCopiado } from '../cuentas/enlace';
import { fechaCorta } from '../formato';
import { etiquetaRol } from '../marco/navegacion';
import { mensajeErrorApi } from './errores';
import type { Mensaje } from './errores';
import { pendientesDe, retirarHabilitacion } from './quitar';

type EstadoCorreos =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; correos: CorreoHabilitadoVista[] };

/**
 * Lista de correos habilitados sin cuenta: sirve para los monitores del proyecto y para el curso. Quien la usa pasa
 * funciones estables (useCallback). Si generarEnlace es null, no muestra «Copiar enlace».
 */
export function CorreosPendientes({
  titulo,
  version,
  cargar,
  generarEnlace,
  retirar,
  preguntaRetirar,
  textoVacio = 'No hay correos pendientes.',
  alCambiarHabilitaciones,
}: {
  titulo: string;
  version: number;
  cargar: () => Promise<CorreoHabilitadoVista[]>;
  generarEnlace: ((idCorreoHabilitado: number) => Promise<EnlaceRegistro>) | null;
  retirar: (idCorreoHabilitado: number) => Promise<void>;
  preguntaRetirar: (correo: string) => string;
  textoVacio?: string;
  alCambiarHabilitaciones?: () => void;
}) {
  const base = useId();
  const [lista, setLista] = useState<EstadoCorreos>({ estado: 'cargando' });
  const [enviando, setEnviando] = useState(false);
  const [generandoId, setGenerandoId] = useState<number | null>(null);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [enlaceGenerado, setEnlaceGenerado] = useState<EnlaceCopiado | null>(null);
  const [porRetirar, setPorRetirar] = useState<CorreoHabilitadoVista | null>(null);
  const volverA = useRef<string | null>(null);

  const idTitulo = `${base}-titulo`;
  const idRetirar = (idCorreoHabilitado: number) => `${base}-retirar-${idCorreoHabilitado}`;

  const recargar = useCallback(async () => {
    try {
      setLista({ estado: 'listo', correos: await cargar() });
    } catch (e) {
      setLista({ estado: 'error', mensaje: mensajeErrorApi(e, []) });
    }
  }, [cargar]);

  useEffect(() => {
    void recargar();
  }, [recargar, version]);

  useEffect(() => {
    if (porRetirar === null && volverA.current !== null) {
      (document.getElementById(volverA.current) ?? document.getElementById(idTitulo))?.focus();
      volverA.current = null;
    }
  }, [porRetirar, lista, idTitulo]);

  async function alCopiarEnlace(c: CorreoHabilitadoVista) {
    if (generarEnlace === null) {
      return;
    }
    setMensaje(null);
    setEnlaceGenerado(null);
    setGenerandoId(c.idCorreoHabilitado);
    const r = await prepararEnlaceInvitacion(
      () => generarEnlace(c.idCorreoHabilitado),
      c.email,
      window.location.origin,
      (e) => mensajeErrorApi(e, []),
    );
    setGenerandoId(null);
    if (r.ok) {
      setEnlaceGenerado(r.enlace);
      return;
    }
    if (r.recargar) {
      await recargar();
      alCambiarHabilitaciones?.();
    }
    setMensaje({ tipo: 'error', texto: r.mensaje });
  }

  async function alConfirmarRetirar(c: CorreoHabilitadoVista) {
    setEnviando(true);
    setMensaje(null);
    setEnlaceGenerado(null);
    const r = await retirarHabilitacion(retirar, c);
    if (r.recargar) {
      await recargar();
      alCambiarHabilitaciones?.();
    }
    setEnviando(false);
    setMensaje(r.mensaje);
    volverA.current = r.recargar ? idTitulo : idRetirar(c.idCorreoHabilitado);
    setPorRetirar(null);
  }

  function alCancelarRetirar(c: CorreoHabilitadoVista) {
    volverA.current = idRetirar(c.idCorreoHabilitado);
    setPorRetirar(null);
  }

  const pendientes = lista.estado === 'listo' ? pendientesDe(lista.correos) : [];
  const ocupado = enviando || porRetirar !== null || generandoId !== null;

  return (
    <div className="correos-pendientes">
      <h3 id={idTitulo} className="tarjeta__subtitulo" tabIndex={-1}>
        {titulo}
      </h3>
      {lista.estado === 'cargando' && <p className="texto-secundario">Cargando…</p>}
      {lista.estado === 'error' && <Alerta tipo="error">{lista.mensaje}</Alerta>}
      {lista.estado === 'listo' && pendientes.length === 0 && <p className="texto-secundario">{textoVacio}</p>}
      {lista.estado === 'listo' && pendientes.length > 0 && (
        <ul className="lista-pendientes" aria-labelledby={idTitulo}>
          {pendientes.map((c) => {
            const generandoEste = generandoId === c.idCorreoHabilitado;
            return (
              <li key={c.idCorreoHabilitado} className="correo-pendiente">
                <div className="correo-pendiente__datos">
                  <span className="correo-pendiente__correo">{c.email}</span>
                  <Insignia>{etiquetaRol(c.rol)}</Insignia>
                  <span className="correo-pendiente__fecha">{`Habilitado el ${fechaCorta(c.fechaHabilitacion)}`}</span>
                </div>
                <div className="correo-pendiente__acciones">
                  {generarEnlace !== null && (
                    <Boton
                      variante="secundario"
                      aria-label={`Copiar enlace de invitación para ${c.email}`}
                      aria-busy={generandoEste}
                      disabled={ocupado}
                      onClick={() => void alCopiarEnlace(c)}
                    >
                      {generandoEste ? 'Generando…' : 'Copiar enlace'}
                    </Boton>
                  )}
                  <Boton
                    id={idRetirar(c.idCorreoHabilitado)}
                    variante="secundario"
                    aria-label={`Retirar la habilitación de ${c.email}`}
                    disabled={ocupado}
                    onClick={() => setPorRetirar(c)}
                  >
                    Retirar
                  </Boton>
                </div>
              </li>
            );
          })}
        </ul>
      )}
      {porRetirar !== null && (
        <ConfirmarQuitar
          key={porRetirar.idCorreoHabilitado}
          pregunta={preguntaRetirar(porRetirar.email)}
          ocupado={enviando}
          textoConfirmar="Sí, retirar"
          textoOcupado="Retirando…"
          alConfirmar={() => void alConfirmarRetirar(porRetirar)}
          alCancelar={() => alCancelarRetirar(porRetirar)}
        />
      )}
      {mensaje !== null && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
      {enlaceGenerado !== null && (
        <div className="enlace-generado">
          <Alerta tipo="exito">{enlaceGenerado.texto}</Alerta>
          <Campo
            etiqueta={`Enlace de invitación para ${enlaceGenerado.email}`}
            readOnly
            value={enlaceGenerado.url}
            onFocus={(e) => e.currentTarget.select()}
            onClick={(e) => e.currentTarget.select()}
          />
        </div>
      )}
    </div>
  );
}
