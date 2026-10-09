package it.pagopa.selfcare.logavailability.client;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.logavailability.model.DailyAvailabilityRecord;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogAnalyticsAvailabilityWriterTest {

    @Test
    void ingestsTheDailyRecordToTheExpectedDcrStream() {
        StubHttpClient httpClient = new StubHttpClient();
        LogAnalyticsAvailabilityWriter writer = new LogAnalyticsAvailabilityWriter(
                httpClient, "https://ingestion.example.test/", "dcr-immutable-id");
        DailyAvailabilityRecord record = DailyAvailabilityRecord.fromCounts(
                LocalDate.parse("2026-10-07"),
                "PROD",
                999,
                1,
                Instant.parse("2026-10-08T01:00:00Z"));

        writer.ingest(record);

        assertEquals(URI.create("https://ingestion.example.test/dataCollectionRules/dcr-immutable-id"
                + "/streams/Custom-SelcAvailability_CL?api-version=2023-01-01"), httpClient.uri);
        assertEquals("https://monitor.azure.com/.default", httpClient.scope);
        assertEquals(1, httpClient.payload.size());
        Map<?, ?> row = (Map<?, ?>) httpClient.payload.get(0);
        assertEquals("2026-10-08T01:00:00Z", row.get("TimeGenerated"));
        assertEquals("2026-10-07T00:00:00Z", row.get("ReferenceDate"));
        assertEquals("PROD", row.get("Environment"));
        assertEquals(999L, row.get("CountLt500"));
        assertEquals(1L, row.get("CountGte500"));
        assertEquals(1000L, row.get("Total"));
        assertEquals(record.availability(), row.get("Availability"));
        assertEquals("2026-10-08T01:00:00Z", row.get("GenerationTimestamp"));
    }

    private static class StubHttpClient extends AzureMonitorHttpClient {
        private URI uri;
        private String scope;
        private List<?> payload;

        private StubHttpClient() {
            super(testCredential(), new ObjectMapper());
        }

        @Override
        public String post(URI uri, String scope, Object payload) {
            this.uri = uri;
            this.scope = scope;
            this.payload = (List<?>) payload;
            return "";
        }
    }

    private static TokenCredential testCredential() {
        return (TokenRequestContext ignored) -> Mono.just(
                new AccessToken("test-token", OffsetDateTime.now().plusHours(1)));
    }
}
