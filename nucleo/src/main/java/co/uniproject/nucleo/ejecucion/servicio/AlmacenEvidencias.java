package co.uniproject.nucleo.ejecucion.servicio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Decisiones 17b-1 y 17b-4: archivos de evidencia en el volumen local, con nombre generado por el servidor. */
@Component
public class AlmacenEvidencias {

    private static final Logger log = LoggerFactory.getLogger(AlmacenEvidencias.class);

    private final Path raiz;

    public AlmacenEvidencias(@Value("${uniproject.evidencias.raiz}") String raiz) {
        this.raiz = Path.of(raiz).toAbsolutePath().normalize();
    }

    /** Escribe el archivo y devuelve su clave. Si la transacción en curso no se confirma, lo borra. */
    public String guardar(byte[] contenido, String extension) {
        String clave = "evidencias/" + UUID.randomUUID() + "." + extension;
        Path destino = ruta(clave);
        try {
            Files.createDirectories(destino.getParent());
            Path temporal = Files.createTempFile(destino.getParent(), "subida-", ".tmp");
            Files.write(temporal, contenido);
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la evidencia", e);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int estado) {
                    if (estado != STATUS_COMMITTED) {
                        borrar(clave);
                    }
                }
            });
        }
        return clave;
    }

    /** Ruta en disco de una clave; nunca sale de la raíz. */
    public Path ruta(String clave) {
        Path ruta = raiz.resolve(clave).normalize();
        if (!ruta.startsWith(raiz)) {
            throw new IllegalArgumentException("Clave de evidencia inválida");
        }
        return ruta;
    }

    void borrar(String clave) {
        try {
            Files.deleteIfExists(ruta(clave));
        } catch (IOException e) {
            log.warn("No se pudo borrar la evidencia huérfana {}", clave);
        }
    }
}
