package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PreconditionsTest {

    @Test
    void validArgumentsAreNotChanged() {
        // given
        List<String> values = List.of("value");

        // when / then
        assertDoesNotThrow(() -> {
            Preconditions.notNull("", "missing");
            Preconditions.hasText(" value ", "missing");
            Preconditions.notEmpty(values, "missing");
            Preconditions.state(true, "invalid");
        });
    }

    @Test
    void nullArgumentRetainsTheSpringExceptionAndMessage() {
        // given
        Object missing = null;

        // when
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Preconditions.notNull(missing, "missing"));

        // then
        assertEquals("missing", failure.getMessage());
    }

    @Test
    void blankTextRetainsTheSpringExceptionAndMessage() {
        // given
        String blank = "\u2003";

        // when
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Preconditions.hasText(blank, "blank"));

        // then
        assertEquals("blank", failure.getMessage());
    }

    @Test
    void emptyCollectionRetainsTheSpringExceptionAndMessage() {
        // given
        List<String> empty = List.of();

        // when
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Preconditions.notEmpty(empty, "empty"));

        // then
        assertEquals("empty", failure.getMessage());
    }

    @Test
    void nullCollectionRetainsTheSpringExceptionAndMessage() {
        // given
        List<String> absent = null;

        // when
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Preconditions.notEmpty(absent, "absent"));

        // then
        assertEquals("absent", failure.getMessage());
    }

    @Test
    void violatedStateRetainsTheSpringExceptionAndMessage() {
        // given
        boolean expression = false;

        // when
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> Preconditions.state(expression, "state"));

        // then
        assertEquals("state", failure.getMessage());
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
