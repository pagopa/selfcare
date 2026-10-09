package it.pagopa.selfcare.logavailability.service;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.logavailability.client.AzureMonitorHttpClient;
import it.pagopa.selfcare.logavailability.client.AzureMonitorLogsClient;
import it.pagopa.selfcare.logavailability.client.AzureTableAvailabilityWriter;
import it.pagopa.selfcare.logavailability.client.LogAnalyticsAvailabilityWriter;
import it.pagopa.selfcare.logavailability.model.DailyAvailabilityRecord;
import it.pagopa.selfcare.logavailability.model.RequestCounts;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvailabilityRunServiceTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-10-07");

    @Test
    void writesTheSameDailyRecordToBothDestinationsInOrder() {
        List<String> calls = new ArrayList<>();
        StubLogsClient logs = new StubLogsClient(calls, null);
        StubTableWriter table = new StubTableWriter(calls, null);
        StubReportingWriter reporting = new StubReportingWriter(calls, null);
        AvailabilityRunService service = createService(logs, table, reporting);

        service.run("DEV", REFERENCE_DATE);

        assertEquals(List.of("query", "table", "reporting"), calls);
        assertEquals(REFERENCE_DATE, table.record.referenceDate());
        assertEquals("DEV", table.record.environment());
        assertEquals(97, table.record.countLt500());
        assertEquals(3, table.record.countGte500());
        assertSame(table.record, reporting.record);
    }

    @Test
    void stopsBeforeWritingWhenTheSourceQueryFails() {
        List<String> calls = new ArrayList<>();
        IllegalStateException cause = new IllegalStateException("source unavailable");
        AvailabilityRunService service = createService(
                new StubLogsClient(calls, cause),
                new StubTableWriter(calls, null),
                new StubReportingWriter(calls, null));

        AvailabilityRunException exception = assertThrows(AvailabilityRunException.class,
                () -> service.run("DEV", REFERENCE_DATE));

        assertEquals(AvailabilityRunException.Stage.SOURCE_QUERY, exception.stage());
        assertSame(cause, exception.getCause());
        assertEquals(List.of("query"), calls);
    }

    @Test
    void stopsBeforeReportingWhenTableStorageWriteFails() {
        List<String> calls = new ArrayList<>();
        IllegalStateException cause = new IllegalStateException("table unavailable");
        AvailabilityRunService service = createService(
                new StubLogsClient(calls, null),
                new StubTableWriter(calls, cause),
                new StubReportingWriter(calls, null));

        AvailabilityRunException exception = assertThrows(AvailabilityRunException.class,
                () -> service.run("DEV", REFERENCE_DATE));

        assertEquals(AvailabilityRunException.Stage.TABLE_STORAGE_WRITE, exception.stage());
        assertSame(cause, exception.getCause());
        assertEquals(List.of("query", "table"), calls);
    }

    @Test
    void identifiesReportingWriteFailureAfterAuthoritativeTableWrite() {
        List<String> calls = new ArrayList<>();
        IllegalStateException cause = new IllegalStateException("ingestion unavailable");
        AvailabilityRunService service = createService(
                new StubLogsClient(calls, null),
                new StubTableWriter(calls, null),
                new StubReportingWriter(calls, cause));

        AvailabilityRunException exception = assertThrows(AvailabilityRunException.class,
                () -> service.run("DEV", REFERENCE_DATE));

        assertEquals(AvailabilityRunException.Stage.LOG_ANALYTICS_WRITE, exception.stage());
        assertSame(cause, exception.getCause());
        assertEquals(List.of("query", "table", "reporting"), calls);
    }

    @Test
    void rejectsExecutionForAnEnvironmentOtherThanTheDeployedOne() {
        List<String> calls = new ArrayList<>();
        AvailabilityRunService service = createService(
                new StubLogsClient(calls, null),
                new StubTableWriter(calls, null),
                new StubReportingWriter(calls, null));

        assertThrows(IllegalArgumentException.class, () -> service.run("PROD", REFERENCE_DATE));
        assertEquals(List.of(), calls);
    }

    private static AvailabilityRunService createService(
            AzureMonitorLogsClient logs,
            AzureTableAvailabilityWriter table,
            LogAnalyticsAvailabilityWriter reporting) {
        AvailabilityRunService service = new AvailabilityRunService();
        service.logsClient = logs;
        service.tableWriter = table;
        service.reportingWriter = reporting;
        service.environment = "DEV";
        return service;
    }

    private static class StubLogsClient extends AzureMonitorLogsClient {
        private final List<String> calls;
        private final RuntimeException failure;

        private StubLogsClient(List<String> calls, RuntimeException failure) {
            super(new StubHttpClient(), new ObjectMapper(),
                    "01234567-89ab-cdef-0123-456789abcdef",
                    "/subscriptions/01234567-89ab-cdef-0123-456789abcdef/resourceGroups/rg/providers/Microsoft.Network/applicationGateways/gw");
            this.calls = calls;
            this.failure = failure;
        }

        @Override
        public RequestCounts countRequests(LocalDate referenceDate) {
            calls.add("query");
            if (failure != null) {
                throw failure;
            }
            return new RequestCounts(97, 3);
        }
    }

    private static class StubTableWriter extends AzureTableAvailabilityWriter {
        private final List<String> calls;
        private final RuntimeException failure;
        private DailyAvailabilityRecord record;

        private StubTableWriter(List<String> calls, RuntimeException failure) {
            super(testCredential(), "availabilitytest");
            this.calls = calls;
            this.failure = failure;
        }

        @Override
        public void upsert(DailyAvailabilityRecord record) {
            calls.add("table");
            this.record = record;
            if (failure != null) {
                throw failure;
            }
        }
    }

    private static class StubReportingWriter extends LogAnalyticsAvailabilityWriter {
        private final List<String> calls;
        private final RuntimeException failure;
        private DailyAvailabilityRecord record;

        private StubReportingWriter(List<String> calls, RuntimeException failure) {
            super(new StubHttpClient(), "https://ingestion.example.test", "dcr-id");
            this.calls = calls;
            this.failure = failure;
        }

        @Override
        public void ingest(DailyAvailabilityRecord record) {
            calls.add("reporting");
            this.record = record;
            if (failure != null) {
                throw failure;
            }
        }
    }

    private static class StubHttpClient extends AzureMonitorHttpClient {
        private StubHttpClient() {
            super(testCredential(), new ObjectMapper());
        }
    }

    private static TokenCredential testCredential() {
        return (TokenRequestContext ignored) -> Mono.just(
                new AccessToken("test-token", OffsetDateTime.now().plusHours(1)));
    }
}
