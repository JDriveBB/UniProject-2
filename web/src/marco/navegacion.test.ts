import { describe, expect, it } from 'vitest';
import { enlacesDe, etiquetaRol, idProyectoEnRuta, textoContador } from './navegacion';

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
});
