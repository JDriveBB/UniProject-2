import { useId, useMemo, useState, type FormEvent } from 'react';
import { evaluarHito } from '../api/evaluacion';
import type { CriterioVista, EvaluacionVista } from '../api/tipos';
import { Boton } from '../componentes/Boton';
import { Campo } from '../componentes/Campo';
import { CampoArea } from '../componentes/CampoArea';
import { Modal } from '../componentes/Modal';
import { Tarjeta } from '../componentes/Tarjeta';
import { decimalEsCo, fechaIsoBogota } from '../formato';
import type { Mensaje } from '../proyectos/errores';
import { notaCalculada, textoCentesimas, textoNota } from './calculo';
import {
  borradorInicial,
  difiereDeCalculada,
  primerCampoConError,
  sinErrores,
  solicitudEvaluacion,
  validarEvaluacion,
  type BorradorEvaluacion,
  type ErroresEvaluacion,
} from './formulario';
import {
  CAMPOS_EVALUACION,
  esConflicto,
  mensajeErrorEvaluacion,
  textoConfirmarRegistro,
  textoDiferencia,
  textoRegistrada,
} from './vista';

export interface PropsFormularioEvaluacion {
  idHitoEquipo: number;
  criterios: CriterioVista[];
  anterior: EvaluacionVista | null;
  fechaLimite: string;
  alHecho: (m: Mensaje, recargar: boolean) => void;
}

const ERRORES_VACIOS: ErroresEvaluacion = { criterios: {} };

