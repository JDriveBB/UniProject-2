import { useId, useState, type FormEvent } from 'react';
import { cambiarNotaGrupal } from '../api/evaluacion';
import type { EvaluacionVista } from '../api/tipos';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { Tarjeta } from '../componentes/Tarjeta';
import type { Mensaje } from '../proyectos/errores';
import { textoNota } from './calculo';
import { numeroNota, validarNota05 } from './formulario';
import {
  CAMPOS_NOTA_GRUPAL,
  esConflicto,
  hayConfirmadas,
  mensajeErrorEvaluacion,
  NOTAS_YA_CONFIRMADAS,
} from './vista';

export interface PropsTarjetaNotaGrupal {
  evaluacion: EvaluacionVista;
  alActualizar: (ev: EvaluacionVista, m: Mensaje) => void;
  alHecho: (m: Mensaje, recargar: boolean) => void;
}

export function TarjetaNotaGrupal({
  evaluacion,
  alActualizar,
  alHecho,
}: PropsTarjetaNotaGrupal) {
  const bloqueada = hayConfirmadas(evaluacion.notas);
  const [texto, setTexto] = useState(() => textoNota(evaluacion.notaGrupal));
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const idCampo = useId();

  const error = intentado ? validarNota05(texto) : undefined;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setIntentado(true);
    const err = validarNota05(texto);
    if (err !== undefined) {
      document.getElementById(idCampo)?.focus();
      return;
    }
    setEnviando(true);
    try {
      const ev = await cambiarNotaGrupal(evaluacion.idEvaluacion, numeroNota(texto));
      alActualizar(ev, {
        tipo: 'exito',
        texto: `Nota grupal actualizada a ${textoNota(ev.notaGrupal)}. Las notas sugeridas se recalcularon.`,
      });
    } catch (errApi: unknown) {
      alHecho(
        { tipo: 'error', texto: mensajeErrorEvaluacion(errApi, CAMPOS_NOTA_GRUPAL) },
        esConflicto(errApi)
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tarjeta titulo="Nota grupal">
      <form className="nota-grupal" noValidate onSubmit={handleSubmit}>
        <Campo
          etiqueta="Nueva nota grupal"
          className="campo--nota"
          inputMode="decimal"
          autoComplete="off"
          disabled={bloqueada || enviando}
          id={idCampo}
          value={texto}
          error={error}
          onChange={(e) => setTexto(e.target.value)}
        />
        <Boton
          type="submit"
          variante="secundario"
          disabled={bloqueada}
          cargando={enviando}
        >
          Cambiar nota grupal
        </Boton>
      </form>
      <p className="texto-ayuda">
        {bloqueada
          ? NOTAS_YA_CONFIRMADAS
          : 'Al cambiarla se recalculan las notas sugeridas. El cambio queda en el registro de auditoría.'}
      </p>
    </Tarjeta>
  );
}
