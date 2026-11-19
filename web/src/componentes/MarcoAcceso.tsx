import { useEffect, type ReactNode } from 'react';
import { tituloDocumento } from './tituloDocumento';

/** Textos del panel de marca (Figma P01a/P01b; Aclaración 1 de la spec 26a, F2). */
const MARCA = {
  nombre: 'UniProject',
  lema: 'Gestión de proyectos colaborativos universitarios',
  descripcion: 'Organiza hitos, reparte tareas, controla dependencias y sigue el avance real de cada integrante.',
} as const;

interface PropsMarcoAcceso {
  titulo: string;
  subtitulo?: string;
  /** Registro separa sus elementos 16 px; ingreso, 20 px (Figma). */
  variante: 'ingreso' | 'registro';
  children: ReactNode;
}

/** Diseño común de ingreso (P01a) y crear cuenta (P01b): panel de marca de 620 px y tarjeta de 400 px. */
export function MarcoAcceso({ titulo, subtitulo, variante, children }: PropsMarcoAcceso) {
  useEffect(() => {
    document.title = tituloDocumento(titulo);
  }, [titulo]);

  return (
    <div className="acceso">
      <div className="acceso__marca">
        <p className="acceso__nombre">{MARCA.nombre}</p>
        <p className="acceso__lema">{MARCA.lema}</p>
        <p className="acceso__descripcion">{MARCA.descripcion}</p>
      </div>
      <main className="acceso__contenido">
        <section className={`tarjeta-acceso tarjeta-acceso--${variante}`}>
          <header className="tarjeta-acceso__cabecera">
            <h1 className="tarjeta-acceso__titulo">{titulo}</h1>
            {subtitulo !== undefined && <p className="tarjeta-acceso__subtitulo">{subtitulo}</p>}
          </header>
          {children}
        </section>
      </main>
    </div>
  );
}
