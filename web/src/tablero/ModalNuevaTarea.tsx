import { useEffect, useId, useRef, useState } from 'react';
import type { FormEvent, SyntheticEvent } from 'react';
import { agregarDependencias, crearTarea } from '../api/tareas';
import type { MiembroVista, TableroHito } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { CampoArea } from '../componentes/CampoArea';
import { Casilla } from '../componentes/Casilla';
import { fechaDia } from '../formato';
import type { Mensaje } from '../proyectos/errores';
import { crearConDependencias, primerCampoConError, TAREA_VACIA, validarTarea } from './nuevaTarea';
import type { ApiNuevaTarea, BorradorTarea, CampoTarea, ErroresTarea } from './nuevaTarea';
import { SelectorPrerrequisitos } from './SelectorPrerrequisitos';
import { etiquetaHito } from './ubicacion';
import type { HitoDelEquipo } from './ubicacion';

const API_NUEVA_TAREA: ApiNuevaTarea = { crearTarea, agregarDependencias };

/**
 * P05 Nueva tarea (Figma nodo 6:200) como modal sobre el tablero (decisión 26c-3): dialog con showModal() (fondo
 * inerte, foco atrapado, capa). RNF5 en 4 clics: «Nueva tarea» → «Bloqueada por» → la tarea → «Guardar» (el título
 * ya tiene el foco; escribir y pasar de campo con Tab no son clics). Escape cierra primero la lista y luego el modal.
 */
export function ModalNuevaTarea({ tablero, hito, miembros, codigos, alCerrar, alCrear }: {
  tablero: TableroHito;
  hito: HitoDelEquipo;
  miembros: MiembroVista[];
  codigos: Map<number, string>;
  alCerrar: () => void;
  alCrear: (mensaje: Mensaje) => void;
}) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const [borrador, setBorrador] = useState<BorradorTarea>(TAREA_VACIA);
  const [intentado, setIntentado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [listaAbierta, setListaAbierta] = useState(false);
  const idTitulo = useId();
  const idSubtitulo = useId();
  const idCampoTitulo = useId();
  const idDescripcion = useId();
  const idEncargado = useId();
  const idHoras = useId();
  const idFecha = useId();
  const idSelector = useId();
  const ids: Record<CampoTarea, string> = {
    titulo: idCampoTitulo,
    descripcion: idDescripcion,
    horas: idHoras,
    fechaLimite: idFecha,
    prerrequisitos: idSelector,
  };

  useEffect(() => {
    const d = dialogo.current;
    if (d !== null && !d.open) {
      d.showModal();
    }
    document.getElementById(idCampoTitulo)?.focus();
  }, [idCampoTitulo]);

  const errores: ErroresTarea = intentado ? validarTarea(borrador, hito) : {};

  function cambiar(parcial: Partial<BorradorTarea>) {
    setBorrador((b) => ({ ...b, ...parcial }));
  }

  /** Escape: con la lista abierta, solo la cierra; mientras guarda, no cierra. Si no, el dialog se cierra (onClose). */
  function alCancelar(evento: SyntheticEvent<HTMLDialogElement>) {
    if (listaAbierta) {
      evento.preventDefault();
      setListaAbierta(false);
      document.getElementById(idSelector)?.focus();
    } else if (guardando) {
      evento.preventDefault();
    }
  }

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (guardando) {
      return;
    }
    setIntentado(true);
    const primero = primerCampoConError(validarTarea(borrador, hito));
    if (primero !== null) {
      document.getElementById(ids[primero])?.focus();
      return;
    }
    setGuardando(true);
    setError(null);
    const resultado = await crearConDependencias(API_NUEVA_TAREA, tablero.idHitoEquipo, borrador);
    if (resultado.tipo === 'error') {
      setGuardando(false);
      setError(resultado.texto);
      return;
    }
    alCrear(resultado);
  }

  return (
    <dialog
      ref={dialogo}
      className="modal"
      aria-labelledby={idTitulo}
      aria-describedby={idSubtitulo}
      onCancel={alCancelar}
      onClose={alCerrar}
    >
      <form className="modal__formulario" noValidate onSubmit={(e) => void guardar(e)}>
        <header className="modal__cabecera">
          <h2 id={idTitulo} className="modal__titulo">
            Nueva tarea
          </h2>
          <p id={idSubtitulo} className="modal__subtitulo">
            {`${etiquetaHito(hito.orden, hito.nombreHito)} · ${tablero.nombreEquipo}`}
          </p>
        </header>
        {error !== null && <Alerta tipo="error">{error}</Alerta>}
        <Campo
          id={idCampoTitulo}
          etiqueta="Título"
          value={borrador.titulo}
          error={errores.titulo}
          disabled={guardando}
          onChange={(e) => cambiar({ titulo: e.target.value })}
        />
        <CampoArea
          id={idDescripcion}
          etiqueta="Descripción"
          className="campo--descripcion-tarea"
          value={borrador.descripcion}
          error={errores.descripcion}
          disabled={guardando}
          onChange={(e) => cambiar({ descripcion: e.target.value })}
        />
        <div className="campo">
          <label className="campo__etiqueta" htmlFor={idEncargado}>
            Encargado
          </label>
          <select
            id={idEncargado}
            className="campo__entrada"
            value={borrador.idEncargado}
            disabled={guardando}
            onChange={(e) => cambiar({ idEncargado: e.target.value })}
          >
            <option value="">Sin asignar</option>
            {miembros.map((m) => (
              <option key={m.idUsuario} value={String(m.idUsuario)}>
                {m.nombre}
              </option>
            ))}
          </select>
        </div>
        <div className="fila-campos">
          <Campo
            id={idHoras}
            etiqueta="Horas estimadas"
            inputMode="decimal"
            autoComplete="off"
            value={borrador.horas}
            error={errores.horas}
            disabled={guardando}
            onChange={(e) => cambiar({ horas: e.target.value })}
          />
          <Campo
            id={idFecha}
            etiqueta="Fecha límite"
            type="date"
            min={hito.fechaInicio}
            max={hito.fechaLimite}
            value={borrador.fechaLimite}
            error={errores.fechaLimite}
            disabled={guardando}
            onChange={(e) => cambiar({ fechaLimite: e.target.value })}
          />
        </div>
        <p className="texto-ayuda modal__ayuda">
          {`Sin fecha límite, la tarea vence con el hito (${fechaDia(hito.fechaLimite)}).`}
        </p>
        <Casilla checked={borrador.critico} disabled={guardando} onChange={(e) => cambiar({ critico: e.target.checked })}>
          Prerrequisito crítico del hito
        </Casilla>
        <SelectorPrerrequisitos
          id={idSelector}
          tareas={tablero.tareas}
          codigos={codigos}
          elegidas={borrador.prerrequisitos}
          abierto={listaAbierta}
          error={errores.prerrequisitos}
          deshabilitado={guardando}
          alAbrir={setListaAbierta}
          alCambiar={(elegidas) => cambiar({ prerrequisitos: elegidas })}
        />
        <div className="modal__acciones">
          <Boton variante="secundario" disabled={guardando} onClick={() => dialogo.current?.close()}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardando}>
            Guardar
          </Boton>
        </div>
      </form>
    </dialog>
  );
}
