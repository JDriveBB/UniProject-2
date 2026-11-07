import { describe, expect, it } from 'vitest';
import { enlacesDe, esLiderDeAlgunEquipo, etiquetaRol, idProyectoEnRuta, textoContador } from './navegacion';

describe('barra de navegación', () => {
  it('docente dentro de un proyecto: Proyectos activo y Panel abre el panel de ese proyecto', () => {
    expect(enlacesDe('DOCENTE', '/proyectos/5')).toEqual([
      { texto: 'Panel', a: '/proyectos/5/panel', activo: false },
      { texto: 'Proyectos', a: '/proyectos', activo: true },
      { texto: 'Reportes', a: '/reportes', activo: false },
    ]);
  });

  it('docente: fuera de un proyecto Panel va a /panel; en el panel solo Panel está activo', () => {
    expect(enlacesDe('DOCENTE', '/proyectos')[0]).toEqual({ texto: 'Panel', a: '/panel', activo: false });
    expect(enlacesDe('DOCENTE', '/proyectos/nuevo')[0]).toEqual({ texto: 'Panel', a: '/panel', activo: false });
    expect(enlacesDe('DOCENTE', '/proyectos/nuevo')[1].activo).toBe(true);
    expect(enlacesDe('DOCENTE', '/proyectos/5/panel').map((e) => e.activo)).toEqual([true, false, false]);
    expect(enlacesDe('DOCENTE', '/equipos/3/historial').map((e) => e.activo)).toEqual([false, false, true]);
  });

  it('estudiante: Tablero, Mis tareas y Ruta crítica', () => {
    expect(enlacesDe('ESTUDIANTE', '/equipos/3/tablero')).toEqual([
      { texto: 'Tablero', a: '/tablero', activo: true },
      { texto: 'Mis tareas', a: '/mis-tareas', activo: false },
      { texto: 'Ruta crítica', a: '/ruta-critica', activo: false },
    ]);
    expect(enlacesDe('ESTUDIANTE', '/hitos/7/ruta-critica').map((e) => e.activo)).toEqual([false, false, true]);
    expect(enlacesDe('ESTUDIANTE', '/equipos/3/historial').map((e) => e.activo)).toEqual([true, false, false]);
  });

  it('monitor y administrador tienen su propia navegación', () => {
    expect(enlacesDe('MONITOR', '/proyectos/2')).toEqual([{ texto: 'Proyectos', a: '/proyectos', activo: true }]);
    expect(enlacesDe('ADMINISTRADOR', '/admin/docentes')).toEqual([
      { texto: 'Docentes', a: '/admin/docentes', activo: true },
    ]);
    expect(idProyectoEnRuta('/proyectos/12/panel')).toBe(12);
    expect(idProyectoEnRuta('/proyectos/nuevo')).toBeNull();
  });

  it('rol bajo el nombre y contador de no leídas', () => {
    expect(etiquetaRol('DOCENTE')).toBe('Docente');
    expect(etiquetaRol('ESTUDIANTE')).toBe('Estudiante');
    expect(textoContador(0)).toBeNull();
    expect(textoContador(3)).toBe('3');
    expect(textoContador(120)).toBe('99+');
  });

  it('navegación de tareas, rol de líder y pertenencia a equipos', () => {
    expect(enlacesDe('ESTUDIANTE', '/tareas/5')[1].activo).toBe(true);
    expect(enlacesDe('ESTUDIANTE', '/tareas/5/')[1].activo).toBe(true);
    expect(enlacesDe('ESTUDIANTE', '/tareas/abc')[1].activo).toBe(false);

    expect(etiquetaRol('ESTUDIANTE', true)).toBe('Líder de equipo');
    expect(etiquetaRol('ESTUDIANTE')).toBe('Estudiante');
    expect(etiquetaRol('DOCENTE', true)).toBe('Docente');

    const equipo1 = {
      miembros: [{ idUsuario: 10, nombre: 'Ana', correo: 'a@u.edu', rolEquipo: 'INTEGRANTE' as const }],
    };
    const equipo2 = {
      miembros: [{ idUsuario: 10, nombre: 'Ana', correo: 'a@u.edu', rolEquipo: 'LIDER' as const }],
    };

    expect(esLiderDeAlgunEquipo([equipo1, equipo2], 10)).toBe(true);
    expect(esLiderDeAlgunEquipo([equipo1], 10)).toBe(false);
    expect(esLiderDeAlgunEquipo([], 10)).toBe(false);
  });
});
