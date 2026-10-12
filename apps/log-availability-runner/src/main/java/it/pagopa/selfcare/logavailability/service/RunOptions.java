package it.pagopa.selfcare.logavailability.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public record RunOptions(boolean shouldRun, List<LocalDate> referenceDates) {

    public RunOptions {
        referenceDates = List.copyOf(referenceDates);
    }

    public static RunOptions parse(
            String[] args,
            Instant now,
            ZoneId scheduledTimeZone,
            int sourceRetentionDays) {
        if (sourceRetentionDays < 2) {
            throw new IllegalArgumentException("Source retention must be at least two days");
        }
        if (args.length == 0) {
            ZonedDateTime scheduledTime = now.atZone(scheduledTimeZone);
            if (scheduledTime.getHour() != 3) {
                return new RunOptions(false, List.of());
            }
            return new RunOptions(true,
                    List.of(now.atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)));
        }
        if (args.length != 1 || args[0] == null || args[0].isBlank()) {
            throw new IllegalArgumentException("Expected no arguments, --backfill or one reference date");
        }

        LocalDate utcToday = now.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate earliestCompleteDate = utcToday.minusDays(sourceRetentionDays).plusDays(1);
        if ("--backfill".equals(args[0])) {
            List<LocalDate> referenceDates = new ArrayList<>();
            for (LocalDate date = earliestCompleteDate; date.isBefore(utcToday); date = date.plusDays(1)) {
                referenceDates.add(date);
            }
            return new RunOptions(true, referenceDates);
        }

        LocalDate referenceDate;
        try {
            referenceDate = LocalDate.parse(args[0]);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Reference date must use yyyy-MM-dd format", exception);
        }
        if (!referenceDate.isBefore(utcToday)) {
            throw new IllegalArgumentException("Reference date must be before today in UTC");
        }
        if (referenceDate.isBefore(earliestCompleteDate)) {
            throw new IllegalArgumentException("Reference date is outside the complete source retention window");
        }
        return new RunOptions(true, List.of(referenceDate));
    }
}
