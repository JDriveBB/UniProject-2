package co.uniproject.nucleo.ejecucion.dependencias;

import java.time.Duration;
import java.util.Set;

/**
 * Lo que el motor necesita de una tarea. avance en 0..100 (100 = completada); prerrequisitos = tareas que deben
 * terminar antes (la tarea A bloquea a la B ⇔ A está en los prerrequisitos de B).
 */
public record TareaPlan(long id, Duration duracionEstimada, int avance, Set<Long> prerrequisitos) {

    public TareaPlan {
        if (duracionEstimada.isNegative()) {
            throw new IllegalArgumentException("duracionEstimada negativa en la tarea " + id);
        }
        if (avance < 0 || avance > 100) {
            throw new IllegalArgumentException("avance fuera de 0..100 en la tarea " + id);
        }
        prerrequisitos = Set.copyOf(prerrequisitos);
        if (prerrequisitos.contains(id)) {
            throw new IllegalArgumentException("La tarea " + id + " no puede ser su propio prerrequisito");
        }
    }

    public boolean completada() {
        return avance == 100;
    }
}
