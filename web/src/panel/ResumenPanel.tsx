import type { ResumenGeneral } from './general';
import type { ResumenDelPanel } from './vista';

export function ResumenPanel({ resumen }: { resumen: ResumenDelPanel | ResumenGeneral }) {
  const { enRojo, equipos, porEvaluar, proxima } = resumen;
  const conAvance = 'avance' in resumen;

  const claseEnRojo = ['resumen-panel__valor', enRojo > 0 ? 'resumen-panel__valor--peligro' : '']
    .filter(Boolean)
    .join(' ');

  const clasePorEvaluar = [
    'resumen-panel__valor',
    porEvaluar > 0 ? 'resumen-panel__valor--primario' : 'resumen-panel__valor--apagado',
  ].join(' ');

  const textoAvance = conAvance && resumen.avance !== null ? `${resumen.avance} %` : '—';

  return (
    <div className={conAvance ? 'resumen-panel' : 'resumen-panel resumen-panel--tres'}>
      <a className="resumen-panel__dato resumen-panel__dato--enlace" href="#atencion">
        <p className="resumen-panel__etiqueta">Equipos en atención</p>
        <p className={claseEnRojo}>{`${enRojo} de ${equipos}`}</p>
      </a>
      <a className="resumen-panel__dato resumen-panel__dato--enlace" href="#atencion">
        <p className="resumen-panel__etiqueta">Por calificar</p>
        <p className={clasePorEvaluar}>{String(porEvaluar)}</p>
      </a>
      {conAvance && (
        <div className="resumen-panel__dato">
          <p className="resumen-panel__etiqueta">Avance del hito en curso</p>
          <p className="resumen-panel__valor">{textoAvance}</p>
        </div>
      )}
      <div className="resumen-panel__dato">
        <p className="resumen-panel__etiqueta">Próxima entrega</p>
        {proxima !== null ? (
          <>
            <p className="resumen-panel__entrega">{proxima.etiqueta}</p>
            <p className="resumen-panel__detalle">{proxima.plazo}</p>
          </>
        ) : (
          <p className="resumen-panel__entrega resumen-panel__valor--apagado">Sin entregas pendientes</p>
        )}
      </div>
    </div>
  );
}
