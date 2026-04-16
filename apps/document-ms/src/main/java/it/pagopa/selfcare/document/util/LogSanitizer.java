package it.pagopa.selfcare.document.util;

import org.owasp.encoder.Encode;

/**
 * Utility class for sanitizing log values to prevent log injection attacks.
 */
public final class LogSanitizer {

    private LogSanitizer() {
        // Utility class - prevent instantiation
    }

    /**
     * Sanitizes a string value for safe logging.
     *
     * @param value the value to sanitize
     * @return the sanitized value, or "null" if the input is null
     */
    public static String sanitize(String value) {
        return value == null ? "null" : Encode.forJava(value);
    }
}
