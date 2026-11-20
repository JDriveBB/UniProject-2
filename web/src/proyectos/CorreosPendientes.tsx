import { useCallback, useEffect, useId, useRef, useState } from 'react';
import { generarEnlaceCorreo, listarCorreos, retirarCorreo } from '../api/proyectos';
import type { CorreoHabilitadoVista } from '../api/tipos';
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
import { pendientesDe, preguntaRetirarCorreo, retirarHabilitacion } from './quitar';

type EstadoCorreos =
  { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; correos: CorreoHabilitadoVista[] };

/** Correos habilitados sin cuenta (bloque 30, provisional sin Figma): solo el docente dueño; "Copiar enlace" (T-041) y "Retirar" (30f-5, 30f-6). */
export function CorreosPendientes({
  idProyecto,
  version,
  alCambiarHabilitaciones,
}: {
  idProyecto: number;
  version: number;
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

  const cargar = useCallback(async () => {
    try {
      setLista({ estado: 'listo', correos: await listarCorreos(idProyecto) });
    } catch (e) {
      setLista({ estado: 'error', mensaje: mensajeErrorApi(e, []) });
    }
  }, [idProyecto]);

  useEffect(() => {
    void cargar();
  }, [cargar, version]);

  useEffect(() => {
    if (porRetirar === null && volverA.current !== null) {
      (document.getElementById(volverA.current) ?? document.getElementById(idTitulo))?.focus();
      volverA.current = null;
    }
  }, [porRetirar, lista, idTitulo]);

  async function alCopiarEnlace(c: CorreoHabilitadoVista) {
    setMensaje(null);
    setEnlaceGenerado(null);
    setGenerandoId(c.idCorreoHabilitado);
    const r = await prepararEnlaceInvitacion(
      () => generarEnlaceCorreo(idProyecto, c.idCorreoHabilitado),
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
      await cargar();
      alCambiarHabilitaciones?.();
    }
    setMensaje({ tipo: 'error', texto: r.mensaje });
  }

  async function alConfirmarRetirar(c: CorreoHabilitadoVista) {
    setEnviando(true);
    setMensaje(null);
    setEnlaceGenerado(null);
    const r = await retirarHabilitacion({ retirarCorreo }, idProyecto, c);
    if (r.recargar) {
      await cargar();
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
        Correos habilitados sin cuenta
      </h3>
      {lista.estado === 'cargando' && <p className="texto-secundario">Cargando…</p>}
      {lista.estado === 'error' && <Alerta tipo="error">{lista.mensaje}</Alerta>}
      {lista.estado === 'listo' && pendientes.length === 0 && (
        <p className="texto-secundario">No hay correos pendientes.</p>
      )}
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
                  <Boton
                    variante="secundario"
                    aria-label={`Copiar enlace de invitación para ${c.email}`}
                    aria-busy={generandoEste}
                    disabled={ocupado}
                    onClick={() => void alCopiarEnlace(c)}
                  >
                    {generandoEste ? 'Generando…' : 'Copiar enlace'}
                  </Boton>
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
          pregunta={preguntaRetirarCorreo(porRetirar.email)}
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
