package co.uniproject.nucleo.evaluacion.calificacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Índice de Contribución Efectiva y nota individual sugerida (entrega1 sección 2.5 y sección 2.6; plan-dominio C1). Java puro.
 * aporte_i = Σ horas_estimadas × f (f = 1 a tiempo, 0,5 tarde); ICE_i = aporte_i · n / Σ aporte (todos 1 si Σ = 0);
 * nota_i = min(5, grupal · (1 − α + α · ICE_i)) con α = 0,5. Es una sugerencia: la nota final la fija el docente.
 */
public final class CalculadoraIce {

    public static final BigDecimal ALFA = new BigDecimal("0.5");
    public static final BigDecimal FACTOR_TARDIA = new BigDecimal("0.5");
    public static final BigDecimal NOTA_MAXIMA = new BigDecimal("5.00");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CINCO = BigDecimal.valueOf(5);

    private CalculadoraIce() {}

    /** RF8: nota grupal = Σ (nota / puntajeMaximo · peso) / 100 · 5, con 2 decimales HALF_UP. Los pesos suman 100. */
    public static BigDecimal notaGrupal(Collection<CriterioCalificado> criterios) {
        if (criterios.isEmpty()) {
            throw new IllegalArgumentException("La rúbrica no tiene criterios");
        }
        BigDecimal pesos = BigDecimal.ZERO;
        BigDecimal suma = BigDecimal.ZERO;
        for (CriterioCalificado c : criterios) {
            pesos = pesos.add(c.pesoPorcentaje());
            suma = suma.add(c.nota().multiply(c.pesoPorcentaje()).divide(c.puntajeMaximo(), 10, RoundingMode.HALF_UP));
        }
        if (pesos.compareTo(CIEN) != 0) {
            throw new IllegalArgumentException("Los pesos de la rúbrica deben sumar 100");
        }
        return suma.multiply(CINCO).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    /**
     * Una entrada por integrante, ordenada por id. La nota se calcula con el ICE ya redondeado a 4 decimales
     * (el mismo que se guarda y se muestra), para que el docente pueda rehacer la cuenta.
     */
    public static List<NotaSugerida> calcular(BigDecimal notaGrupal, Collection<Long> integrantes,
                                              Collection<TareaCompletada> tareas) {
        if (notaGrupal.signum() < 0 || notaGrupal.compareTo(NOTA_MAXIMA) > 0) {
            throw new IllegalArgumentException("notaGrupal fuera de 0..5");
        }
        Set<Long> ids = new TreeSet<>(integrantes);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("El equipo no tiene integrantes");
        }
        Map<Long, BigDecimal> aporte = new TreeMap<>();
        for (long id : ids) {
            aporte.put(id, BigDecimal.ZERO);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (TareaCompletada t : tareas) {
            if (!aporte.containsKey(t.idEstudiante())) {
                throw new IllegalArgumentException("Tarea de alguien fuera del equipo: " + t.idEstudiante());
            }
            BigDecimal credito = t.aTiempo() ? t.horasEstimadas() : t.horasEstimadas().multiply(FACTOR_TARDIA);
            aporte.merge(t.idEstudiante(), credito, BigDecimal::add);
            total = total.add(credito);
        }
        BigDecimal n = BigDecimal.valueOf(ids.size());
        List<NotaSugerida> resultado = new ArrayList<>(ids.size());
        for (Map.Entry<Long, BigDecimal> e : aporte.entrySet()) {
            BigDecimal ice = total.signum() == 0
                    ? BigDecimal.ONE.setScale(4)
                    : e.getValue().multiply(n).divide(total, 4, RoundingMode.HALF_UP);
            BigDecimal nota = notaIndividual(notaGrupal, ice);
            resultado.add(new NotaSugerida(e.getKey(), e.getValue(), ice, nota));
        }
        return List.copyOf(resultado);
    }

    /** nota_i = min(5, grupal · (1 − α + α · ICE_i)), 2 decimales HALF_UP. Lo reutiliza el cambio de nota grupal. */
    public static BigDecimal notaIndividual(BigDecimal notaGrupal, BigDecimal ice) {
        return notaGrupal.multiply(BigDecimal.ONE.subtract(ALFA).add(ALFA.multiply(ice)))
                .setScale(2, RoundingMode.HALF_UP)
                .min(NOTA_MAXIMA);
    }
}
