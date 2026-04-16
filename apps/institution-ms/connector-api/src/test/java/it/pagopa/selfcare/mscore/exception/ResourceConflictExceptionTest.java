package it.pagopa.selfcare.mscore.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ResourceConflictExceptionTest {
    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link ResourceConflictException#ResourceConflictException(String, String)}
     *   <li>{@link ResourceConflictException#getCode()}
     * </ul>
     */
    @Test
    void testConstructor() {
        assertEquals("Code", (new ResourceConflictException("An error occurred", "Code")).getCode());
    }
}

