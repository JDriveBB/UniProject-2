import { useState } from 'react';
import { agregarDependencias, quitarDependencia } from '../api/tareas';
import type { TareaVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Casilla } from '../componentes/Casilla';
import { Insignia } from '../componentes/Insignia';
import { Modal } from '../componentes/Modal';
import type { Mensaje } from '../proyectos/errores';
import { nombreConCodigo } from '../tablero/marcas';
import { guardarDependencias, textoCiclo } from './dependencias';

export interface PropsModalDependencias {
  tarea: TareaVista;
  tareas: TareaVista[];
  codigos: Map<number, string>;
  alCerrar: () => void;
  alGuardar: (m: Mensaje) => void;
}

export function ModalDependencias({
  tarea,
  tareas,
  codigos,
  alCerrar,
  alGuardar,
}: PropsModalDependencias) {
  const [elegidas, setElegidas] = useState<number[]>([...tarea.prerrequisitos]);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [huboCambios, setHuboCambios] = useState(false);
  const [excluidas, setExcluidas] = useState<number[] | null>(null);

  const demasTareas = tareas
    .filter((t) => t.idTarea !== tarea.idTarea)
    .sort((a, b) => a.idTarea - b.idTarea);

  const nombresExcluidas =
    excluidas !== null
      ? excluidas.map((id) => {
          const t = tareas.find((cand) => cand.idTarea === id);
          return t !== undefined ? nombreConCodigo(codigos, t) : (codigos.get(id) ?? `Tarea ${id}`);
        })
      : [];

  const avisoCicloTexto = excluidas !== null ? textoCiclo(nombresExcluidas) : null;

  function cerrarModal() {
    if (avisoCicloTexto !== null) {
      alGuardar({ tipo: 'aviso', texto: avisoCicloTexto });
    } else if (huboCambios) {
      alGuardar({
        tipo: 'aviso',
        texto: 'Algunas dependencias sí cambiaron antes del error. Revisa la tarea.',
      });
    } else {
      alCerrar();
    }
  }

  async function enviar() {
    if (avisoCicloTexto !== null) {
      alGuardar({ tipo: 'aviso', texto: avisoCicloTexto });
      return;
    }

    setGuardando(true);
    setError(null);
    const res = await guardarDependencias(
      { agregarDependencias, quitarDependencia },
      tarea.idTarea,
      tarea.prerrequisitos,
      elegidas
    );
    setGuardando(false);

    if (res.tipo === 'sinCambios') {
      alCerrar();
    } else if (res.tipo === 'error') {
      setError(res.texto);
      if (res.huboCambios) {
        setHuboCambios(true);
      }
    } else if (res.tipo === 'guardado') {
      if (res.ciclicos.length === 0) {
        alGuardar({
          tipo: 'exito',
          texto: 'Dependencias guardadas. La ruta crítica se recalculó.',
        });
      } else {
        setExcluidas(res.ciclicos);
        setElegidas((prev) => prev.filter((id) => !res.ciclicos.includes(id)));
        setHuboCambios(true);
      }
    }
  }

  function conmutar(id: number, marcada: boolean) {
    if (marcada) {
      setElegidas((prev) => [...prev, id]);
    } else {
      setElegidas((prev) => prev.filter((item) => item !== id));
    }
  }

  // Fallo parcial: lo ya guardado dejó `tarea.prerrequisitos` desactualizado; reintentar repetiría cambios (404).
  // Solo queda Cancelar, que recarga la página con el aviso.
  const bloqueado = error !== null && huboCambios;
  const subtitulo = `${nombreConCodigo(codigos, tarea)}. Elige las tareas que deben terminar antes de esta.`;

  return (
    <Modal
      ancho="tarea"
      titulo="Establecer dependencias"
      subtitulo={subtitulo}
      ocupado={guardando}
      alCerrar={cerrarModal}
      alEnviar={() => void enviar()}
      acciones={() => (
        <>
          {excluidas === null && (
            <Boton type="button" variante="secundario" onClick={cerrarModal}>
              Cancelar
            </Boton>
          )}
          <Boton type="submit" variante="primario" cargando={guardando} disabled={bloqueado}>
            Aceptar
          </Boton>
        </>
      )}
    >
      {error !== null && <Alerta tipo="error">{error}</Alerta>}
      {avisoCicloTexto !== null && (
        <p className="aviso-modal aviso-modal--peligro" role="status">
          {avisoCicloTexto}
        </p>
      )}

      {demasTareas.length === 0 ? (
        <p className="pagina__vacio">No hay otras tareas en este hito.</p>
      ) : (
        <ul className="lista-dependencias" aria-label="Tareas del hito">
          {demasTareas.map((t) => {
            const esExcluida = excluidas !== null && excluidas.includes(t.idTarea);
            const marcada = elegidas.includes(t.idTarea);
            const clasesOpcion = [
              'opcion-dependencia',
              esExcluida ? 'opcion-dependencia--excluida' : '',
            ]
              .filter(Boolean)
              .join(' ');

            return (
              <li key={t.idTarea} className={clasesOpcion}>
                <Casilla
                  checked={marcada}
                  disabled={guardando || excluidas !== null || bloqueado}
                  onChange={(e) => conmutar(t.idTarea, e.target.checked)}
                >
                  {nombreConCodigo(codigos, t)}
                </Casilla>
                {esExcluida && <Insignia tono="peligro">Excluida por ciclo</Insignia>}
              </li>
            );
          })}
        </ul>
      )}
    </Modal>
  );
}
