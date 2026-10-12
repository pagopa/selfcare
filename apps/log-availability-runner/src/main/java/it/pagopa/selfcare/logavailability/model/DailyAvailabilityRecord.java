package it.pagopa.selfcare.logavailability.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record DailyAvailabilityRecord(
        LocalDate referenceDate,
        String environment,
        long countLt500,
        long countGte500,
        long total,
        BigDecimal availability,
        Instant generationTimestamp) {

    private static final Set<String> ENVIRONMENTS = Set.of("DEV", "UAT", "PROD");

    public DailyAvailabilityRecord {
        if (referenceDate == null || generationTimestamp == null) {
            throw new IllegalArgumentException("Reference date and generation timestamp are required");
        }
        if (!ENVIRONMENTS.contains(environment)) {
            throw new IllegalArgumentException("Environment must be DEV, UAT or PROD");
        }
        if (countLt500 < 0 || countGte500 < 0) {
            throw new IllegalArgumentException("Counts must not be negative");
        }
        long expectedTotal = Math.addExact(countLt500, countGte500);
        if (total != expectedTotal) {
            throw new IllegalArgumentException("Total must equal the sum of both status counts");
        }
        if (availability == null || availability.compareTo(BigDecimal.ZERO) < 0
                || availability.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Availability must be between 0 and 100");
        }
        availability = availability.setScale(2, RoundingMode.HALF_UP);
    }

    public static DailyAvailabilityRecord fromCounts(
            LocalDate referenceDate,
            String environment,
            long countLt500,
            long countGte500,
            Instant generationTimestamp) {
        long total = Math.addExact(countLt500, countGte500);
        BigDecimal percentage = total == 0
                ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf(countLt500)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new DailyAvailabilityRecord(referenceDate, environment, countLt500,
                countGte500, total, percentage, generationTimestamp);
    }

    public String partitionKey() {
        return String.valueOf(referenceDate.getYear());
    }

    public String rowKey() {
        return referenceDate.toString();
    }
}
