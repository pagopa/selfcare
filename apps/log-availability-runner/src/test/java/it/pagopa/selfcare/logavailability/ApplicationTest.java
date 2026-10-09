package it.pagopa.selfcare.logavailability;

import it.pagopa.selfcare.logavailability.service.AvailabilityRunException;
import it.pagopa.selfcare.logavailability.service.AvailabilityRunService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationTest {

    private static final LocalDate YESTERDAY = LocalDate.now(ZoneOffset.UTC).minusDays(1);

    @Test
    void runsTheRequestedReferenceDate() {
        StubRunService service = new StubRunService(null);

        assertEquals(0, application(service, 90).run(YESTERDAY.toString()));
        assertEquals(List.of("DEV " + YESTERDAY), service.calls);
    }

    @Test
    void backfillsEveryCompleteDayInTheRetentionWindow() {
        StubRunService service = new StubRunService(null);

        assertEquals(0, application(service, 3).run("--backfill"));
        assertEquals(List.of("DEV " + YESTERDAY.minusDays(1), "DEV " + YESTERDAY), service.calls);
    }

    @Test
    void rejectsInvalidArgumentsWithoutRunning() {
        StubRunService service = new StubRunService(null);

        assertEquals(2, application(service, 90).run("not-a-date"));
        assertEquals(2, application(service, 1).run(YESTERDAY.toString()));
        assertEquals(List.of(), service.calls);
    }

    @Test
    void scheduledTriggerEitherRunsYesterdayOrSkips() {
        StubRunService service = new StubRunService(null);

        assertEquals(0, application(service, 90).run());
        assertTrue(service.calls.isEmpty() || service.calls.equals(List.of("DEV " + YESTERDAY)));
    }

    @Test
    void stopsAtTheFirstFailedStage() {
        StubRunService service = new StubRunService(new AvailabilityRunException(
                AvailabilityRunException.Stage.TABLE_STORAGE_WRITE, new IllegalStateException("down")));

        assertEquals(1, application(service, 3).run("--backfill"));
        assertEquals(1, service.calls.size());
    }

    @Test
    void failsOnUnexpectedErrors() {
        StubRunService service = new StubRunService(new IllegalArgumentException("wrong environment"));

        assertEquals(1, application(service, 90).run(YESTERDAY.toString()));
    }

    private static Application application(AvailabilityRunService service, int retentionDays) {
        Application application = new Application();
        application.availabilityRunService = service;
        application.environment = "DEV";
        application.sourceRetentionDays = retentionDays;
        return application;
    }

    private static class StubRunService extends AvailabilityRunService {
        private final RuntimeException failure;
        private final List<String> calls = new ArrayList<>();

        private StubRunService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void run(String selectedEnvironment, LocalDate referenceDate) {
            calls.add(selectedEnvironment + " " + referenceDate);
            if (failure != null) {
                throw failure;
            }
        }
    }
}
