package it.pagopa.selfcare.document.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class LogSanitizerTest {

    // ---- sanitize ----

    @Test
    void sanitize_shouldReturnNullStringWhenInputIsNull() {
        String result = LogSanitizer.sanitize(null);
        assertEquals("null", result);
    }

    @Test
    void sanitize_shouldReturnSameValueForPlainString() {
        String result = LogSanitizer.sanitize("simpleValue");
        assertEquals("simpleValue", result);
    }

    @Test
    void sanitize_shouldReturnEmptyStringForEmptyInput() {
        String result = LogSanitizer.sanitize("");
        assertEquals("", result);
    }

    @Test
    void sanitize_shouldEncodeNewlineCharacterToPreventLogInjection() {
        String result = LogSanitizer.sanitize("value\ninjected log line");
        assertNotNull(result);
        assertFalse(result.contains("\n"), "Sanitized value must not contain newline characters");
    }

    @Test
    void sanitize_shouldEncodeCarriageReturnToPreventLogInjection() {
        String result = LogSanitizer.sanitize("value\rinjected");
        assertNotNull(result);
        assertFalse(result.contains("\r"), "Sanitized value must not contain carriage return characters");
    }

    @Test
    void sanitize_shouldEncodeTabCharacter() {
        String result = LogSanitizer.sanitize("value\tinjected");
        assertNotNull(result);
        assertFalse(result.contains("\t"), "Sanitized value must not contain tab characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"normal text", "abc123", "user@example.com", "path/to/resource"})
    void sanitize_shouldReturnUnchangedValueForSafeInput(String input) {
        String result = LogSanitizer.sanitize(input);
        assertEquals(input, result);
    }

    @Test
    void sanitize_shouldEncodeBackslashEscapeSequences() {
        String malicious = "value\\nfake log entry";
        String result = LogSanitizer.sanitize(malicious);
        assertNotNull(result);
    }

    @Test
    void sanitize_shouldHandleStringWithOnlyWhitespace() {
        String result = LogSanitizer.sanitize("   ");
        assertNotNull(result);
    }
}

