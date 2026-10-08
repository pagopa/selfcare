package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LogUtilsTest {

    @Test
    void keepsNullAndOrdinaryIdentifiers() {
        assertNull(LogUtils.sanitize(null));
        assertEquals("prod-io", LogUtils.sanitize("prod-io"));
    }

    @Test
    void escapesControlCharactersInsteadOfCreatingLogLines() {
        assertEquals("id\\r\\nforged\\tentry", LogUtils.sanitize("id\r\nforged\tentry"));
    }

    @Test
    void escapesTheStringRepresentationOfRequestObjects() {
        Object request = new Object() {
            @Override
            public String toString() {
                return "request\nforged";
            }
        };
        assertEquals("request\\nforged", LogUtils.sanitize(request));
    }
}
