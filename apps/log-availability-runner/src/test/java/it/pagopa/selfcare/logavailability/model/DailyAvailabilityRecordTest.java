package it.pagopa.selfcare.logavailability.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyAvailabilityRecordTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-10-07");
    private static final Instant GENERATED_AT = Instant.parse("2026-10-08T01:00:00Z");

    @Test
    void computesAvailabilityRoundedToTwoDecimalPlaces() {
        DailyAvailabilityRecord record = DailyAvailabilityRecord.fromCounts(
                REFERENCE_DATE, "PROD", 1999, 1, GENERATED_AT);

        assertEquals(2000, record.total());
        assertEquals(new BigDecimal("99.95"), record.availability());
    }

    @Test
    void returnsOneHundredPercentWhenThereIsNoTraffic() {
        DailyAvailabilityRecord record = DailyAvailabilityRecord.fromCounts(
                REFERENCE_DATE, "DEV", 0, 0, GENERATED_AT);

        assertEquals(new BigDecimal("100.00"), record.availability());
    }

    @Test
    void usesYearAndReferenceDateAsStorageKeys() {
        DailyAvailabilityRecord record = DailyAvailabilityRecord.fromCounts(
                REFERENCE_DATE, "UAT", 10, 2, GENERATED_AT);

        assertEquals("2026", record.partitionKey());
        assertEquals("2026-10-07", record.rowKey());
    }

    @Test
    void rejectsNegativeCountsAndInvalidEnvironment() {
        assertThrows(IllegalArgumentException.class, () ->
                DailyAvailabilityRecord.fromCounts(REFERENCE_DATE, "PROD", -1, 0, GENERATED_AT));
        assertThrows(IllegalArgumentException.class, () ->
                DailyAvailabilityRecord.fromCounts(REFERENCE_DATE, "DEV-AR", 1, 0, GENERATED_AT));
    }

    @Test
    void rejectsInconsistentTotals() {
        assertThrows(IllegalArgumentException.class, () ->
                new DailyAvailabilityRecord(REFERENCE_DATE, "PROD", 1, 2, 4,
                        BigDecimal.valueOf(25), GENERATED_AT));
    }
}
