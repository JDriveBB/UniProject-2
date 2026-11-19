import { useEffect, useRef } from 'react';
import { Link } from 'react-router';
import { Insignia } from '../componentes/Insignia';
import { rutas } from '../rutas';
import { estadoHito } from './marcas';
import { etiquetaHito } from './ubicacion';
import type { HitoDelEquipo } from './ubicacion';

/**
 * Pestañas de hitos de P04 (decisión 26c-2): enlaces que cambian ?hito= (no role="tab"), la activa con
 * aria-current. Un hito enviado o evaluado lleva su insignia (interpretación C3); en curso, ninguna.
 */
export function PestanasHitos({
  idEquipo,
  hitos,
  activo,
  alElegir,
}: {
  idEquipo: number;
  hitos: HitoDelEquipo[];
  activo: number;
  alElegir: () => void;
}) {
  const ref = useRef<HTMLElement>(null);

  useEffect(() => {
    // No se usa scrollIntoView porque movería también la página en vertical.
    const barra = ref.current;
    const pestana = barra?.querySelector<HTMLElement>('[aria-current="page"]');
    if (!barra || !pestana) {
      return;
    }
    const b = barra.getBoundingClientRect();
    const p = pestana.getBoundingClientRect();
    if (p.left < b.left || p.right > b.right) {
      barra.scrollLeft += p.left - b.left;
    }
  }, [activo]);

  return (
    <nav ref={ref} className="pestanas" aria-label="Hitos del equipo">
      {hitos.map((h) => {
        const esActivo = h.idHitoEquipo === activo;
        const estado = estadoHito(h.estado);
        return (
          <Link
            key={h.idHitoEquipo}
            to={rutas.tableroEnHito(idEquipo, h.idHitoEquipo)}
            className={esActivo ? 'pestana pestana--activa' : 'pestana'}
            aria-current={esActivo ? 'page' : undefined}
            onClick={alElegir}
          >
            {etiquetaHito(h.orden, h.nombreHito)}
            {h.estado !== 'EN_CURSO' && <Insignia tono={estado.tono}>{estado.texto}</Insignia>}
          </Link>
        );
      })}
    </nav>
  );
}
