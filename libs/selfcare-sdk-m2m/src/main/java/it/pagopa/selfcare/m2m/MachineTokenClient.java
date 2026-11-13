package it.pagopa.selfcare.m2m;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MachineTokenClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(MachineTokenClient.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final Duration CACHE_TTL = Duration.ofMinutes(4);
  private static final Duration REFRESH_MARGIN = Duration.ofSeconds(30);
  private final boolean enabled;
  private final HttpClient http;
  private final URI endpoint;
  private final Clock clock;
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<Request, CompletableFuture<CachedToken>> cache = new HashMap<>();

  @Inject
  public MachineTokenClient(
      @ConfigProperty(name = "selfcare.m2m.enabled", defaultValue = "false") boolean enabled,
      @ConfigProperty(name = "selfcare.m2m.endpoint") Optional<String> endpoint,
      @ConfigProperty(name = "selfcare.m2m.allow-insecure-http", defaultValue = "false")
          boolean allowInsecureHttp) {
    this.enabled = enabled;
    this.clock = Clock.systemUTC();
    if (enabled) {
      this.endpoint = requireEndpoint(endpoint.orElseThrow(
          () -> new IllegalStateException("M2M endpoint is required")), allowInsecureHttp);
      if ("http".equals(this.endpoint.getScheme())) {
        LOGGER.warn("event=machine_client_insecure_http_enabled");
      }
      this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT)
          .followRedirects(HttpClient.Redirect.NEVER).build();
    } else {
      this.endpoint = null;
      this.http = null;
    }
  }

  public MachineTokenClient(boolean enabled, Optional<String> endpoint) {
    this(enabled, endpoint, false);
  }

  MachineTokenClient(HttpClient http, URI endpoint, Clock clock) {
    this.enabled = true;
    this.http = http;
    this.endpoint = endpoint;
    this.clock = clock;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public synchronized CompletionStage<String> token(String tenant, String audience, String scope) {
    if (!enabled) {
      return CompletableFuture.failedFuture(new IllegalStateException("M2M client is disabled"));
    }
    if (tenant == null || !tenant.matches("AR|PNPG")
        || audience == null || !audience.matches("[a-z][a-z0-9-]{0,63}")
        || scope == null || !scope.matches("[a-z][a-z0-9.:-]{0,127}")) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid M2M request"));
    }
    Request request = new Request(tenant, audience, scope);
    CompletableFuture<CachedToken> current = cache.get(request);
    if (current != null && !current.isCompletedExceptionally() && !current.isCancelled()) {
      if (!current.isDone() || current.join().refreshAt().isAfter(clock.instant())) {
        return current.thenApply(CachedToken::value);
      }
    }
    CompletableFuture<CachedToken> acquired = acquire(request);
    cache.put(request, acquired);
    return acquired.thenApply(CachedToken::value);
  }

  private CompletableFuture<CachedToken> acquire(Request request) {
    final String body;
    try {
      body = mapper.writeValueAsString(request);
    } catch (JsonProcessingException exception) {
      return CompletableFuture.failedFuture(new IllegalStateException("Cannot encode M2M request"));
    }
    Instant started = clock.instant();
    return http.sendAsync(HttpRequest.newBuilder(endpoint)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> decode(response.statusCode(), response.body(), request.scope(), started))
        .whenComplete((token, failure) -> {
          if (failure != null) {
            LOGGER.warn("event=machine_token_acquisition_failed tenant={} audience={}",
                request.tenantId(), request.audience());
          }
        });
  }

  private CachedToken decode(int status, String body, String scope, Instant started) {
    if (status != 200) {
      throw new IllegalStateException("M2M token endpoint returned HTTP " + status);
    }
    if (body == null || body.length() > 16384) {
      throw new IllegalStateException("Invalid M2M token response");
    }
    try {
      JsonNode json = mapper.readTree(body);
      long lifetime = json.path("expires_in").asLong();
      String value = json.path("access_token").asText();
      if (!"Bearer".equals(json.path("token_type").asText()) || value.isBlank()
          || !scope.equals(json.path("scope").asText()) || lifetime < 60 || lifetime > 900) {
        throw new IllegalStateException("Invalid M2M token response");
      }
      long cacheSeconds = Math.min(CACHE_TTL.toSeconds(), lifetime - REFRESH_MARGIN.toSeconds());
      return new CachedToken(value, started.plusSeconds(cacheSeconds));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Invalid M2M token response");
    }
  }

  static URI requireHttps(String value) {
    return requireEndpoint(value, false);
  }

  private static URI requireEndpoint(String value, boolean allowInsecureHttp) {
    URI uri = URI.create(value);
    boolean allowedScheme = "https".equals(uri.getScheme())
        || (allowInsecureHttp && "http".equals(uri.getScheme()));
    if (!allowedScheme || uri.getHost() == null
        || uri.getUserInfo() != null || uri.getFragment() != null) {
      throw new IllegalArgumentException(
          "M2M endpoint must be an absolute HTTPS URI; HTTP requires explicit opt-in");
    }
    return uri;
  }

  private record Request(String tenantId, String audience, String scope) {}
  private record CachedToken(String value, Instant refreshAt) {}
}
