import { useEffect, useId, useState, type FormEvent } from 'react';
import { rectificarNombre } from '../api/perfil';
import type { DatosPersonales } from '../api/tipos';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Tarjeta } from '../componentes/Tarjeta';
import { fechaCorta } from '../formato';
import { etiquetaRol } from '../marco/navegacion';
import type { Mensaje } from '../proyectos/errores';
import { useSesion } from '../sesion/ContextoSesion';
import { validarNombre } from './formularios';
import { CAMPOS_NOMBRE, NOMBRE_ACTUALIZADO, mensajeErrorPerfil, textoAutorizacion } from './vista';

export interface PropsTarjetaDatos {
  datos: DatosPersonales;
  telegramVinculado: boolean;
  alCambiarNombre: (nombre: string) => void;
  avisar: (m: Mensaje) => void;
}

export function TarjetaDatos({ datos, telegramVinculado, alCambiarNombre, avisar }: PropsTarjetaDatos) {
  const { refrescar } = useSesion();
  const [abierto, setAbierto] = useState(false);
  const [nombre, setNombre] = useState(datos.nombre);
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const baseId = useId();
  const idBotonCambiar = baseId + '-cambiar';
  const idCampoNombre = baseId + '-nombre';

  useEffect(() => {
    if (abierto) {
      document.getElementById(idCampoNombre)?.focus();
    }
  }, [abierto, idCampoNombre]);

  function cancelar() {
    setAbierto(false);
    setIntentado(false);
    setNombre(datos.nombre);
    document.getElementById(idBotonCambiar)?.focus();
  }

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    const error = validarNombre(nombre);
    if (error !== undefined) {
      document.getElementById(idCampoNombre)?.focus();
      return;
    }
    setEnviando(true);
    try {
      const respuesta = await rectificarNombre(nombre.trim());
      try {
        await refrescar();
      } catch {
        // ignorado deliberadamente
      }
      alCambiarNombre(respuesta.nombre);
      setAbierto(false);
      avisar({ tipo: 'exito', texto: NOMBRE_ACTUALIZADO });
    } catch (e) {
      avisar({ tipo: 'error', texto: mensajeErrorPerfil(e, CAMPOS_NOMBRE) });
    } finally {
      setEnviando(false);
    }
  }

  const errorNombre = intentado ? validarNombre(nombre) : undefined;

  return (
    <Tarjeta titulo="Mis datos">
      <p className="texto-secundario">Estos son los datos personales que UniProject guarda de ti (Ley 1581 de 2012).</p>
      <dl className="datos-perfil">
        <dt>Nombre completo</dt>
        <dd>{datos.nombre}</dd>
        <dt>Correo institucional</dt>
        <dd>{datos.email}</dd>
        <dt>Rol</dt>
        <dd>{etiquetaRol(datos.rol)}</dd>
        <dt>Telegram</dt>
        <dd>{telegramVinculado ? 'Vinculado' : 'No vinculado'}</dd>
        <dt>Autorización de datos</dt>
        <dd>{textoAutorizacion(datos)}</dd>
        <dt>Cuenta creada</dt>
        <dd>{fechaCorta(datos.fechaRegistro)}</dd>
      </dl>
      <p className="texto-ayuda">El correo no se puede cambiar.</p>
      {!abierto ? (
        <div>
          <Boton id={idBotonCambiar} variante="secundario" onClick={() => setAbierto(true)}>
            Cambiar nombre
          </Boton>
        </div>
      ) : (
        <form className="formulario-en-linea" noValidate onSubmit={(e) => void guardar(e)}>
          <Campo
            id={idCampoNombre}
            etiqueta="Nombre completo"
            autoComplete="name"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            error={errorNombre}
            disabled={enviando}
          />
          <Boton type="submit" cargando={enviando} disabled={enviando}>
            Guardar
          </Boton>
          <Boton variante="secundario" onClick={cancelar} disabled={enviando}>
            Cancelar
          </Boton>
        </form>
      )}
    </Tarjeta>
  );
}
