import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { mensajeDeError } from '../api/cliente';
import { habilitarDocente, listarDocentesHabilitados } from '../api/cuentas';
import type { CorreoHabilitadoVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { Insignia } from '../componentes/Insignia';
import { Tabla } from '../componentes/Tabla';
import type { ColumnaTabla } from '../componentes/Tabla';
import { Tarjeta } from '../componentes/Tarjeta';
import { mensajeErrorHabilitar, validarCorreoDocente } from '../cuentas/validacion';
import { fechaCorta } from '../formato';

type EstadoLista =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; docentes: CorreoHabilitadoVista[] };

const COLUMNAS: ColumnaTabla<CorreoHabilitadoVista>[] = [
  { titulo: 'Correo', celda: (d) => d.email },
  {
    titulo: 'Estado',
    celda: (d) =>
      d.registrado ? <Insignia tono="exito">Cuenta creada</Insignia> : <Insignia tono="aviso">Pendiente de registro</Insignia>,
  },
  { titulo: 'Habilitado el', celda: (d) => fechaCorta(d.fechaHabilitacion) },
];

/**
 * Administración de docentes (R-17). Sin pantalla en Figma: diseño provisional (decisión 26a-7).
 * El administrador habilita el correo; el docente crea su cuenta en /registro.
 */
export function AdminDocentes() {
  const [lista, setLista] = useState<EstadoLista>({ estado: 'cargando' });
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const cargar = useCallback(async () => {
    try {
      setLista({ estado: 'listo', docentes: await listarDocentesHabilitados() });
    } catch (e) {
      setLista({ estado: 'error', mensaje: mensajeDeError(e) });
    }
  }, []);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  function reintentar() {
    setLista({ estado: 'cargando' });
    void cargar();
  }

  async function alHabilitar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setError(null);
    setExito(null);
    const problema = validarCorreoDocente(email);
    if (problema !== null) {
      setError(problema);
      return;
    }
    setEnviando(true);
    try {
      const habilitado = await habilitarDocente(email.trim());
      setExito(`Correo habilitado: ${habilitado.email}. El docente ya puede crear su cuenta en «Crear cuenta».`);
      setEmail('');
      await cargar();
    } catch (e) {
      setError(mensajeErrorHabilitar(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo="Docentes"
        descripcion="Habilita el correo institucional de cada docente. Con ese correo, el docente crea su cuenta desde «Crear cuenta»."
      />
      <Tarjeta titulo="Habilitar docente">
        <div className="pila">
          <form className="formulario-en-linea" onSubmit={(e) => void alHabilitar(e)} noValidate>
            <Campo
              etiqueta="Correo institucional"
              type="email"
              placeholder="nombre@uninorte.edu.co"
              autoComplete="off"
              maxLength={254}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <Boton type="submit" cargando={enviando}>
              Habilitar
            </Boton>
          </form>
          {error !== null && <Alerta tipo="error">{error}</Alerta>}
          {exito !== null && <Alerta tipo="exito">{exito}</Alerta>}
        </div>
      </Tarjeta>
      <section className="pagina__seccion" aria-labelledby="titulo-docentes-habilitados">
        <h2 id="titulo-docentes-habilitados" className="pagina__subtitulo">
          Docentes habilitados
        </h2>
        {lista.estado === 'cargando' && <Cargando />}
        {lista.estado === 'error' && (
          <div className="pila">
            <Alerta tipo="error">{lista.mensaje}</Alerta>
            <div>
              <Boton variante="secundario" onClick={reintentar}>
                Reintentar
              </Boton>
            </div>
          </div>
        )}
        {lista.estado === 'listo' && (
          <Tabla
            titulo="Correos de docentes habilitados"
            columnas={COLUMNAS}
            filas={lista.docentes}
            claveFila={(d) => d.idCorreoHabilitado}
            vacio="Aún no hay docentes habilitados."
          />
        )}
      </section>
    </div>
  );
}
