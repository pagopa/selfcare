package it.pagopa.selfcare.logavailability.client;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.logavailability.model.RequestCounts;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AzureMonitorLogsClientTest {

    private static final String WORKSPACE_ID = "01234567-89ab-cdef-0123-456789abcdef";
    private static final String APPLICATION_GATEWAY_ID =
            "/subscriptions/01234567-89ab-cdef-0123-456789abcdef/resourceGroups/monitor-rg/providers/Microsoft.Network/applicationGateways/gateway";
    private static final String SUCCESS_RESPONSE = """
            {
              "tables": [{
                "columns": [
                  {"name": "CountGte500", "type": "long"},
                  {"name": "CountLt500", "type": "long"}
                ],
                "rows": [[3, 97]]
              }]
            }
            """;

    @Test
    void parsesCountsByColumnNameAndSendsUtcQueryForTheWorkspace() {
        StubHttpClient httpClient = new StubHttpClient(SUCCESS_RESPONSE);
        AzureMonitorLogsClient client = new AzureMonitorLogsClient(
                httpClient, new ObjectMapper(), WORKSPACE_ID, APPLICATION_GATEWAY_ID);

        RequestCounts counts = client.countRequests(LocalDate.parse("2026-10-07"));

        assertEquals(new RequestCounts(97, 3), counts);
        assertEquals(URI.create(
                "https://api.loganalytics.azure.com/v1/workspaces/" + WORKSPACE_ID + "/query"),
                httpClient.uri);
        assertEquals("https://api.loganalytics.io/.default", httpClient.scope);
        assertEquals("2026-10-07T00:00:00Z/2026-10-08T00:00:00Z",
                httpClient.payload.get("timespan"));
        assertTrue(((String) httpClient.payload.get("query")).contains(
                "datetime(2026-10-07T00:00:00Z)"));
    }

    @Test
    void rejectsPartialQueryErrorsEvenWhenAzureReturnsAnHttpSuccess() {
        StubHttpClient httpClient = new StubHttpClient("""
                {
                  "error": {"code": "PartialError", "message": "Some results may be missing"},
                  "tables": [{
                    "columns": [{"name": "CountLt500"}, {"name": "CountGte500"}],
                    "rows": [[97, 3]]
                  }]
                }
                """);
        AzureMonitorLogsClient client = createClient(httpClient);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.countRequests(LocalDate.parse("2026-10-07")));

        assertEquals("Azure Monitor query returned an error", exception.getMessage());
    }

    @Test
    void rejectsMissingOrMalformedCountResults() {
        assertThrows(IllegalStateException.class,
                () -> createClient(new StubHttpClient("""
                        {"tables":[{"columns":[{"name":"CountLt500"}],"rows":[[10]]}]}
                        """)).countRequests(LocalDate.parse("2026-10-07")));
        assertThrows(IllegalStateException.class,
                () -> createClient(new StubHttpClient("""
                        {"tables":[{"columns":[{"name":"CountLt500"},{"name":"CountGte500"}],"rows":[[10,-1]]}]}
                        """)).countRequests(LocalDate.parse("2026-10-07")));
        assertThrows(IllegalStateException.class,
                () -> createClient(new StubHttpClient("""
                        {"tables":[{"columns":[{"name":"CountLt500"},{"name":"CountGte500"}],"rows":[[10.5,1]]}]}
                        """)).countRequests(LocalDate.parse("2026-10-07")));
    }

    private static AzureMonitorLogsClient createClient(StubHttpClient httpClient) {
        return new AzureMonitorLogsClient(
                httpClient, new ObjectMapper(), WORKSPACE_ID, APPLICATION_GATEWAY_ID);
    }

    private static class StubHttpClient extends AzureMonitorHttpClient {
        private final String response;
        private URI uri;
        private String scope;
        private Map<?, ?> payload;

        private StubHttpClient(String response) {
            super(testCredential(), new ObjectMapper());
            this.response = response;
        }

        @Override
        public String post(URI uri, String scope, Object payload) {
            this.uri = uri;
            this.scope = scope;
            this.payload = (Map<?, ?>) payload;
            return response;
        }
    }

    private static TokenCredential testCredential() {
        return (TokenRequestContext ignored) -> Mono.just(
                new AccessToken("test-token", OffsetDateTime.now().plusHours(1)));
    }
}
