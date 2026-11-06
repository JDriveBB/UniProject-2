package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.eventos.interno.ContratoInterno;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Código de recuperación (decisión 28-3): 8 bytes de selector + 24 de verificador, en Base64 URL sin relleno
 * (43 caracteres). Se guarda el selector y el SHA-256 del verificador; el verificador se compara en tiempo constante.
 */
final class CodigosRecuperacion {

    static final int BYTES_SELECTOR = 8;
    static final int BYTES_VERIFICADOR = 24;

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Pattern PATRON = Pattern.compile(ContratoInterno.PATRON_CODIGO_RECUPERACION);

    /** Código recién generado. toString no muestra el texto (es secreto). */
    record Nuevo(String texto, byte[] selector, byte[] hashVerificador) {
        @Override
        public String toString() {
            return "Nuevo[texto=***]";
        }
    }

    /** Código recibido, ya separado. toString no muestra nada. */
    record Partes(byte[] selector, byte[] verificador) {
        @Override
        public String toString() {
            return "Partes[***]";
        }
    }

    private CodigosRecuperacion() {
    }

    static Nuevo nuevo() {
        byte[] bytes = new byte[BYTES_SELECTOR + BYTES_VERIFICADOR];
        ALEATORIO.nextBytes(bytes);
        byte[] selector = Arrays.copyOfRange(bytes, 0, BYTES_SELECTOR);
        byte[] verificador = Arrays.copyOfRange(bytes, BYTES_SELECTOR, bytes.length);
        return new Nuevo(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), selector, hash(verificador));
    }

    static Optional<Partes> leer(String texto) {
        if (texto == null || !PATRON.matcher(texto).matches()) {
            return Optional.empty();
        }
        byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode(texto);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (bytes.length != BYTES_SELECTOR + BYTES_VERIFICADOR) {
            return Optional.empty();
        }
        return Optional.of(new Partes(Arrays.copyOfRange(bytes, 0, BYTES_SELECTOR),
                Arrays.copyOfRange(bytes, BYTES_SELECTOR, bytes.length)));
    }

    static boolean coincide(byte[] verificador, byte[] hashGuardado) {
        return hashGuardado != null && MessageDigest.isEqual(hash(verificador), hashGuardado);
    }

    static byte[] hash(byte[] datos) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(datos);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