export function FormularioEvaluacion({
  idHitoEquipo,
  criterios,
  anterior,
  fechaLimite,
  alHecho,
}: PropsFormularioEvaluacion) {
  const [borrador, setBorrador] = useState<BorradorEvaluacion>(() =>
    borradorInicial(criterios, anterior)
  );
  const [intentado, setIntentado] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const idBase = useId();
  const idBotonRegistrar = `${idBase}-registrar`;
  const idCampo = (clave: string) => `${idBase}-${clave}`;

  const errores: ErroresEvaluacion = intentado
    ? validarEvaluacion(borrador, criterios)
    : ERRORES_VACIOS;

  const notasPorCriterio = useMemo(() => {
    const notas: Record<number, string> = {};
    for (const c of criterios) {
      notas[c.idCriterio] = borrador.criterios[c.idCriterio]?.nota ?? '';
    }
    return notas;
  }, [criterios, borrador.criterios]);

  const calculada = notaCalculada(criterios, notasPorCriterio);

  function alEnviarFormulario(e: FormEvent) {
    e.preventDefault();
    setIntentado(true);
    const err = validarEvaluacion(borrador, criterios);
    if (!sinErrores(err)) {
      const campo = primerCampoConError(err, criterios);
      if (campo !== null) {
        document.getElementById(idCampo(campo))?.focus();
      }
      return;
    }
    setConfirmando(true);
  }

  async function registrar() {
    setEnviando(true);
    try {
      const ev = await evaluarHito(idHitoEquipo, solicitudEvaluacion(borrador, criterios));
      setConfirmando(false);
      setEnviando(false);
      alHecho({ tipo: 'exito', texto: textoRegistrada(ev) }, true);
    } catch (e: unknown) {
      setConfirmando(false);
      setEnviando(false);
      alHecho(
        { tipo: 'error', texto: mensajeErrorEvaluacion(e, CAMPOS_EVALUACION) },
        esConflicto(e)
      );
    }
  }

  return (
    <>
      <form className="detalle-tarea__columna" noValidate onSubmit={alEnviarFormulario}>
        <Tarjeta titulo="Rúbrica de evaluación">
          <p className="texto-secundario">
            Califica cada criterio. La nota de cada uno va de 0 a su puntaje máximo.
          </p>
          <ul className="criterios-evaluacion">
            {criterios.map((c) => {
              const idTituloCriterio = `${idBase}-titulo-${c.idCriterio}`;
              const borradorCriterio = borrador.criterios[c.idCriterio] ?? {
                nota: '',
                retroalimentacion: '',
              };
              const errorCriterio = errores.criterios[c.idCriterio];

              return (
                <li key={c.idCriterio}>
                  <div
                    className="criterio-evaluacion"
                    role="group"
                    aria-labelledby={idTituloCriterio}
                  >
                    <p id={idTituloCriterio} className="criterio-evaluacion__titulo">
                      {c.descripcion}
                    </p>
                    <p className="criterio-evaluacion__meta">
                      {`Peso ${decimalEsCo(c.pesoPorcentaje)} % · Puntaje máximo ${textoCentesimas(c.puntajeMaximo)}`}
                    </p>
                    <div className="criterio-evaluacion__fila">
                      <Campo
                        etiqueta="Nota"
                        className="campo--nota"
                        inputMode="decimal"
                        autoComplete="off"
                        disabled={enviando}
                        id={idCampo(`nota-${c.idCriterio}`)}
                        value={borradorCriterio.nota}
                        error={errorCriterio?.nota}
                        onChange={(e) => {
                          const valor = e.target.value;
                          setBorrador((prev) => ({
                            ...prev,
                            criterios: {
                              ...prev.criterios,
                              [c.idCriterio]: {
                                nota: valor,
                                retroalimentacion:
                                  prev.criterios[c.idCriterio]?.retroalimentacion ?? '',
                              },
                            },
                          }));
                        }}
                      />
                      <CampoArea
                        etiqueta="Retroalimentación (opcional)"
                        className="campo--retroalimentacion"
                        disabled={enviando}
                        id={idCampo(`retro-${c.idCriterio}`)}
                        value={borradorCriterio.retroalimentacion}
                        error={errorCriterio?.retroalimentacion}
                        onChange={(e) => {
                          const valor = e.target.value;
                          setBorrador((prev) => ({
                            ...prev,
                            criterios: {
                              ...prev.criterios,
                              [c.idCriterio]: {
                                nota: prev.criterios[c.idCriterio]?.nota ?? '',
                                retroalimentacion: valor,
                              },
                            },
                          }));
                        }}
                      />
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        </Tarjeta>

        <Tarjeta titulo="Nota del hito">
          <p className="resumen-hito__dato">
            <span className="resumen-hito__etiqueta">Nota calculada:</span>{' '}
            <strong>{calculada !== null ? textoNota(calculada) : '—'}</strong>
          </p>
          <p className="texto-ayuda">
            Se calcula con la rúbrica: suma de (nota ÷ puntaje máximo × peso) ÷ 100 × 5.
          </p>
          <Campo
            etiqueta="Nota grupal (opcional)"
            className="campo--nota"
            inputMode="decimal"
            autoComplete="off"
            placeholder={calculada !== null ? textoNota(calculada) : undefined}
            disabled={enviando}
            id={idCampo('notaGrupal')}
            value={borrador.notaGrupal}
            error={errores.notaGrupal}
            onChange={(e) => {
              const valor = e.target.value;
              setBorrador((prev) => ({ ...prev, notaGrupal: valor }));
            }}
          />
          <p className="texto-ayuda">
            {difiereDeCalculada(borrador.notaGrupal, calculada) && calculada !== null
              ? textoDiferencia(calculada)
              : 'Déjala vacía para usar la nota calculada.'}
          </p>
          <CampoArea
            etiqueta="Retroalimentación general (opcional)"
            disabled={enviando}
            id={idCampo('retroalimentacion')}
            value={borrador.retroalimentacion}
            error={errores.retroalimentacion}
            onChange={(e) => {
              const valor = e.target.value;
              setBorrador((prev) => ({ ...prev, retroalimentacion: valor }));
            }}
          />
          <div className="pie-evaluacion">
            <Boton id={idBotonRegistrar} type="submit" variante="primario" cargando={enviando}>
              Registrar evaluación
            </Boton>
          </div>
        </Tarjeta>
      </form>

      {confirmando && (
        <Modal
          ancho="dialogo"
          titulo="¿Registrar la evaluación?"
          ocupado={enviando}
          alCerrar={() => {
            setConfirmando(false);
            document.getElementById(idBotonRegistrar)?.focus();
          }}
          alEnviar={registrar}
          acciones={(cerrar) => (
            <>
              <Boton
                type="button"
                variante="secundario"
                disabled={enviando}
                onClick={cerrar}
              >
                Cancelar
              </Boton>
              <Boton
                type="submit"
                variante="primario"
                cargando={enviando}
              >
                Registrar evaluación
              </Boton>
            </>
          )}
        >
          <p className="modal__texto">
            {textoConfirmarRegistro(fechaLimite, fechaIsoBogota(new Date()))}
          </p>
        </Modal>
      )}
    </>
  );
}
