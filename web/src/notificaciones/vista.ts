import type { Bandeja } from '../api/tipos';
import { destinoSeguro } from '../sesion/destino';
import type { Marca } from '../tablero/marcas';

export const SIN_NOTIFICACIONES =
  'Aún no tienes notificaciones. Aquí verás los mismos avisos que te llegan por Telegram.';
export const SOLO_BANDEJA = 'Solo en la bandeja: cuando se generó no tenías Telegram vinculado.';
export const LIMITE_BANDEJA = 50;
export const TEXTO_LIMITE = 'Se muestran las 50 notificaciones más recientes.';
export const TODAS_LEIDAS = 'Marcaste todas las notificaciones como leídas.';
export const NO_DISPONIBLE = 'Esta notificación ya no está disponible.';
export const NUEVA: Marca = { texto: 'Nueva', tono: 'info' };

const MARCAS_POR_TIPO: Record<string, Marca> = {
  'hito.enviado': { texto: 'Hito enviado', tono: 'critico' },
  'hito.evaluado': { texto: 'Hito evaluado', tono: 'exito' },
  'tarea.ruta_critica.riesgo': { texto: 'Riesgo en ruta crítica', tono: 'peligro' },
  'equipo.inactividad': { texto: 'Inactividad', tono: 'aviso' },
  'tarea.reasignada': { texto: 'Tarea reasignada', tono: 'info' },
  'tarea.correccion': { texto: 'Corrección solicitada', tono: 'aviso' },
};

const MARCA_POR_DEFECTO: Marca = { texto: 'Aviso', tono: 'neutro' };

/** Marca (texto y tono) según el tipo de notificación. */
export function marcaTipo(tipo: string): Marca {
  if (Object.hasOwn(MARCAS_POR_TIPO, tipo)) {
    return MARCAS_POR_TIPO[tipo];
  }
  return MARCA_POR_DEFECTO;
}

/** Ruta SPA interna válida o null si es insegura o externa. */
export function enlaceInterno(enlace: string): string | null {
  return destinoSeguro({ desde: enlace }) === enlace ? enlace : null;
}

/** Texto descriptivo para el número de notificaciones sin leer. */
export function textoNoLeidas(n: number): string {
  if (n === 0) {
    return 'No tienes notificaciones sin leer.';
  }
  if (n === 1) {
    return 'Tienes 1 notificación sin leer.';
  }
  return `Tienes ${n} notificaciones sin leer.`;
}

/** Objeto nuevo de bandeja con la notificación marcada como leída. */
export function marcarEnBandeja(b: Bandeja, idNotificacion: number): Bandeja {
  let estabaSinLeer = false;
  const notificaciones = b.notificaciones.map((n) => {
    if (n.idNotificacion === idNotificacion) {
      if (!n.leida) {
        estabaSinLeer = true;
      }
      return { ...n, leida: true };
    }
    return { ...n };
  });
  const noLeidas = estabaSinLeer ? Math.max(0, b.noLeidas - 1) : b.noLeidas;
  return {
    noLeidas,
    notificaciones,
  };
}

/** Objeto nuevo de bandeja con todas las notificaciones leídas y contador a 0. */
export function marcarTodasEnBandeja(b: Bandeja): Bandeja {
  return {
    noLeidas: 0,
    notificaciones: b.notificaciones.map((n) => ({ ...n, leida: true })),
  };
}

/** Decide si al abrir una notificación debe marcarse como leída en el servidor. */
export function debeMarcarAlAbrir(notificacion: { leida: boolean }): boolean {
  return !notificacion.leida;
}
