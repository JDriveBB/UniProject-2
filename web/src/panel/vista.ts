import type {
  EstadoHitoEquipo,
  HitoEquipoVista,
  IntegranteInactivo,
  PanelEquipo,
  PanelProyecto,
  ProyectoDetalle,
  ProyectoVista,
  TareaBloqueada,
} from '../api/tipos';
import type { TonoInsignia } from '../componentes/Insignia';
import { fechaCorta, fechaHora } from '../formato';
import { rutas } from '../rutas';
import { estadoHito, type Marca } from '../tablero/marcas';
import { etiquetaHito, hitosDelEquipo } from '../tablero/ubicacion';

export const SIN_ACCESO_PANEL = 'No tienes acceso al panel de este proyecto o no existe.';
export const SIN_ATENCION = 'Todo en orden: ningún equipo necesita que intervengas hoy.';
export const SIN_ALERTAS = 'Sin alertas: nadie está inactivo y no hay tareas bloqueadas por más de 48 horas.';
export const EXPLICACION_ALERTAS =
  'Integrantes sin actividad según la alerta de inactividad de cada hito y tareas bloqueadas por dependencia hace más de 48 horas. Los hitos sin alerta de inactividad no se revisan.';
export const EXPLICACION_ESTADO =
  '«Requiere atención»: hay integrantes inactivos o tareas bloqueadas por más de 48 horas. «Tareas en riesgo»: tareas sin completar con riesgo alto de retraso.';
export const EXPLICACION_VENCIDO = '«Vencido sin enviar»: la fecha límite pasó y el equipo no ha enviado el hito.';
export const EXPLICACION_AVANCE_HITO =
  '«Avance del hito»: avance de las tareas del hito en curso de cada equipo, ponderado por sus horas estimadas («Todo enviado» si ya envió todos sus hitos).';

export const REQUIERE_ATENCION: Marca = { texto: 'Requiere atención', tono: 'peligro' };
export const TAREAS_EN_RIESGO: Marca = { texto: 'Tareas en riesgo', tono: 'aviso' };
export const AL_DIA: Marca = { texto: 'Al día', tono: 'exito' };
export const VENCIDO_SIN_ENVIAR: Marca = { texto: 'Vencido sin enviar', tono: 'peligro' };
export const INACTIVO: Marca = { texto: 'Inactivo', tono: 'aviso' };
export const BLOQUEADA: Marca = { texto: 'Bloqueada', tono: 'peligro' };

const MS_POR_DIA = 86_400_000;

function fechaUtc(fecha: string): number {
  const [anio, mes, dia] = fecha.split('-').map(Number);
  return Date.UTC(anio, mes - 1, dia);
}

function cantidadTexto(cantidad: number, singular: string, plural: string): string {
  return cantidad === 1 ? `1 ${singular}` : `${cantidad} ${plural}`;
}

export function diasHasta(fecha: string, hoy: string): number {
  const dias = Math.round((fechaUtc(fecha) - fechaUtc(hoy)) / MS_POR_DIA);
  return dias === 0 ? 0 : dias;
}

export function textoPlazo(dias: number): string {
  if (dias === 0) {
    return 'vence hoy';
  }
  if (dias === 1) {
    return 'vence mañana';
  }
  if (dias > 1) {
    return `vence en ${dias} días`;
  }
  if (dias === -1) {
    return 'venció ayer';
  }
  return `venció hace ${Math.abs(dias)} días`;
}

/** «20/11» en hora de Colombia; null si no es una fecha. */
function diaMesEnvio(fechaEnvio: string): { iso: string; texto: string } | null {
  const corta = fechaCorta(fechaEnvio);
  const partes = corta.split('/');
  if (partes.length !== 3) {
    return null;
  }
  const [dia, mes, anio] = partes;
  return { iso: `${anio}-${mes}-${dia}`, texto: `${dia}/${mes}` };
}

/** D-12: «Enviado el 20/11 · 2 días antes» / «· el mismo día» / «· 1 día tarde», contra la fecha límite. */
export function textoEnvio(fechaEnvio: string, fechaLimite: string): string | null {
  const envio = diaMesEnvio(fechaEnvio);
  if (envio === null) {
    return null;
  }
  const dias = diasHasta(fechaLimite, envio.iso);
  let relativo: string;
  if (dias === 0) {
    relativo = 'el mismo día';
  } else if (dias > 0) {
    relativo = `${cantidadTexto(dias, 'día', 'días')} antes`;
  } else {
    relativo = `${cantidadTexto(-dias, 'día', 'días')} tarde`;
  }
  return `Enviado el ${envio.texto} · ${relativo}`;
}

