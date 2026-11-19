import { useId, useState } from 'react';
import { confirmarNota } from '../api/evaluacion';
import type { EvaluacionVista, NotaIndividualVista } from '../api/tipos';
import { Boton } from '../componentes/Boton';
import { Insignia } from '../componentes/Insignia';
import { Modal } from '../componentes/Modal';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaHora } from '../formato';
import type { Mensaje } from '../proyectos/errores';
import { textoFactor, textoNota } from './calculo';
import { confirmarTodas } from './confirmarTodas';
import { numeroNota, validarNota05 } from './formulario';
import {
  BOTON_CANCELAR,
  BOTON_CONFIRMAR_TODAS,
  CAMPOS_NOTA_FINAL,
  esConflicto,
  estadoNota,
  EXPLICACION_ICE,
  mensajeErrorEvaluacion,
  textoConfirmarTodas,
  TITULO_CONFIRMAR_TODAS,
} from './vista';

export interface PropsTablaNotasIndividuales {
  evaluacion: EvaluacionVista;
  alActualizar: (ev: EvaluacionVista, m: Mensaje) => void;
  alHecho: (m: Mensaje, recargar: boolean) => void;
}

export function TablaNotasIndividuales({ evaluacion, alActualizar, alHecho }: PropsTablaNotasIndividuales) {
  const [borradores, setBorradores] = useState<Record<number, string>>(() => {
    const inicial: Record<number, string> = {};
    for (const n of evaluacion.notas) {
      if (n.notaFinal !== null) {
        inicial[n.idEstudiante] = textoNota(n.notaFinal);
      } else if (n.notaSugerida !== null) {
        inicial[n.idEstudiante] = textoNota(n.notaSugerida);
      } else {
        inicial[n.idEstudiante] = '';
      }
    }
    return inicial;
  });

  const [errores, setErrores] = useState<Record<number, string>>({});
  const [enviando, setEnviando] = useState<number | null>(null);
  const [modalConfirmarTodas, setModalConfirmarTodas] = useState(false);
  const [enviandoTodas, setEnviandoTodas] = useState(false);

  const idBase = useId();
  const idBotonConfirmarTodas = `${idBase}-confirmar-todas`;

  const pendientes = evaluacion.notas.filter((n) => n.fechaConfirmacion === null);
  const accionEnCurso = enviando !== null || enviandoTodas;
  const hayErrores = Object.keys(errores).length > 0;

  async function confirmarFila(n: NotaIndividualVista) {
    const borrador = borradores[n.idEstudiante] ?? '';
    const error = validarNota05(borrador);
    if (error !== undefined) {
      setErrores((prev) => ({ ...prev, [n.idEstudiante]: error }));
      document.getElementById(`${idBase}-nota-${n.idEstudiante}`)?.focus();
      return;
    }

    setErrores((prev) => {
      const copia = { ...prev };
      delete copia[n.idEstudiante];
      return copia;
    });
    setEnviando(n.idEstudiante);

    try {
      const valorNumero = numeroNota(borrador);
      const ev = await confirmarNota(evaluacion.idEvaluacion, n.idEstudiante, valorNumero);
      const fila = ev.notas.find((item) => item.idEstudiante === n.idEstudiante);
      const valor = fila?.notaFinal ?? valorNumero;
      alActualizar(ev, {
        tipo: 'exito',
        texto: `Nota final de ${n.nombre} confirmada: ${textoNota(valor)}.`,
      });
    } catch (errApi: unknown) {
      alHecho({ tipo: 'error', texto: mensajeErrorEvaluacion(errApi, CAMPOS_NOTA_FINAL) }, esConflicto(errApi));
    } finally {
      setEnviando(null);
    }
  }

  function intentarConfirmarTodas() {
    const nuevosErrores: Record<number, string> = {};
    let primerInvalido: number | null = null;
    for (const n of pendientes) {
      const valor = borradores[n.idEstudiante] ?? '';
      const error = validarNota05(valor);
      if (error !== undefined) {
        nuevosErrores[n.idEstudiante] = error;
        if (primerInvalido === null) {
          primerInvalido = n.idEstudiante;
        }
      }
    }
    if (Object.keys(nuevosErrores).length > 0) {
      setErrores((prev) => ({ ...prev, ...nuevosErrores }));
      if (primerInvalido !== null) {
        document.getElementById(`${idBase}-nota-${primerInvalido}`)?.focus();
      }
      return;
    }
    setModalConfirmarTodas(true);
  }

  async function ejecutarConfirmarTodas() {
    setEnviandoTodas(true);
    try {
      const filasParaConfirmar = pendientes.map((n) => ({
        idEstudiante: n.idEstudiante,
        nombre: n.nombre,
        notaFinal: numeroNota(borradores[n.idEstudiante] ?? ''),
      }));

      const res = await confirmarTodas({ confirmarNota }, evaluacion.idEvaluacion, filasParaConfirmar);

      setModalConfirmarTodas(false);

      if (res.reemplazada) {
        const motivo = res.fallidas[0]?.motivo ?? 'La evaluación fue modificada por otro usuario.';
        alHecho({ tipo: 'error', texto: motivo }, true);
        return;
      }

      if (res.fallidas.length === 0) {
        const mensaje: Mensaje = {
          tipo: 'exito',
          texto: res.confirmadas === 1 ? 'Se confirmó 1 nota.' : `Se confirmaron ${res.confirmadas} notas.`,
        };
        if (res.evaluacion !== null) {
          alActualizar(res.evaluacion, mensaje);
        } else {
          alHecho(mensaje, false);
        }
        return;
      }

      const nombres = res.fallidas.map((f) => f.nombre).join(', ');
      const primerMotivo = res.fallidas[0].motivo;
      const base = `Se confirmaron ${res.confirmadas} de ${filasParaConfirmar.length}. No se pudo confirmar: ${nombres}.`;
      const textoError = primerMotivo ? `${base} ${primerMotivo}` : base;
      const mensajeError: Mensaje = { tipo: 'error', texto: textoError };

      if (res.evaluacion !== null) {
        alActualizar(res.evaluacion, mensajeError);
      } else {
        alHecho(mensajeError, false);
      }
    } catch (errInesperado: unknown) {
      setModalConfirmarTodas(false);
      alHecho(
        { tipo: 'error', texto: mensajeErrorEvaluacion(errInesperado, CAMPOS_NOTA_FINAL) },
        esConflicto(errInesperado),
      );
    } finally {
      setEnviandoTodas(false);
    }
  }

  const columnas: ColumnaTabla<NotaIndividualVista>[] = [
    {
      titulo: 'Estudiante',
      celda: (n) => n.nombre,
    },
    {
      titulo: 'ICE',
      clase: 'tabla__col-ice',
      celda: (n) => (n.factorAjuste !== null ? textoFactor(n.factorAjuste) : '—'),
    },
    {
      titulo: 'Sugerida',
      clase: 'tabla__col-nota tabla__col-sugerida',
      celda: (n) => (n.notaSugerida !== null ? textoNota(n.notaSugerida) : '—'),
    },
    {
      titulo: 'Nota final',
      clase: 'tabla__col-nota',
      celda: (n) => {
        const idCampo = `${idBase}-nota-${n.idEstudiante}`;
        const idError = `${idCampo}-error`;
        const error = errores[n.idEstudiante];
        return (
          <div>
            <input
              id={idCampo}
              className="tabla__entrada"
              inputMode="decimal"
              autoComplete="off"
              aria-label={`Nota final de ${n.nombre}`}
              aria-invalid={error !== undefined}
              aria-describedby={error !== undefined ? idError : undefined}
              disabled={accionEnCurso}
              value={borradores[n.idEstudiante] ?? ''}
              onChange={(e) => {
                const valor = e.target.value;
                setBorradores((prev) => ({ ...prev, [n.idEstudiante]: valor }));
                if (errores[n.idEstudiante] !== undefined) {
                  const errorActual = validarNota05(valor);
                  setErrores((prev) => {
                    const copia = { ...prev };
                    if (errorActual !== undefined) {
                      copia[n.idEstudiante] = errorActual;
                    } else {
                      delete copia[n.idEstudiante];
                    }
                    return copia;
                  });
                }
              }}
            />
            {error !== undefined && (
              <p id={idError} className="campo__error">
                {error}
              </p>
            )}
          </div>
        );
      },
    },
    {
      titulo: 'Estado',
      clase: 'tabla__col-estado-nota',
      celda: (n) => {
        const estado = estadoNota(n);
        return (
          <div className="estado-nota">
            <Insignia tono={estado.tono}>{estado.texto}</Insignia>
            {n.fechaConfirmacion !== null && <span className="texto-secundario">{fechaHora(n.fechaConfirmacion)}</span>}
          </div>
        );
      },
    },
    {
      titulo: 'Acción',
      clase: 'tabla__col-confirmar',
      celda: (n) => (
        <Boton
          type="button"
          variante="secundario"
          aria-label={`Confirmar la nota de ${n.nombre}`}
          cargando={enviando === n.idEstudiante}
          disabled={accionEnCurso}
          onClick={() => confirmarFila(n)}
        >
          Confirmar
        </Boton>
      ),
    },
  ];

  return (
    <>
      <Tarjeta
        titulo="Notas individuales"
        acciones={
          <Boton
            id={idBotonConfirmarTodas}
            type="button"
            variante="secundario"
            disabled={pendientes.length === 0 || accionEnCurso || hayErrores}
            onClick={intentarConfirmarTodas}
          >
            {BOTON_CONFIRMAR_TODAS}
          </Boton>
        }
      >
        <p className="texto-secundario">{EXPLICACION_ICE}</p>
        <Tabla
          titulo="Notas individuales del equipo"
          clase="tabla--evaluacion tabla--notas-individuales"
          claveFila={(n) => n.idEstudiante}
          vacio="La evaluación no tiene notas individuales."
          columnas={columnas}
          filas={evaluacion.notas}
        />
      </Tarjeta>

      {modalConfirmarTodas && (
        <Modal
          ancho="dialogo"
          titulo={TITULO_CONFIRMAR_TODAS}
          ocupado={enviandoTodas}
          alCerrar={() => {
            setModalConfirmarTodas(false);
            document.getElementById(idBotonConfirmarTodas)?.focus();
          }}
          alEnviar={() => void ejecutarConfirmarTodas()}
          acciones={(cerrar) => (
            <>
              <Boton type="button" variante="secundario" disabled={enviandoTodas} onClick={cerrar}>
                {BOTON_CANCELAR}
              </Boton>
              <Boton type="submit" variante="primario" cargando={enviandoTodas}>
                {BOTON_CONFIRMAR_TODAS}
              </Boton>
            </>
          )}
        >
          <p className="modal__texto">{textoConfirmarTodas(pendientes.length)}</p>
        </Modal>
      )}
    </>
  );
}
