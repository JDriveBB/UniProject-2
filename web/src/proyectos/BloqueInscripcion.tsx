import { useState } from 'react';
import type { FormEvent } from 'react';
import { configurarInscripcion } from '../api/proyectos';
import type { InscripcionVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { useAvisos } from '../componentes/Avisos';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { instanteDeValorLocal, valorLocalBogota } from '../formato';
import { estadoInscripcion, validarCierre, validarHorasRonda } from './cupos';
import { mensajeErrorApi } from './errores';

const CAMPOS_INSCRIPCION = [
  ['cierreEquipos', 'Cierre de la inscripción'],
  ['horasRonda', 'Horas de la votación'],
] as const;

/** Fecha de cierre de la inscripción a equipos y duración de la votación de líder (solo el docente dueño). */
export function BloqueInscripcion({
  idProyecto,
  inscripcion,
  alGuardado,
}: {
  idProyecto: number;
  inscripcion: InscripcionVista | null;
  alGuardado: (v: InscripcionVista) => void;
}) {
  if (inscripcion === null) {
    return <p className="texto-secundario">Cargando…</p>;
  }
  return <FormularioInscripcion idProyecto={idProyecto} inscripcion={inscripcion} alGuardado={alGuardado} />;
}

function FormularioInscripcion({
  idProyecto,
  inscripcion,
  alGuardado,
}: {
  idProyecto: number;
  inscripcion: InscripcionVista;
  alGuardado: (v: InscripcionVista) => void;
}) {
  const avisar = useAvisos();
  const [cierre, setCierre] = useState(() =>
    inscripcion.cierreEquipos === null ? '' : valorLocalBogota(inscripcion.cierreEquipos),
  );
  const [horas, setHoras] = useState(String(inscripcion.horasRonda));
  const [intentado, setIntentado] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const errorCierre = intentado ? (validarCierre(cierre, new Date()) ?? undefined) : undefined;
  const errorHoras = intentado ? (validarHorasRonda(horas) ?? undefined) : undefined;

  async function enviar(cierreEquipos: string | null, horasRonda: number) {
    setOcupado(true);
    setError(null);
    try {
      const v = await configurarInscripcion(idProyecto, { cierreEquipos, horasRonda });
      setCierre(v.cierreEquipos === null ? '' : valorLocalBogota(v.cierreEquipos));
      setHoras(String(v.horasRonda));
      setIntentado(false);
      avisar('Inscripción guardada.');
      alGuardado(v);
    } catch (e) {
      setError(mensajeErrorApi(e, CAMPOS_INSCRIPCION));
    } finally {
      setOcupado(false);
    }
  }

  function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setIntentado(true);
    if (validarCierre(cierre, new Date()) !== null || validarHorasRonda(horas) !== null) {
      return;
    }
    void enviar(cierre.trim() === '' ? null : instanteDeValorLocal(cierre), Number(horas.trim()));
  }

  return (
    <div className="pila">
      <h3 className="tarjeta__subtitulo">Inscripción a equipos</h3>
      <p className="texto-secundario">{estadoInscripcion(inscripcion)}</p>
      <form className="pila" onSubmit={guardar} noValidate>
        <div className="fila-campos">
          <Campo
            etiqueta="Cierre de la inscripción (hora de Colombia)"
            type="datetime-local"
            value={cierre}
            onChange={(ev) => setCierre(ev.target.value)}
            error={errorCierre}
            disabled={ocupado}
          />
          <Campo
            etiqueta="Duración de la votación de líder (horas)"
            type="number"
            min={1}
            max={336}
            value={horas}
            onChange={(ev) => setHoras(ev.target.value)}
            error={errorHoras}
            disabled={ocupado}
          />
        </div>
        {error !== null && <Alerta tipo="error">{error}</Alerta>}
        <div className="pagina__acciones">
          <Boton type="submit" cargando={ocupado}>
            Guardar
          </Boton>
          {inscripcion.cierreEquipos !== null && (
            <Boton
              variante="secundario"
              disabled={ocupado}
              onClick={() => void enviar(null, inscripcion.horasRonda)}
            >
              Quitar fecha
            </Boton>
          )}
        </div>
      </form>
    </div>
  );
}
