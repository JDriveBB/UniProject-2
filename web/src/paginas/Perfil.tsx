import { useEffect, useId, useState } from 'react';
import { mensajeDeError } from '../api/cliente';
import { datosPersonales } from '../api/perfil';
import type { DatosPersonales } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Cargando } from '../componentes/Cargando';
import { EncabezadoPagina } from '../componentes/EncabezadoPagina';
import { TarjetaContrasena } from '../perfil/TarjetaContrasena';
import { TarjetaDatos } from '../perfil/TarjetaDatos';
import { TarjetaEliminarCuenta } from '../perfil/TarjetaEliminarCuenta';
import { TarjetaTelegram } from '../perfil/TarjetaTelegram';
import { ID_TELEGRAM } from '../perfil/vista';
import type { Mensaje } from '../proyectos/errores';
import { useUsuario } from '../sesion/ContextoSesion';

type EstadoCarga =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'listo'; datos: DatosPersonales };

export function Perfil() {
  const usuario = useUsuario();
  const [carga, setCarga] = useState<EstadoCarga>({ estado: 'cargando' });
  const [vuelta, setVuelta] = useState(0);
  const [mensaje, setMensaje] = useState<Mensaje | null>(null);
  const [foco, setFoco] = useState<string | null>(null);

  const idAviso = useId();

  useEffect(() => {
    let vigente = true;
    datosPersonales()
      .then((datos) => {
        if (vigente) {
          setCarga({ estado: 'listo', datos });
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
  }, [vuelta]);

  useEffect(() => {
    if (foco !== null) {
      (document.getElementById(foco) ?? document.getElementById(idAviso))?.focus();
      setFoco(null);
    }
  }, [foco, idAviso]);

  function avisar(m: Mensaje) {
    setMensaje(m);
    setFoco(idAviso);
  }

  if (carga.estado === 'cargando') {
    return <Cargando />;
  }

  if (carga.estado === 'error') {
    return (
      <div className="pagina">
        <EncabezadoPagina titulo="Mi perfil" />
        <Alerta tipo="error">{carga.mensaje}</Alerta>
        <div>
          <Boton variante="secundario" onClick={() => setVuelta((v) => v + 1)}>
            Reintentar
          </Boton>
        </div>
      </div>
    );
  }

  const datos = carga.datos;

  return (
    <div className="pagina">
      <EncabezadoPagina titulo="Mi perfil" descripcion="Tus datos, tu vinculación con Telegram y tu cuenta." />
      {mensaje !== null && (
        <Alerta id={idAviso} tabIndex={-1} tipo={mensaje.tipo}>
          {mensaje.texto}
        </Alerta>
      )}
      <div className="perfil">
        <div className="perfil__columna">
          <TarjetaDatos
            datos={datos}
            telegramVinculado={usuario.telegramVinculado}
            alCambiarNombre={(nombre) =>
              setCarga((prev) =>
                prev.estado === 'listo'
                  ? { estado: 'listo', datos: { ...prev.datos, nombre } }
                  : prev,
              )
            }
            avisar={avisar}
          />
          <TarjetaContrasena
            telegramVinculado={usuario.telegramVinculado}
            avisar={avisar}
          />
        </div>
        <div id={ID_TELEGRAM} className="perfil__columna">
          <TarjetaTelegram
            vinculado={usuario.telegramVinculado}
            avisar={avisar}
          />
          <TarjetaEliminarCuenta
            rol={usuario.rol}
            avisar={avisar}
          />
        </div>
      </div>
    </div>
  );
}
