package it.pagopa.selfcare.dashboard.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvalidRequestExceptionTest {
    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link InvalidRequestException#InvalidRequestException(String)}
     *   <li>{@link InvalidRequestException#getMessage()}
     * </ul>
     */
    @Test
    void testConstructor() {
        assertEquals("Invalid Request", (new InvalidRequestException("Invalid Request")).getMessage());
    }
}

