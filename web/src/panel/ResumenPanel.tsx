import type { ResumenDelPanel } from './vista';

export function ResumenPanel({ resumen }: { resumen: ResumenDelPanel }) {
  const { enRojo, equipos, porEvaluar, avance, proxima } = resumen;

  const claseEnRojo = ['resumen-panel__valor', enRojo > 0 ? 'resumen-panel__valor--peligro' : '']
    .filter(Boolean)
    .join(' ');

  const clasePorEvaluar = [
    'resumen-panel__valor',
    porEvaluar > 0 ? 'resumen-panel__valor--primario' : 'resumen-panel__valor--apagado',
  ].join(' ');

  const textoAvance = avance !== null ? `${avance} %` : '—';

  return (
    <div className="resumen-panel">
      <a className="resumen-panel__dato resumen-panel__dato--enlace" href="#atencion">
        <p className="resumen-panel__etiqueta">Equipos en atención</p>
        <p className={claseEnRojo}>{`${enRojo} de ${equipos}`}</p>
      </a>
      <a className="resumen-panel__dato resumen-panel__dato--enlace" href="#atencion">
        <p className="resumen-panel__etiqueta">Por calificar</p>
        <p className={clasePorEvaluar}>{String(porEvaluar)}</p>
      </a>
      <div className="resumen-panel__dato">
        <p className="resumen-panel__etiqueta">Avance promedio</p>
        <p className="resumen-panel__valor">{textoAvance}</p>
      </div>
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
