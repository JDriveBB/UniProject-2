import { useState } from 'react';
import { crearCurso, editarCurso } from '../api/cursos';
import type { CursoVista } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { CampoArea } from '../componentes/CampoArea';
import { Modal } from '../componentes/Modal';
import { mensajeErrorApi } from '../proyectos/errores';
import {
  CAMPOS_CURSO,
  MAXIMO_DESCRIPCION_CURSO,
  MAXIMO_NOMBRE_CURSO,
  MAXIMO_PERIODO,
  solicitudCurso,
  validarCurso,
} from './vista';
import type { ErroresCurso } from './vista';

/** Modal para crear o editar un curso del docente. */
export function FormularioCurso({
  inicial,
  alCerrar,
  alGuardado,
}: {
  inicial: CursoVista | null;
  alCerrar: () => void;
  alGuardado: (curso: CursoVista) => void;
}) {
  const [nombre, setNombre] = useState(inicial?.nombre ?? '');
  const [periodo, setPeriodo] = useState(inicial?.periodo ?? '');
  const [descripcion, setDescripcion] = useState(inicial?.descripcion ?? '');
  const [errores, setErrores] = useState<ErroresCurso>({});
  const [errorApi, setErrorApi] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  async function alEnviar() {
    setErrorApi(null);
    const encontrados = validarCurso(nombre, periodo, descripcion);
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) {
      return;
    }
    setOcupado(true);
    try {
      const solicitud = solicitudCurso(nombre, periodo, descripcion);
      const guardado =
        inicial === null ? await crearCurso(solicitud) : await editarCurso(inicial.idCurso, solicitud);
      alGuardado(guardado);
    } catch (e) {
      setErrorApi(mensajeErrorApi(e, CAMPOS_CURSO));
    } finally {
      setOcupado(false);
    }
  }

  return (
    <Modal
      titulo={inicial === null ? 'Nuevo curso' : 'Editar curso'}
      ocupado={ocupado}
      alCerrar={alCerrar}
      alEnviar={() => void alEnviar()}
      acciones={(cerrar) => (
        <>
          <Boton variante="secundario" onClick={cerrar} disabled={ocupado}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={ocupado}>
            {inicial === null ? 'Crear curso' : 'Guardar cambios'}
          </Boton>
        </>
      )}
    >
      <div className="pila">
        {errorApi !== null && <Alerta tipo="error">{errorApi}</Alerta>}
        <Campo
          etiqueta="Nombre del curso"
          placeholder="Diseño de Software I"
          maxLength={MAXIMO_NOMBRE_CURSO}
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          error={errores.nombre}
          disabled={ocupado}
          required
        />
        <Campo
          etiqueta="Periodo"
          placeholder="2026-2"
          maxLength={MAXIMO_PERIODO}
          value={periodo}
          onChange={(e) => setPeriodo(e.target.value)}
          error={errores.periodo}
          disabled={ocupado}
          required
        />
        <CampoArea
          etiqueta="Descripción (opcional)"
          maxLength={MAXIMO_DESCRIPCION_CURSO}
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          error={errores.descripcion}
          disabled={ocupado}
        />
      </div>
    </Modal>
  );
}
