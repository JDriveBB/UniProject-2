import type { EvaluacionVista, NotaIndividualVista } from '../api/tipos';
import { Insignia } from '../componentes/Insignia';
import { fechaCorta } from '../formato';
import { textoNota } from './calculo';
import { CARACTER, NOTA_AJUSTADA, notaAjustada } from './vista';

export interface PropsResumenEvaluacion {
  evaluacion: EvaluacionVista;
  vista: 'docente' | 'estudiante' | 'monitor';
  notaPropia: NotaIndividualVista | null;
}

export function ResumenEvaluacion({ evaluacion, vista, notaPropia }: PropsResumenEvaluacion) {
  const caracterMarca = CARACTER[evaluacion.caracter];

  return (
    <div className="resumen-ruta">
      <div className="dato-resumen">
        <p className="dato-resumen__etiqueta">Nota grupal</p>
        <p className="dato-resumen__valor dato-resumen__valor--primario">
          {textoNota(evaluacion.notaGrupal)}
        </p>
        {vista === 'docente' && notaAjustada(evaluacion) && (
          <div>
            <Insignia tono={NOTA_AJUSTADA.tono}>{NOTA_AJUSTADA.texto}</Insignia>
          </div>
        )}
      </div>

      {vista === 'docente' && (
        <div className="dato-resumen">
          <p className="dato-resumen__etiqueta">Nota calculada</p>
          <p className="dato-resumen__valor">
            {evaluacion.notaCalculada !== null ? textoNota(evaluacion.notaCalculada) : '—'}
          </p>
        </div>
      )}

      {vista === 'estudiante' && (
        <div className="dato-resumen">
          <p className="dato-resumen__etiqueta">Tu nota individual</p>
          <p className="dato-resumen__valor">
            {notaPropia !== null && notaPropia.notaFinal !== null
              ? textoNota(notaPropia.notaFinal)
              : 'Pendiente'}
          </p>
        </div>
      )}

      <div className="dato-resumen">
        <p className="dato-resumen__etiqueta">Carácter</p>
        <div>
          <Insignia tono={caracterMarca.tono}>{caracterMarca.texto}</Insignia>
        </div>
        <p className="dato-resumen__etiqueta">
          Evaluada el {fechaCorta(evaluacion.fechaEvaluacion)}
        </p>
      </div>
    </div>
  );
}
