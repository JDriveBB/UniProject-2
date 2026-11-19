export const DURACION_AVISO_MS = 4000;
export const MAXIMO_AVISOS_VISIBLES = 3;

export interface Aviso {
  readonly id: number;
  readonly texto: string;
}

export interface ColaAvisos {
  agregar(texto: string): number;
  cerrar(id: number): void;
  lista(): readonly Aviso[];
}

/**
 * Cola de avisos breves: máximo 3 visibles (si entra un cuarto, sale el más viejo)
 * y textos repetidos seguidos se juntan sin apilar dos iguales.
 */
export function crearColaAvisos(maximo: number = MAXIMO_AVISOS_VISIBLES): ColaAvisos {
  let siguienteId = 1;
  let avisos: Aviso[] = [];

  function agregar(texto: string): number {
    const ultimo = avisos.at(-1);
    if (ultimo !== undefined && ultimo.texto === texto) {
      return ultimo.id;
    }
    const id = siguienteId;
    siguienteId += 1;
    const siguientes = [...avisos, { id, texto }];
    avisos = siguientes.length > maximo ? siguientes.slice(siguientes.length - maximo) : siguientes;
    return id;
  }

  function cerrar(id: number): void {
    avisos = avisos.filter((a) => a.id !== id);
  }

  function lista(): readonly Aviso[] {
    return [...avisos];
  }

  return { agregar, cerrar, lista };
}
