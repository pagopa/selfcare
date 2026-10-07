package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PreconditionsTest {

    @Test
    void validArgumentsAreNotChanged() {
        assertDoesNotThrow(() -> {
            Preconditions.notNull("", "missing");
            Preconditions.hasText(" value ", "missing");
            Preconditions.notEmpty(List.of("value"), "missing");
            Preconditions.notEmpty(Map.of("key", "value"), "missing");
            Preconditions.isTrue(true, "invalid");
            Preconditions.state(true, "invalid");
        });
    }

    @Test
    void invalidArgumentsRetainTheSpringExceptionAndMessage() {
        assertEquals("missing", assertThrows(IllegalArgumentException.class,
                () -> Preconditions.notNull(null, "missing")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Preconditions.hasText("\u2003", "blank"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.notEmpty(List.of(), "empty"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.notEmpty(Map.of(), "empty"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.isTrue(false, "invalid"));
        assertEquals("state", assertThrows(IllegalStateException.class,
                () -> Preconditions.state(false, "state")).getMessage());
    }
}
