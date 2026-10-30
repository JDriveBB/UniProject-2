package co.uniproject.nucleo.ejecucion.dependencias;

/**
 * RF9 (plan-dominio §3, R2). ALTO: holgura proyectada negativa (compromete su fecha límite: dispara RF4).
 * MEDIO: holgura menor que un cuarto de la duración estimada. BAJO: el resto y toda tarea completada.
 */
public enum NivelRiesgo { BAJO, MEDIO, ALTO }
