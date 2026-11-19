import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { mensajeDeError } from '../api/cliente';
import { misTareas } from '../api/tareas';
import type { MiTareaVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { BarraAvance } from '../componentes/BarraAvance';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Insignia } from '../componentes/Insignia';
import { Tabla, type ColumnaTabla } from '../componentes/Tabla';
import { fechaDia } from '../formato';
import { rutas } from '../rutas';
import { estadoTarea, marcaPrincipal } from '../tablero/marcas';

type EstadoCarga =
  { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo'; tareas: MiTareaVista[] };

export function MisTareas() {
  const [carga, setCarga] = useState<EstadoCarga>({ estado: 'cargando' });

  useEffect(() => {
    let vigente = true;
    misTareas()
      .then((tareas) => {
        if (vigente) {
          setCarga({ estado: 'listo', tareas });
        }
      })
      .catch((e) => {
        if (vigente) {
          setCarga({ estado: 'error', mensaje: mensajeDeError(e) });
        }
      });
    return () => {
      vigente = false;
    };
  }, []);

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }

  if (carga.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Mis tareas" descripcion="Tareas a tu cargo en todos tus proyectos." />
        <Alerta tipo="error">{carga.mensaje}</Alerta>
      </div>
    );
  }

  const columnas: ColumnaTabla<MiTareaVista>[] = [
    {
      titulo: 'Tarea',
      celda: (t) => (
        <Link className="enlace-tarea" to={rutas.tarea(t.idTarea)}>
          {t.titulo}
        </Link>
      ),
    },
    {
      titulo: 'Hito',
      celda: (t) => `${t.nombreHito} · ${t.proyecto}`,
    },
    {
      titulo: 'Estado',
      celda: (t) => {
        const marca = estadoTarea(t.estado);
        return <Insignia tono={marca.tono}>{marca.texto}</Insignia>;
      },
    },
    {
      titulo: 'Avance',
      celda: (t) => <BarraAvance porcentaje={t.porcentajeAvance} />,
    },
    {
      titulo: 'Fecha límite',
      celda: (t) => fechaDia(t.fechaLimite),
    },
    {
      titulo: 'Marcas',
      celda: (t) => {
        const marca = marcaPrincipal(t);
        return marca === null ? null : <Insignia tono={marca.tono}>{marca.texto}</Insignia>;
      },
    },
  ];

  return (
    <div className="pagina">
      <EncabezadoPagina titulo="Mis tareas" descripcion="Tareas a tu cargo en todos tus proyectos." />
      <Tabla
        titulo="Mis tareas"
        clase="tabla--mis-tareas"
        vacio="No tienes tareas asignadas."
        claveFila={(t) => t.idTarea}
        columnas={columnas}
        filas={carga.tareas}
      />
    </div>
  );
}
