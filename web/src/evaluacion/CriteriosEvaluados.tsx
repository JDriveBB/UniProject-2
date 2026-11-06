import type { CriterioEvaluadoVista, EvaluacionVista } from '../api/tipos';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { Tarjeta } from '../componentes/Tarjeta';
import { decimalEsCo } from '../formato';
import { textoNota } from './calculo';

export interface PropsCriteriosEvaluados {
  evaluacion: EvaluacionVista;
}

const COLUMNAS: ColumnaTabla<CriterioEvaluadoVista>[] = [
  {
    titulo: 'Criterio',
    celda: (c) => c.descripcion,
  },
  {
    titulo: 'Peso (%)',
    clase: 'tabla__col-peso',
    celda: (c) => `${decimalEsCo(c.pesoPorcentaje)} %`,
  },
  {
    titulo: 'Nota',
    clase: 'tabla__col-nota',
    celda: (c) => `${textoNota(c.nota)} / ${textoNota(c.puntajeMaximo)}`,
  },
  {
    titulo: 'Retroalimentación',
    celda: (c) =>
      c.retroalimentacion !== null ? (
        <p className="texto-largo">{c.retroalimentacion}</p>
      ) : (
        <span className="texto-secundario">—</span>
      ),
  },
];

export function CriteriosEvaluados({ evaluacion }: PropsCriteriosEvaluados) {
  return (
    <Tarjeta titulo="Rúbrica de evaluación">
      <Tabla
        titulo="Calificación por criterio"
        clase="tabla--evaluacion"
        claveFila={(c) => c.idCriterio}
        vacio="La evaluación no tiene criterios."
        columnas={COLUMNAS}
        filas={evaluacion.criterios}
      />
      {evaluacion.retroalimentacion !== null && (
        <>
          <h3 className="tarjeta__subtitulo">Retroalimentación general</h3>
          <p className="texto-largo">{evaluacion.retroalimentacion}</p>
        </>
      )}
    </Tarjeta>
  );
}
