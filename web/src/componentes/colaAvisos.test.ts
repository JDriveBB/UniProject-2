import { describe, expect, it } from 'vitest';
import { crearColaAvisos, DURACION_AVISO_MS, MAXIMO_AVISOS_VISIBLES } from './colaAvisos';

describe('cola de avisos breves', () => {
  it('define una duración de 4 segundos y un máximo de 3 avisos visibles', () => {
    expect(DURACION_AVISO_MS).toBe(4000);
    expect(MAXIMO_AVISOS_VISIBLES).toBe(3);
  });

  it('agrega avisos en orden, asigna ids distintos y permite cerrarlos', () => {
    const cola = crearColaAvisos();
    expect(cola.lista()).toEqual([]);

    const id1 = cola.agregar('Avance registrado: 50 %.');
    const id2 = cola.agregar('Comentario publicado.');

    expect(id1).not.toBe(id2);
    expect(cola.lista()).toEqual([
      { id: id1, texto: 'Avance registrado: 50 %.' },
      { id: id2, texto: 'Comentario publicado.' },
    ]);

    cola.cerrar(id1);
    expect(cola.lista()).toEqual([{ id: id2, texto: 'Comentario publicado.' }]);

    cola.cerrar(999);
    expect(cola.lista()).toEqual([{ id: id2, texto: 'Comentario publicado.' }]);
  });

  it('conserva como máximo 3 avisos visibles sacando al más viejo', () => {
    const cola = crearColaAvisos();
    const id1 = cola.agregar('Primero');
    const id2 = cola.agregar('Segundo');
    const id3 = cola.agregar('Tercero');
    const id4 = cola.agregar('Cuarto');

    expect(cola.lista()).toEqual([
      { id: id2, texto: 'Segundo' },
      { id: id3, texto: 'Tercero' },
      { id: id4, texto: 'Cuarto' },
    ]);
    expect(cola.lista().some((a) => a.id === id1)).toBe(false);
  });

  it('junta textos repetidos seguidos sin apilar dos iguales', () => {
    const cola = crearColaAvisos();
    const id1 = cola.agregar('Cambios guardados.');
    const id1Repetido = cola.agregar('Cambios guardados.');

    expect(id1Repetido).toBe(id1);
    expect(cola.lista()).toEqual([{ id: id1, texto: 'Cambios guardados.' }]);

    const id2 = cola.agregar('Equipo creado.');
    const id3 = cola.agregar('Cambios guardados.');

    expect(id3).not.toBe(id1);
    expect(cola.lista()).toEqual([
      { id: id1, texto: 'Cambios guardados.' },
      { id: id2, texto: 'Equipo creado.' },
      { id: id3, texto: 'Cambios guardados.' },
    ]);

    cola.cerrar(id3);
    const id4 = cola.agregar('Equipo creado.');
    expect(id4).toBe(id2);
    expect(cola.lista()).toEqual([
      { id: id1, texto: 'Cambios guardados.' },
      { id: id2, texto: 'Equipo creado.' },
    ]);
  });

  it('devuelve una copia de la lista para no exponer el estado interno', () => {
    const cola = crearColaAvisos();
    cola.agregar('Aviso 1');
    const copia = cola.lista() as { id: number; texto: string }[];
    copia.pop();
    expect(cola.lista()).toHaveLength(1);
  });
});
