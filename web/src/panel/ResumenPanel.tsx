import type { ResumenDelPanel } from './vista';

export function ResumenPanel({ resumen }: { resumen: ResumenDelPanel }) {
  const { enRojo, equipos, porEvaluar, avance } = resumen;

  const claseEnRojo = ['dato-resumen__valor', enRojo > 0 ? 'dato-resumen__valor--peligro' : '']
    .filter(Boolean)
    .join(' ');

  const clasePorEvaluar = [
    'dato-resumen__valor',
    porEvaluar > 0 ? 'dato-resumen__valor--primario' : 'dato-resumen__valor--apagado',
  ].join(' ');

  const textoAvance = avance !== null ? `${avance} %` : '—';

  return (
    <div className="resumen-ruta">
      <a className="dato-resumen dato-resumen--enlace" href="#equipos">
        <p className="dato-resumen__etiqueta">Equipos que requieren atención</p>
        <p className={claseEnRojo}>{`${enRojo} de ${equipos}`}</p>
      </a>
      <a className="dato-resumen dato-resumen--enlace" href="#por-evaluar">
        <p className="dato-resumen__etiqueta">Hitos por evaluar</p>
        <p className={clasePorEvaluar}>{String(porEvaluar)}</p>
      </a>
      <div className="dato-resumen">
        <p className="dato-resumen__etiqueta">Avance promedio</p>
        <p className="dato-resumen__valor">{textoAvance}</p>
        <p className="dato-resumen__nota">Promedio de las tareas de cada equipo</p>
      </div>
    </div>
  );
}
