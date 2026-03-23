package it.pagopa.selfcare.party.registry_proxy.connector.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InternalExceptionTest {
    /**
     * Method under test: {@link InternalException#InternalException(Throwable)}
     */
    @Test
    void testConstructor() {
        Throwable throwable = new Throwable();
        InternalException actualInternalException = new InternalException(throwable);
        Throwable cause = actualInternalException.getCause();
        assertSame(throwable, cause);
        Throwable[] suppressed = actualInternalException.getSuppressed();
        assertEquals(0, suppressed.length);
        assertEquals("java.lang.Throwable", actualInternalException.getLocalizedMessage());
        assertEquals("java.lang.Throwable", actualInternalException.getMessage());
        assertNull(cause.getLocalizedMessage());
        assertNull(cause.getCause());
        assertNull(cause.getMessage());
        assertSame(suppressed, cause.getSuppressed());
    }

    @Test
    void constructorWithMessage() {
        String message = "Error message";
        InternalException exception = new InternalException(message);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructorWithMessage_nullMessage() {
        InternalException exception = new InternalException((String) null);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }
}

