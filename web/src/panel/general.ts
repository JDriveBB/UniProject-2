import type { ProyectoVista } from '../api/tipos';
import { estadoProyecto, resumenDocente } from '../proyectos/tarjetas';
import type { Marca } from '../tablero/marcas';
import { cargarPanel, type ApiCargaPanel, type DatosPanel } from './carga';
import {
  atencionDelDia,
  entregasDelProyecto,
  porEvaluar,
  type ItemAtencion,
  type ResumenDelPanel,
  type TipoAtencion,
} from './vista';

export interface PanelDeProyecto {
  proyecto: ProyectoVista;
  datos: DatosPanel | null;
}

export type ResumenGeneral = Omit<ResumenDelPanel, 'avance'>;

export interface ProyectoDelPanel {
  idProyecto: number;
  nombre: string;
  marca: Marca | null;
}

const ORDEN_ATENCION: Record<TipoAtencion, number> = {
  vencido: 0,
  equipo: 1,
  calificar: 2,
  proximo: 3,
  riesgo: 4,
};

/** Carga en paralelo los paneles de los proyectos propios; un fallo deja datos en null. */
export async function cargarPanelesPropios(
  api: ApiCargaPanel,
  proyectos: readonly ProyectoVista[],
): Promise<PanelDeProyecto[]> {
  const resultados = await Promise.allSettled(proyectos.map((p) => cargarPanel(api, p.idProyecto)));
  return proyectos.map((proyecto, i) => {
    const resultado = resultados[i];
    return {
      proyecto,
      datos: resultado.status === 'fulfilled' ? resultado.value : null,
    };
  });
}

/** Junta lo que requiere atención en todos los proyectos cargados, ordenado por tipo y luego por proyecto. */
export function atencionGeneral(paneles: PanelDeProyecto[], hoy: string): ItemAtencion[] {
  const items: ItemAtencion[] = [];
  for (const { proyecto, datos } of paneles) {
    if (datos === null) {
      continue;
    }
    for (const item of atencionDelDia(datos.panel, datos.detalle, hoy)) {
      items.push({
        ...item,
        clave: `p${proyecto.idProyecto}-${item.clave}`,
        proyecto: proyecto.nombre,
        nombreAccesible: `${item.accion}: ${item.texto} · ${proyecto.nombre}`,
      });
    }
  }
  return items.sort((a, b) => ORDEN_ATENCION[a.tipo] - ORDEN_ATENCION[b.tipo]);
}

/** Resume cifras de todos los proyectos cargados y elige la entrega próxima con menos días. */
export function resumenGeneral(paneles: PanelDeProyecto[], hoy: string): ResumenGeneral {
  let enRojo = 0;
  let equipos = 0;
  let totalPorEvaluar = 0;
  let mejorProxima: { dias: number; etiqueta: string; plazo: string } | null = null;

  for (const { proyecto, datos } of paneles) {
    if (datos === null) {
      continue;
    }
    enRojo += datos.panel.equipos.filter((e) => e.enRojo).length;
    equipos += datos.panel.equipos.length;
    totalPorEvaluar += porEvaluar(datos.detalle).length;

    const entregaProxima = entregasDelProyecto(datos.detalle, hoy).find((e) => e.proxima);
    if (entregaProxima !== undefined && (mejorProxima === null || entregaProxima.dias < mejorProxima.dias)) {
      mejorProxima = {
        dias: entregaProxima.dias,
        etiqueta: entregaProxima.etiqueta,
        plazo: `${entregaProxima.plazo} · ${proyecto.nombre}`,
      };
    }
  }

  return {
    enRojo,
    equipos,
    porEvaluar: totalPorEvaluar,
    proxima: mejorProxima === null ? null : { etiqueta: mejorProxima.etiqueta, plazo: mejorProxima.plazo },
  };
}

/** Lista de proyectos con su insignia de estado para la tarjeta «Tus proyectos». */
export function proyectosDelPanel(paneles: PanelDeProyecto[], hoy: string): ProyectoDelPanel[] {
  return paneles.map(({ proyecto, datos }) => ({
    idProyecto: proyecto.idProyecto,
    nombre: proyecto.nombre,
    marca: datos === null ? null : estadoProyecto(resumenDocente(datos, hoy)),
  }));
}

/** Instante más reciente de panel.generadoEn entre los cargados, o null si ninguno cargó. */
export function ultimaActualizacion(paneles: PanelDeProyecto[]): string | null {
  let mejor: { ms: number; valor: string } | null = null;
  for (const { datos } of paneles) {
    if (datos === null) {
      continue;
    }
    const { generadoEn } = datos.panel;
    const ms = new Date(generadoEn.replace(/(\.\d{3})\d+/, '$1')).getTime();
    if (Number.isNaN(ms)) {
      continue;
    }
    if (mejor === null || ms > mejor.ms) {
      mejor = { ms, valor: generadoEn };
    }
  }
  return mejor?.valor ?? null;
}
