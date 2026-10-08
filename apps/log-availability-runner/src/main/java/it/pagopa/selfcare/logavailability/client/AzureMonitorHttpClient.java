package it.pagopa.selfcare.logavailability.client;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.credential.AccessToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class AzureMonitorHttpClient {

    private static final int MAX_ATTEMPTS = 3;

    private final TokenCredential credential;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Inject
    public AzureMonitorHttpClient(TokenCredential credential, ObjectMapper objectMapper) {
        this.credential = credential;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String post(URI uri, String scope, Object payload) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Azure Monitor endpoints must use HTTPS");
        }
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize Azure Monitor request", exception);
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            AccessToken accessToken = credential.getToken(
                    new TokenRequestContext().addScopes(scope)).block();
            if (accessToken == null) {
                throw new IllegalStateException("Managed Identity returned no access token");
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMinutes(2))
                    .header("Authorization", "Bearer " + accessToken.getToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            try {
                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return response.body();
                }
                if (!isRetryable(status) || attempt == MAX_ATTEMPTS) {
                    throw new AzureRequestException(status);
                }
                waitBeforeRetry(response.headers().firstValue("Retry-After").orElse(null), attempt);
            } catch (IOException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new IllegalStateException("Azure Monitor request failed", exception);
                }
                waitBeforeRetry(null, attempt);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Azure Monitor request was interrupted", exception);
            }
        }
        throw new IllegalStateException("Azure Monitor request exhausted its retry attempts");
    }

    private static boolean isRetryable(int status) {
        return status == 429 || status >= 500;
    }

    private static void waitBeforeRetry(String retryAfter, int attempt) {
        long seconds = attempt * 2L;
        if (retryAfter != null) {
            try {
                seconds = Math.max(seconds, Long.parseLong(retryAfter));
            } catch (NumberFormatException ignored) {
                // Fall back to the bounded local backoff.
            }
        }
        try {
            Thread.sleep(Math.min(seconds, 30) * 1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Azure Monitor retry was interrupted", exception);
        }
    }

    public static class AzureRequestException extends RuntimeException {
        private final int statusCode;

        AzureRequestException(int statusCode) {
            super("Azure Monitor request returned HTTP " + statusCode);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }
}