function fechasDeEnvio(detalle: ProyectoDetalle): Map<number, string> {
  const fechas = new Map<number, string>();
  for (const equipo of detalle.equipos) {
    for (const he of equipo.hitos) {
      if (he.fechaEnvio != null) {
        fechas.set(he.idHitoEquipo, he.fechaEnvio);
      }
    }
  }
  return fechas;
}

export function proyectosPropios(lista: ProyectoVista[], idUsuario: number): ProyectoVista[] {
  return lista.filter((p) => p.idDocente === idUsuario);
}

export interface PorEvaluar {
  idHitoEquipo: number;
  idEquipo: number;
  nombreEquipo: string;
  orden: number;
  nombreHito: string;
  fechaLimite: string;
}

interface HitoConEquipo {
  idHitoEquipo: number;
  idEquipo: number;
  nombreEquipo: string;
  orden: number;
  nombreHito: string;
  fechaLimite: string;
  estado: EstadoHitoEquipo;
}

function hitosDeEquipos(detalle: ProyectoDetalle): HitoConEquipo[] {
  const lista: HitoConEquipo[] = [];
  for (const equipo of detalle.equipos) {
    const hitos = hitosDelEquipo({
      proyecto: detalle.proyecto,
      hitos: detalle.hitos,
      equipo,
    });
    for (const h of hitos) {
      lista.push({
        idHitoEquipo: h.idHitoEquipo,
        idEquipo: equipo.idEquipo,
        nombreEquipo: equipo.nombreEquipo,
        orden: h.orden,
        nombreHito: h.nombreHito,
        fechaLimite: h.fechaLimite,
        estado: h.estado,
      });
    }
  }
  return lista.sort(
    (a, b) =>
      a.orden - b.orden || a.nombreEquipo.localeCompare(b.nombreEquipo, 'es') || a.idHitoEquipo - b.idHitoEquipo,
  );
}

export function porEvaluar(detalle: ProyectoDetalle): PorEvaluar[] {
  return hitosDeEquipos(detalle)
    .filter((h) => h.estado === 'ENVIADO')
    .map(({ idHitoEquipo, idEquipo, nombreEquipo, orden, nombreHito, fechaLimite }) => ({
      idHitoEquipo,
      idEquipo,
      nombreEquipo,
      orden,
      nombreHito,
      fechaLimite,
    }));
}

export type TipoAtencion = 'vencido' | 'equipo' | 'calificar' | 'proximo' | 'riesgo';

export interface ItemAtencion {
  clave: string;
  tipo: TipoAtencion;
  tono: TonoInsignia;
  etiqueta: string;
  /** Solo en el panel general (T-030). */
  proyecto?: string;
  texto: string;
  accion: string;
  ruta: string;
  nombreAccesible: string;
}

function crearItemAtencion(base: Omit<ItemAtencion, 'nombreAccesible'>): ItemAtencion {
  return {
    ...base,
    nombreAccesible: `${base.accion}: ${base.texto}`,
  };
}

function textoEquipoEnRojo(e: PanelEquipo): string {
  const partes: string[] = [];
  if (e.integrantesInactivos.length > 0) {
    partes.push(cantidadTexto(e.integrantesInactivos.length, 'integrante inactivo', 'integrantes inactivos'));
  }
  if (e.tareasBloqueadas.length > 0) {
    partes.push(cantidadTexto(e.tareasBloqueadas.length, 'tarea bloqueada', 'tareas bloqueadas'));
  }
  return partes.length > 0 ? `${e.nombreEquipo}: ${partes.join(' · ')}` : `${e.nombreEquipo}: requiere atención`;
}

