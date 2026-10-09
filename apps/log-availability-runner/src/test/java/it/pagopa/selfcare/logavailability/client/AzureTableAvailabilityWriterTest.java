package it.pagopa.selfcare.logavailability.client;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.logavailability.model.DailyAvailabilityRecord;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AzureTableAvailabilityWriterTest {

    @Test
    void rejectsInvalidStorageAccountNames() {
        TokenCredential credential = credential();

        assertThrows(IllegalArgumentException.class,
                () -> new AzureTableAvailabilityWriter(credential, "Invalid-Name"));
        assertThrows(IllegalArgumentException.class,
                () -> new AzureTableAvailabilityWriter(credential, "ab"));
        assertThrows(IllegalArgumentException.class,
                () -> new AzureTableAvailabilityWriter(credential, null, null));
    }

    @Test
    void replacesTheDailyEntityInTheAvailabilityTable() throws Exception {
        List<HttpRequest> requests = new ArrayList<>();
        AzureTableAvailabilityWriter writer = new AzureTableAvailabilityWriter(
                credential(), "selcdweusynthmon", request -> {
                    requests.add(request);
                    return Mono.just(new NoContentResponse(request));
                });
        DailyAvailabilityRecord record = DailyAvailabilityRecord.fromCounts(
                LocalDate.parse("2026-10-07"), "DEV", 97, 3, Instant.parse("2026-10-08T01:00:00Z"));

        writer.upsert(record);

        assertEquals(1, requests.size());
        HttpRequest request = requests.get(0);
        assertEquals(HttpMethod.PUT, request.getHttpMethod());
        assertEquals("selcdweusynthmon.table.core.windows.net", request.getUrl().getHost());
        String path = URLDecoder.decode(request.getUrl().getPath(), StandardCharsets.UTF_8);
        assertEquals("/SelcAvailability(PartitionKey='2026',RowKey='2026-10-07')", path);
        assertTrue(request.getHeaders().getValue("Authorization").startsWith("Bearer "));

        JsonNode body = new ObjectMapper().readTree(request.getBodyAsBinaryData().toString());
        assertEquals("2026-10-07", body.get("ReferenceDate").asText());
        assertEquals("DEV", body.get("Environment").asText());
        assertEquals(97, body.get("CountLt500").asLong());
        assertEquals(3, body.get("CountGte500").asLong());
        assertEquals(100, body.get("Total").asLong());
        assertEquals(97.0, body.get("Availability").asDouble());
        assertTrue(body.get("GenerationTimestamp").asText().startsWith("2026-10-08T01:00:00"));
    }

    private static TokenCredential credential() {
        return (TokenRequestContext ignored) -> Mono.just(
                new AccessToken("test-token", OffsetDateTime.now().plusHours(1)));
    }

    private static class NoContentResponse extends HttpResponse {
        private final HttpHeaders headers = new HttpHeaders();

        NoContentResponse(HttpRequest request) {
            super(request);
        }

        @Override
        public int getStatusCode() {
            return 204;
        }

        @Override
        @Deprecated
        public String getHeaderValue(String name) {
            return headers.getValue(name);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public Flux<ByteBuffer> getBody() {
            return Flux.empty();
        }

        @Override
        public Mono<byte[]> getBodyAsByteArray() {
            return Mono.empty();
        }

        @Override
        public Mono<String> getBodyAsString() {
            return Mono.empty();
        }

        @Override
        public Mono<String> getBodyAsString(Charset charset) {
            return Mono.empty();
        }
    }
}
