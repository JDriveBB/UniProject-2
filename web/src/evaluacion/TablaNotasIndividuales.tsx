import { useId, useState } from 'react';
import { confirmarNota } from '../api/evaluacion';
import type { EvaluacionVista, NotaIndividualVista } from '../api/tipos';
import { Boton } from '../componentes/Boton';
import { Insignia } from '../componentes/Insignia';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaHora } from '../formato';
import type { Mensaje } from '../proyectos/errores';
import { textoFactor, textoNota } from './calculo';
import { numeroNota, validarNota05 } from './formulario';
import {
  CAMPOS_NOTA_FINAL,
  esConflicto,
  estadoNota,
  EXPLICACION_ICE,
  mensajeErrorEvaluacion,
} from './vista';

export interface PropsTablaNotasIndividuales {
  evaluacion: EvaluacionVista;
  alActualizar: (ev: EvaluacionVista, m: Mensaje) => void;
  alHecho: (m: Mensaje, recargar: boolean) => void;
}

export function TablaNotasIndividuales({
  evaluacion,
  alActualizar,
  alHecho,
}: PropsTablaNotasIndividuales) {
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

  const idBase = useId();

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
      alHecho(
        { tipo: 'error', texto: mensajeErrorEvaluacion(errApi, CAMPOS_NOTA_FINAL) },
        esConflicto(errApi)
      );
    } finally {
      setEnviando(null);
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
      clase: 'tabla__col-nota',
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
              disabled={enviando !== null}
              value={borradores[n.idEstudiante] ?? ''}
              onChange={(e) => {
                const valor = e.target.value;
                setBorradores((prev) => ({ ...prev, [n.idEstudiante]: valor }));
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
            {n.fechaConfirmacion !== null && (
              <span className="texto-secundario">{fechaHora(n.fechaConfirmacion)}</span>
            )}
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
          disabled={enviando !== null}
          onClick={() => confirmarFila(n)}
        >
          Confirmar
        </Boton>
      ),
    },
  ];

  return (
    <Tarjeta titulo="Notas individuales">
      <p className="texto-secundario">{EXPLICACION_ICE}</p>
      <Tabla
        titulo="Notas individuales del equipo"
        clase="tabla--evaluacion"
        claveFila={(n) => n.idEstudiante}
        vacio="La evaluación no tiene notas individuales."
        columnas={columnas}
        filas={evaluacion.notas}
      />
    </Tarjeta>
  );
}