export function atencionDelDia(panel: PanelProyecto, detalle: ProyectoDetalle, hoy: string): ItemAtencion[] {
  const items: ItemAtencion[] = [];
  const hitos = hitosDeEquipos(detalle);
  const equiposOrdenados = [...panel.equipos].sort(
    (a, b) => a.nombreEquipo.localeCompare(b.nombreEquipo, 'es') || a.idEquipo - b.idEquipo,
  );

  for (const h of hitos) {
    if (h.estado === 'EN_CURSO' && hoy > h.fechaLimite) {
      const plazo = textoPlazo(diasHasta(h.fechaLimite, hoy));
      items.push(
        crearItemAtencion({
          clave: `vencido-${h.idEquipo}-${h.idHitoEquipo}`,
          tipo: 'vencido',
          tono: 'peligro',
          etiqueta: 'Vencido',
          texto: `${h.nombreEquipo} no envió ${etiquetaHito(h.orden, h.nombreHito)}: ${plazo}`,
          accion: 'Ver tablero',
          ruta: rutas.tableroEnHito(h.idEquipo, h.idHitoEquipo),
        }),
      );
    }
  }

  for (const e of equiposOrdenados) {
    if (e.enRojo) {
      items.push(
        crearItemAtencion({
          clave: `equipo-${e.idEquipo}`,
          tipo: 'equipo',
          tono: 'peligro',
          etiqueta: 'Requiere atención',
          texto: textoEquipoEnRojo(e),
          accion: 'Ver tablero',
          ruta: rutas.tableroEquipo(e.idEquipo),
        }),
      );
    }
  }

  const envios = fechasDeEnvio(detalle);
  for (const p of porEvaluar(detalle)) {
    const fechaEnvio = envios.get(p.idHitoEquipo);
    const envio = fechaEnvio === undefined ? null : diaMesEnvio(fechaEnvio);
    items.push(
      crearItemAtencion({
        clave: `calificar-${p.idHitoEquipo}`,
        tipo: 'calificar',
        tono: 'info',
        etiqueta: 'Por calificar',
        texto: `${p.nombreEquipo} envió ${etiquetaHito(p.orden, p.nombreHito)}${envio === null ? '' : ` el ${envio.texto}`}`,
        accion: 'Calificar',
        ruta: rutas.evaluarHito(p.idHitoEquipo),
      }),
    );
  }

  for (const h of hitos) {
    if (h.estado === 'EN_CURSO') {
      const dias = diasHasta(h.fechaLimite, hoy);
      if (dias >= 0 && dias <= 3) {
        items.push(
          crearItemAtencion({
            clave: `proximo-${h.idEquipo}-${h.idHitoEquipo}`,
            tipo: 'proximo',
            tono: 'aviso',
            etiqueta: 'Entrega próxima',
            texto: `${h.nombreEquipo} aún no envía ${etiquetaHito(h.orden, h.nombreHito)}: ${textoPlazo(dias)}`,
            accion: 'Ver tablero',
            ruta: rutas.tableroEnHito(h.idEquipo, h.idHitoEquipo),
          }),
        );
      }
    }
  }

  for (const e of equiposOrdenados) {
    if (!e.enRojo && e.tareasRiesgoAlto > 0) {
      const riesgo = cantidadTexto(
        e.tareasRiesgoAlto,
        'tarea con riesgo alto de retraso',
        'tareas con riesgo alto de retraso',
      );
      items.push(
        crearItemAtencion({
          clave: `riesgo-${e.idEquipo}`,
          tipo: 'riesgo',
          tono: 'aviso',
          etiqueta: 'Riesgo alto',
          texto: `${e.nombreEquipo}: ${riesgo}`,
          accion: 'Ver tablero',
          ruta: rutas.tableroEquipo(e.idEquipo),
        }),
      );
    }
  }

  return items;
}

export function estadoEquipo(e: Pick<PanelEquipo, 'enRojo' | 'tareasRiesgoAlto'>): Marca {
  if (e.enRojo) {
    return REQUIERE_ATENCION;
  }
  if (e.tareasRiesgoAlto > 0) {
    return TAREAS_EN_RIESGO;
  }
  return AL_DIA;
}

export interface EntregaEquipo {
  idEquipo: number;
  nombreEquipo: string;
  idHitoEquipo: number;
  marca: Marca;
  /** D-12: «Enviado el 20/11 · 2 días antes»; null si el hito no se ha enviado. */
  envio: string | null;
}

export type UrgenciaEntrega = 'pasada' | 'vencida' | 'pronto' | 'normal';

export interface Entrega {
  idHito: number;
  etiqueta: string;
  fechaLimite: string;
  dias: number;
  plazo: string;
  urgencia: UrgenciaEntrega;
  proxima: boolean;
  equipos: EntregaEquipo[];
}

function urgenciaEntrega(dias: number, algunoEnCurso: boolean): UrgenciaEntrega {
  if (dias < 0) {
    return algunoEnCurso ? 'vencida' : 'pasada';
  }
  if (dias <= 3) {
    return 'pronto';
  }
  return 'normal';
}

function envioDe(he: HitoEquipoVista, fechaLimite: string): string | null {
  return he.fechaEnvio == null ? null : textoEnvio(he.fechaEnvio, fechaLimite);
}

