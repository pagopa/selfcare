package it.pagopa.selfcare.logavailability.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunOptionsTest {

    private static final ZoneId ITALY = ZoneId.of("Europe/Rome");

    @Test
    void runsWhenTheContainerStartsDuringTheThreeAmHourInSummerTime() {
        RunOptions options = RunOptions.parse(
                new String[0], Instant.parse("2026-06-01T01:15:00Z"), ITALY, 90);

        assertTrue(options.shouldRun());
        assertEquals(java.util.List.of(LocalDate.parse("2026-05-31")), options.referenceDates());
    }

    @Test
    void skipsTheUtcTriggerThatIsNotThreeInTheMorningDuringWinterTime() {
        RunOptions options = RunOptions.parse(
                new String[0], Instant.parse("2026-12-01T01:00:00Z"), ITALY, 90);

        assertFalse(options.shouldRun());
        assertTrue(options.referenceDates().isEmpty());
    }

    @Test
    void runsAtThreeInTheMorningDuringItalianWinterTime() {
        RunOptions options = RunOptions.parse(
                new String[0], Instant.parse("2026-12-01T02:15:00Z"), ITALY, 90);

        assertTrue(options.shouldRun());
        assertEquals(java.util.List.of(LocalDate.parse("2026-11-30")), options.referenceDates());
    }

    @Test
    void acceptsOnlyCompleteDaysInsideRetentionForManualRuns() {
        RunOptions options = RunOptions.parse(
                new String[]{"2026-09-01"}, Instant.parse("2026-10-08T01:00:00Z"), ITALY, 90);

        assertTrue(options.shouldRun());
        assertEquals(java.util.List.of(LocalDate.parse("2026-09-01")), options.referenceDates());
        assertThrows(IllegalArgumentException.class, () -> RunOptions.parse(
                new String[]{"2026-07-10"}, Instant.parse("2026-10-08T01:00:00Z"), ITALY, 90));
        assertThrows(IllegalArgumentException.class, () -> RunOptions.parse(
                new String[]{"2026-10-08"}, Instant.parse("2026-10-08T01:00:00Z"), ITALY, 90));
    }

    @Test
    void backfillsOnlyCompleteDaysWithinRetention() {
        RunOptions options = RunOptions.parse(
                new String[]{"--backfill"}, Instant.parse("2026-10-08T01:00:00Z"), ITALY, 90);

        assertTrue(options.shouldRun());
        assertEquals(LocalDate.parse("2026-07-11"), options.referenceDates().get(0));
        assertEquals(LocalDate.parse("2026-10-07"),
                options.referenceDates().get(options.referenceDates().size() - 1));
        assertEquals(89, options.referenceDates().size());
    }
}
