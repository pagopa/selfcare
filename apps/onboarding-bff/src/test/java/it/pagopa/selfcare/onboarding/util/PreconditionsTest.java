package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "\r", "\u2003"})
    void missingTextIsRejectedEvenAfterNullSafeDiagnosticSanitization(String value) {
        assertDoesNotThrow(() -> LogUtils.sanitize(value));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Preconditions.hasText(value, "An Institution ID is required"));
        assertEquals("An Institution ID is required", failure.getMessage());
    }
}
