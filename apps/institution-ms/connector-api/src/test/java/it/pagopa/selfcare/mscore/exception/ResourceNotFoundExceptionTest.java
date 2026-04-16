package it.pagopa.selfcare.mscore.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ResourceNotFoundExceptionTest {
    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link ResourceNotFoundException#ResourceNotFoundException(String, String)}
     *   <li>{@link ResourceNotFoundException#getCode()}
     * </ul>
     */
    @Test
    void testConstructor() {
        assertEquals("Code", (new ResourceNotFoundException("An error occurred", "Code")).getCode());
    }
}

