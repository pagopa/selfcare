package it.pagopa.selfcare.mscore.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InvalidRequestExceptionTest {
    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link InvalidRequestException#InvalidRequestException(String, String)}
     *   <li>{@link InvalidRequestException#getCode()}
     * </ul>
     */
    @Test
    void testConstructor() {
        assertEquals("Code", (new InvalidRequestException("An error occurred", "Code")).getCode());
    }
}

