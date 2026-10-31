package it.pagopa.selfcare.m2m;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;

class MachineTokenClientTest {
  private HttpServer server;
  private MachineTokenClient client;
  private TokenCredential credential;
  private Clock clock;
  private final AtomicInteger calls = new AtomicInteger();
  private final AtomicInteger status = new AtomicInteger(200);
  private final AtomicInteger lifetime = new AtomicInteger(300);
  private final ObjectMapper mapper = new ObjectMapper();
  private final Instant now = Instant.parse("2026-10-30T12:00:00Z");

  @BeforeEach
  void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/token/service", exchange -> {
      int number = calls.incrementAndGet();
      JsonNode request = mapper.readTree(exchange.getRequestBody());
      int responseStatus = "Bearer entra-token".equals(
          exchange.getRequestHeaders().getFirst("Authorization")) ? status.get() : 401;
      byte[] body = mapper.writeValueAsBytes(java.util.Map.of(
          "access_token", request.path("tenantId").asText() + "-" + number,
          "token_type", "Bearer", "expires_in", lifetime.get(), "scope", request.path("scope").asText()));
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(responseStatus, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    credential = mock(TokenCredential.class);
    when(credential.getToken(any())).thenReturn(
        Mono.just(new AccessToken("entra-token", OffsetDateTime.now().plusHours(1))));
    clock = mock(Clock.class);
    when(clock.instant()).thenReturn(now);
    client = new MachineTokenClient(credential, HttpClient.newHttpClient(),
        URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/token/service"),
        "api://auth/.default", clock);
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @Test
  void cachesAndCoalescesConcurrentRequestsWithoutMixingTenants() {
    var pending = new ArrayList<CompletableFuture<String>>();
    for (int i = 0; i < 10; i++) {
      pending.add(client.token("AR", "onboarding-functions", "onboarding-functions.invoke")
          .toCompletableFuture());
    }
    CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).join();
    assertTrue(pending.stream().allMatch(token -> token.join().equals("AR-1")));
    assertEquals("PNPG-2", token("PNPG"));
    assertEquals("AR-1", token("AR"));
    assertEquals(2, calls.get());
  }

  @Test
  void cacheSeparatesAudiencesAndPermissions() {
    assertEquals("AR-1", token("AR"));
    client.token("AR", "another-service", "another.invoke").toCompletableFuture().join();
    client.token("AR", "onboarding-functions", "another.permission").toCompletableFuture().join();
    assertEquals(3, calls.get());
    assertEquals("AR-1", token("AR"));
  }

  @ParameterizedTest
  @ValueSource(ints = {300, 900})
  void renewsAtFourMinutesWithoutExtendingCacheLifetimeOnReads(int tokenLifetime) {
    lifetime.set(tokenLifetime);
    assertEquals("AR-1", token("AR"));
    when(clock.instant()).thenReturn(now.plusSeconds(239));
    assertEquals("AR-1", token("AR"));
    assertEquals(1, calls.get());

    when(clock.instant()).thenReturn(now.plusSeconds(240));
    assertEquals("AR-2", token("AR"));
    when(clock.instant()).thenReturn(now.plusSeconds(479));
    assertEquals("AR-2", token("AR"));
    assertEquals(2, calls.get());
  }

  @Test
  void shorterTokenLifetimeTakesPrecedenceOverFourMinuteCache() {
    lifetime.set(120);
    assertEquals("AR-1", token("AR"));
    when(clock.instant()).thenReturn(now.plusSeconds(89));
    assertEquals("AR-1", token("AR"));
    when(clock.instant()).thenReturn(now.plusSeconds(90));
    assertEquals("AR-2", token("AR"));
  }

  @Test
  void failedRenewalDoesNotReturnOldTokenAndCanBeRetried() {
    token("AR");
    when(clock.instant()).thenReturn(now.plusSeconds(240));
    status.set(503);
    assertThrows(java.util.concurrent.CompletionException.class, () -> token("AR"));
    status.set(200);
    assertEquals("AR-3", token("AR"));
  }

  @Test
  void managedIdentityFailureDoesNotCallIssuerOrReturnFallback() {
    when(credential.getToken(any())).thenReturn(Mono.error(new IllegalStateException("Unavailable")));
    assertThrows(java.util.concurrent.CompletionException.class, () -> token("AR"));
    assertEquals(0, calls.get());
  }

  @Test
  void rejectsRedirectAndUnauthorizedResponses() {
    status.set(302);
    assertThrows(java.util.concurrent.CompletionException.class, () -> token("AR"));
    status.set(403);
    assertThrows(java.util.concurrent.CompletionException.class, () -> token("AR"));
  }

  @Test
  void refusesInsecureEndpointAndWorksWithoutConfigurationWhenDisabled() {
    assertThrows(IllegalArgumentException.class,
        () -> MachineTokenClient.requireHttps("http://auth/token/service"));
    assertThrows(IllegalArgumentException.class,
        () -> MachineTokenClient.requireHttps("https://user:password@auth/token/service"));
    MachineTokenClient disabled =
        new MachineTokenClient(false, Optional.empty(), Optional.empty(), Optional.empty());
    assertFalse(disabled.isEnabled());
    assertThrows(java.util.concurrent.CompletionException.class,
        () -> disabled.token("AR", "onboarding-functions", "onboarding-functions.invoke")
            .toCompletableFuture().join());
  }

  private String token(String tenant) {
    return client.token(tenant, "onboarding-functions", "onboarding-functions.invoke")
        .toCompletableFuture().join();
  }
}
