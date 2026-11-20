import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { mensajeDeError } from '../api/cliente';
import { generarEnlaceDocente, habilitarDocente, listarDocentesHabilitados } from '../api/cuentas';
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
import { prepararEnlaceInvitacion } from '../cuentas/enlace';
import type { EnlaceCopiado } from '../cuentas/enlace';
import { mensajeErrorHabilitar, validarCorreoDocente } from '../cuentas/validacion';
import { fechaCorta } from '../formato';

type EstadoLista =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; docentes: CorreoHabilitadoVista[] };

/**
 * Administración de docentes (R-17). Sin pantalla en Figma: diseño provisional (decisión 26a-7).
 * El administrador habilita el correo y copia el enlace de invitación para que el docente cree su cuenta (T-041).
 */
export function AdminDocentes() {
  const [lista, setLista] = useState<EstadoLista>({ estado: 'cargando' });
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [generandoId, setGenerandoId] = useState<number | null>(null);
  const [errorEnlace, setErrorEnlace] = useState<string | null>(null);
  const [enlaceGenerado, setEnlaceGenerado] = useState<EnlaceCopiado | null>(null);

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
    setErrorEnlace(null);
    setEnlaceGenerado(null);
    setLista({ estado: 'cargando' });
    void cargar();
  }

  async function alHabilitar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setError(null);
    setExito(null);
    setErrorEnlace(null);
    const problema = validarCorreoDocente(email);
    if (problema !== null) {
      setError(problema);
      return;
    }
    setEnviando(true);
    try {
      const habilitado = await habilitarDocente(email.trim());
      setExito(
        `Correo habilitado: ${habilitado.email}. Copia su enlace de invitación en la tabla de abajo para que cree su cuenta.`,
      );
      setEmail('');
      await cargar();
    } catch (e) {
      setError(mensajeErrorHabilitar(e));
    } finally {
      setEnviando(false);
    }
  }

  async function alCopiarEnlace(d: CorreoHabilitadoVista) {
    setErrorEnlace(null);
    setEnlaceGenerado(null);
    setGenerandoId(d.idCorreoHabilitado);
    const r = await prepararEnlaceInvitacion(
      () => generarEnlaceDocente(d.idCorreoHabilitado),
      d.email,
      window.location.origin,
      mensajeDeError,
    );
    setGenerandoId(null);
    if (r.ok) {
      setEnlaceGenerado(r.enlace);
      return;
    }
    if (r.recargar) {
      await cargar();
    }
    setErrorEnlace(r.mensaje);
  }

  const columnas: ColumnaTabla<CorreoHabilitadoVista>[] = [
    { titulo: 'Correo', celda: (d) => d.email },
    {
      titulo: 'Estado',
      celda: (d) =>
        d.registrado ? (
          <Insignia tono="exito">Cuenta creada</Insignia>
        ) : (
          <Insignia tono="aviso">Pendiente de registro</Insignia>
        ),
    },
    { titulo: 'Habilitado el', celda: (d) => fechaCorta(d.fechaHabilitacion) },
    {
      titulo: 'Acción',
      celda: (d) => {
        if (d.registrado) {
          return null;
        }
        const generandoEste = generandoId === d.idCorreoHabilitado;
        return (
          <Boton
            variante="secundario"
            aria-label={`Copiar enlace de invitación para ${d.email}`}
            aria-busy={generandoEste}
            disabled={generandoId !== null}
            onClick={() => void alCopiarEnlace(d)}
          >
            {generandoEste ? 'Generando…' : 'Copiar enlace'}
          </Boton>
        );
      },
    },
  ];

  return (
    <div className="pagina">
      <EncabezadoPagina
        titulo="Docentes"
        descripcion="Habilita el correo institucional de cada docente y copia su enlace de invitación para que cree su cuenta."
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
          <>
            <Tabla
              titulo="Correos de docentes habilitados"
              columnas={columnas}
              filas={lista.docentes}
              claveFila={(d) => d.idCorreoHabilitado}
              vacio="Aún no hay docentes habilitados."
              clase="tabla--docentes"
            />
            {errorEnlace !== null && <Alerta tipo="error">{errorEnlace}</Alerta>}
            {enlaceGenerado !== null && (
              <div className="enlace-generado">
                <Alerta tipo="exito">{enlaceGenerado.texto}</Alerta>
                <Campo
                  etiqueta={`Enlace de invitación para ${enlaceGenerado.email}`}
                  readOnly
                  value={enlaceGenerado.url}
                  onFocus={(e) => e.currentTarget.select()}
                  onClick={(e) => e.currentTarget.select()}
                />
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
