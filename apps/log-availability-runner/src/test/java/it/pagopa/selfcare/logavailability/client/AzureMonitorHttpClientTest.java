package it.pagopa.selfcare.logavailability.client;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AzureMonitorHttpClientTest {

    private static final URI ENDPOINT = URI.create("https://api.loganalytics.azure.com/v1/workspaces/id/query");
    private static final String SCOPE = "https://api.loganalytics.io/.default";

    private final List<Long> sleeps = new ArrayList<>();

    @Test
    void postsJsonWithBearerTokenAndReturnsTheBodyOnSuccess() {
        StubHttpClient http = new StubHttpClient().respond(200, "{\"ok\":true}", null);
        List<String> scopes = new ArrayList<>();

        String body = client(http, scopes).post(ENDPOINT, SCOPE, Map.of("query", "Q"));

        assertEquals("{\"ok\":true}", body);
        assertEquals(List.of(SCOPE), scopes);
        HttpRequest request = http.requests.get(0);
        assertEquals("POST", request.method());
        assertEquals(ENDPOINT, request.uri());
        assertEquals(Optional.of("Bearer test-token"), request.headers().firstValue("Authorization"));
        assertEquals(Optional.of("application/json"), request.headers().firstValue("Content-Type"));
        assertEquals(List.of(), sleeps);
    }

    @Test
    void rejectsNonHttpsEndpoints() {
        StubHttpClient http = new StubHttpClient();

        assertThrows(IllegalArgumentException.class,
                () -> client(http, new ArrayList<>()).post(URI.create("http://example.test"), SCOPE, Map.of()));
        assertEquals(List.of(), http.requests);
    }

    @Test
    void failsWhenThePayloadCannotBeSerialized() {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {
                };
            }
        };
        AzureMonitorHttpClient client = new AzureMonitorHttpClient(
                credential(new ArrayList<>()), failingMapper, new StubHttpClient(), sleeps::add);

        assertThrows(IllegalStateException.class, () -> client.post(ENDPOINT, SCOPE, Map.of()));
    }

    @Test
    void failsWhenManagedIdentityReturnsNoToken() {
        AzureMonitorHttpClient client = new AzureMonitorHttpClient(
                ignored -> Mono.empty(), new ObjectMapper(), new StubHttpClient(), sleeps::add);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.post(ENDPOINT, SCOPE, Map.of()));
        assertEquals("Managed Identity returned no access token", exception.getMessage());
    }

    @Test
    void doesNotRetryClientErrors() {
        StubHttpClient http = new StubHttpClient().respond(403, "forbidden", null);

        AzureMonitorHttpClient.AzureRequestException exception = assertThrows(
                AzureMonitorHttpClient.AzureRequestException.class,
                () -> client(http, new ArrayList<>()).post(ENDPOINT, SCOPE, Map.of()));

        assertEquals(403, exception.statusCode());
        assertEquals(1, http.requests.size());
        assertEquals(List.of(), sleeps);
    }

    @Test
    void retriesThrottlingHonouringRetryAfterThenSucceeds() {
        StubHttpClient http = new StubHttpClient()
                .respond(429, "", "10")
                .respond(503, "", "not-a-number")
                .respond(200, "done", null);

        String body = client(http, new ArrayList<>()).post(ENDPOINT, SCOPE, Map.of());

        assertEquals("done", body);
        assertEquals(3, http.requests.size());
        assertEquals(List.of(10_000L, 4_000L), sleeps);
    }

    @Test
    void capsRetryAfterAndFailsWhenServerErrorsPersist() {
        StubHttpClient http = new StubHttpClient()
                .respond(500, "", "120")
                .respond(500, "", null)
                .respond(500, "", null);

        AzureMonitorHttpClient.AzureRequestException exception = assertThrows(
                AzureMonitorHttpClient.AzureRequestException.class,
                () -> client(http, new ArrayList<>()).post(ENDPOINT, SCOPE, Map.of()));

        assertEquals(500, exception.statusCode());
        assertEquals(List.of(30_000L, 4_000L), sleeps);
    }

    @Test
    void retriesIoErrorsAndFailsAfterTheLastAttempt() {
        StubHttpClient http = new StubHttpClient()
                .fail(new IOException("reset"))
                .respond(200, "recovered", null);

        assertEquals("recovered", client(http, new ArrayList<>()).post(ENDPOINT, SCOPE, Map.of()));

        StubHttpClient alwaysFailing = new StubHttpClient()
                .fail(new IOException("1"))
                .fail(new IOException("2"))
                .fail(new IOException("3"));
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client(alwaysFailing, new ArrayList<>()).post(ENDPOINT, SCOPE, Map.of()));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void stopsAndPreservesTheInterruptFlagWhenInterrupted() {
        StubHttpClient http = new StubHttpClient().fail(new InterruptedException());

        try {
            assertThrows(IllegalStateException.class,
                    () -> client(http, new ArrayList<>()).post(ENDPOINT, SCOPE, Map.of()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void stopsWhenTheBackoffIsInterrupted() {
        StubHttpClient http = new StubHttpClient().respond(503, "", null);
        AzureMonitorHttpClient client = new AzureMonitorHttpClient(
                credential(new ArrayList<>()), new ObjectMapper(), http,
                millis -> {
                    throw new InterruptedException();
                });

        try {
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> client.post(ENDPOINT, SCOPE, Map.of()));
            assertEquals("Azure Monitor retry was interrupted", exception.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private AzureMonitorHttpClient client(StubHttpClient http, List<String> scopes) {
        return new AzureMonitorHttpClient(credential(scopes), new ObjectMapper(), http, sleeps::add);
    }

    private static TokenCredential credential(List<String> scopes) {
        return (TokenRequestContext context) -> {
            scopes.addAll(context.getScopes());
            return Mono.just(new AccessToken("test-token", OffsetDateTime.now().plusHours(1)));
        };
    }

    private static class StubHttpClient extends HttpClient {
        private final Deque<Object> outcomes = new ArrayDeque<>();
        private final List<HttpRequest> requests = new ArrayList<>();

        StubHttpClient respond(int status, String body, String retryAfter) {
            outcomes.add(new StubResponse(status, body, retryAfter));
            return this;
        }

        StubHttpClient fail(Exception exception) {
            outcomes.add(exception);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException, InterruptedException {
            requests.add(request);
            Object outcome = outcomes.remove();
            if (outcome instanceof IOException exception) {
                throw exception;
            }
            if (outcome instanceof InterruptedException exception) {
                throw exception;
            }
            StubResponse response = (StubResponse) outcome;
            response.request = request;
            return (HttpResponse<T>) response;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    private static class StubResponse implements HttpResponse<String> {
        private final int status;
        private final String body;
        private final HttpHeaders headers;
        private HttpRequest request;

        StubResponse(int status, String body, String retryAfter) {
            this.status = status;
            this.body = body;
            this.headers = HttpHeaders.of(
                    retryAfter == null ? Map.of() : Map.of("Retry-After", List.of(retryAfter)),
                    (name, value) -> true);
        }

        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