export function entregasDelProyecto(detalle: ProyectoDetalle, hoy: string): Entrega[] {
  const hitosOrdenados = [...detalle.hitos].sort((a, b) => a.orden - b.orden || a.idHito - b.idHito);
  const equiposOrdenados = [...detalle.equipos].sort(
    (a, b) => a.nombreEquipo.localeCompare(b.nombreEquipo, 'es') || a.idEquipo - b.idEquipo,
  );

  let proximaAsignada = false;
  return hitosOrdenados.map((h) => {
    const dias = diasHasta(h.fechaLimite, hoy);
    const proxima = !proximaAsignada && dias >= 0;
    if (proxima) {
      proximaAsignada = true;
    }

    let algunoEnCurso = false;
    const equipos: EntregaEquipo[] = [];
    for (const equipo of equiposOrdenados) {
      const he = equipo.hitos.find((item) => item.idHito === h.idHito);
      if (he !== undefined) {
        if (he.estado === 'EN_CURSO') {
          algunoEnCurso = true;
        }
        equipos.push({
          idEquipo: equipo.idEquipo,
          nombreEquipo: equipo.nombreEquipo,
          idHitoEquipo: he.idHitoEquipo,
          marca: marcaCelda(he.estado, h.fechaLimite, hoy),
          envio: envioDe(he, h.fechaLimite),
        });
      }
    }

    return {
      idHito: h.idHito,
      etiqueta: etiquetaHito(h.orden, h.nombreHito),
      fechaLimite: h.fechaLimite,
      dias,
      plazo: textoPlazo(dias),
      urgencia: urgenciaEntrega(dias, algunoEnCurso),
      proxima,
      equipos,
    };
  });
}

export interface ResumenDelPanel {
  enRojo: number;
  equipos: number;
  porEvaluar: number;
  avance: number | null;
  proxima: { etiqueta: string; plazo: string } | null;
}

/** D-12: avance del hito en curso (ponderado por horas), o null si ningún equipo tiene tareas en curso. */
export function resumenPanel(panel: PanelProyecto, porEvaluar: number, entregas: Entrega[]): ResumenDelPanel {
  const enRojo = panel.equipos.filter((e) => e.enRojo).length;
  const equipos = panel.equipos.length;
  const avances = panel.equipos.map((e) => e.avanceHitoActual).filter((a): a is number => a !== null);
  const avance = avances.length === 0 ? null : Math.round(avances.reduce((suma, a) => suma + a, 0) / avances.length);
  const entregaProxima = entregas.find((e) => e.proxima);
  const proxima =
    entregaProxima === undefined ? null : { etiqueta: entregaProxima.etiqueta, plazo: entregaProxima.plazo };
  return { enRojo, equipos, porEvaluar, avance, proxima };
}

export function marcaCelda(estado: EstadoHitoEquipo, fechaLimite: string, hoy: string): Marca {
  if (estado === 'EN_CURSO' && hoy > fechaLimite) {
    return VENCIDO_SIN_ENVIAR;
  }
  return estadoHito(estado);
}

export function textoDias(n: number): string {
  return cantidadTexto(n, 'día', 'días');
}

export function textoInactivo(i: IntegranteInactivo): string {
  return `${i.nombre} · sin actividad hace ${textoDias(i.diasSinActividad)} (última: ${fechaCorta(i.ultimaActividad)})`;
}

export function textoBloqueo(horas: number): string {
  return horas >= 48 ? `bloqueada hace ${textoDias(Math.floor(horas / 24))}` : `bloqueada hace ${horas} h`;
}

export interface AlertasEquipo {
  idEquipo: number;
  nombreEquipo: string;
  inactivos: IntegranteInactivo[];
  bloqueadas: TareaBloqueada[];
}

export function alertasPorEquipo(panel: PanelProyecto): AlertasEquipo[] {
  const lista: AlertasEquipo[] = [];
  for (const e of panel.equipos) {
    if (e.integrantesInactivos.length > 0 || e.tareasBloqueadas.length > 0) {
      lista.push({
        idEquipo: e.idEquipo,
        nombreEquipo: e.nombreEquipo,
        inactivos: e.integrantesInactivos,
        bloqueadas: e.tareasBloqueadas,
      });
    }
  }
  return lista;
}

export function textoActualizado(generadoEn: string): string {
  return `Actualizado el ${fechaHora(generadoEn)}`;
}

export interface EstadoBotonPanel {
  texto: string;
  deshabilitado: boolean;
}

export function estadoBotonPanel(accion: 'actualizar' | 'reintentar', enCurso: boolean): EstadoBotonPanel {
  if (accion === 'actualizar') {
    return {
      texto: enCurso ? 'Actualizando…' : 'Actualizar',
      deshabilitado: enCurso,
    };
  }
  return {
    texto: enCurso ? 'Reintentando…' : 'Reintentar',
    deshabilitado: enCurso,
  };
}
