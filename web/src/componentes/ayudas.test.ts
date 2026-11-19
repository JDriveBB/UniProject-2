import { describe, expect, it } from 'vitest';
import {
  AYUDA_RIESGO,
  AYUDA_RUTA_CRITICA,
  EXPLICACION_ALERTAS,
  EXPLICACION_ESTADO,
  EXPLICACION_VENCIDO,
} from './ayudas';

describe('ayudas', () => {
  it('define textos no vacíos para todas las ayudas', () => {
    for (const texto of [
      AYUDA_RUTA_CRITICA,
      AYUDA_RIESGO,
      EXPLICACION_ESTADO,
      EXPLICACION_ALERTAS,
      EXPLICACION_VENCIDO,
    ]) {
      expect(texto.trim().length).toBeGreaterThan(0);
    }
  });

  it('explica el riesgo mencionando 4 horas', () => {
    expect(AYUDA_RIESGO).toContain('4 horas');
  });
});
