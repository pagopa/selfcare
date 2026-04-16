package it.pagopa.selfcare.mscore.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MsCoreExceptionTest {
    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link MsCoreException#MsCoreException(String, String)}
     *   <li>{@link MsCoreException#getCode()}
     * </ul>
     */
    @Test
    void testConstructor() {
        assertEquals("Code", (new MsCoreException("An error occurred", "Code")).getCode());
    }
}

