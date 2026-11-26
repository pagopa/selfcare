package it.pagopa.selfcare.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.arc.Arc;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = OneMailHttpTest.OneMailStub.class, restrictToAnnotatedClass = true)
class OneMailHttpTest {
  @Inject MailService mailService;
  @Inject ObjectMapper mapper;

  @Test
  void sendsAndRetriesWithTheCorrectTenantCredentialsWithoutARequestContext() throws Exception {
    var ar = sendFromWorker("AR");
    var pnpg = sendFromWorker("PNPG");
    CompletableFuture.allOf(ar, pnpg).get(10, TimeUnit.SECONDS);

    Map<String, Integer> attempts = new HashMap<>();
    for (int i = 0; i < 4; i++) {
      var request = OneMailStub.REQUESTS.poll(1, TimeUnit.SECONDS);
      assertNotNull(request, "Expected two HTTP attempts per tenant");
      assertEquals("POST", request.method());
      assertEquals("/v1/emails/send/high?dryRun=false", request.target());
      var body = mapper.readTree(request.body());
      String tenant = body.at("/templateContent/templateAttributes/tenant").asText();
      assertEquals(List.of("test-" + tenant + "-mail-key"), request.keys());
      assertEquals(tenant + "@example.test", body.at("/from/email").asText());
      assertEquals(tenant + "-template", body.at("/templateContent/templateId").asText());
      attempts.merge(tenant, 1, Integer::sum);
    }
    assertEquals(Map.of("AR", 2, "PNPG", 2), attempts);
    assertEquals(0, OneMailStub.REQUESTS.size());
  }

  private CompletableFuture<Void> sendFromWorker(String tenant) {
    return CompletableFuture.runAsync(() -> {
      assertFalse(Arc.container().requestContext().isActive());
      mailService.sendOneMail(tenant, tenant + "-user", "recipient@example.test",
          tenant + "-template", Map.of("tenant", tenant))
          .await().atMost(Duration.ofSeconds(8));
      assertFalse(Arc.container().requestContext().isActive());
    });
  }

  public static class OneMailStub implements QuarkusTestResourceLifecycleManager {
    static final BlockingQueue<Request> REQUESTS = new LinkedBlockingQueue<>();
    private HttpServer server;

    @Override
    public Map<String, String> start() {
      try {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      } catch (IOException failure) {
        throw new IllegalStateException("Cannot start OneMail stub", failure);
      }
      var attempts = new ConcurrentHashMap<List<String>, Integer>();
      server.createContext("/v1/emails/send/high", exchange -> {
        try (exchange) {
          var keys = List.copyOf(exchange.getRequestHeaders().getOrDefault("x-api-key", List.of()));
          REQUESTS.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
              keys, exchange.getRequestBody().readAllBytes()));
          boolean firstAttempt = attempts.merge(keys, 1, Integer::sum) == 1;
          var response = (firstAttempt ? "{}" : "{\"requestId\":\"accepted\"}")
              .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(firstAttempt ? 500 : 202, response.length);
          exchange.getResponseBody().write(response);
        }
      });
      server.start();
      return Map.ofEntries(
          Map.entry("quarkus.rest-client.one-mail.url", "http://127.0.0.1:" + server.getAddress().getPort()),
          Map.entry("quarkus.log.category.\"org.jboss.resteasy.reactive.client.logging\".level", "DEBUG"),
          Map.entry("tenant.ar.mail.enabled", "true"),
          Map.entry("tenant.pnpg.mail.enabled", "true"),
          Map.entry("tenant.ar.one-mail.api-key", "test-AR-mail-key"),
          Map.entry("tenant.pnpg.one-mail.api-key", "test-PNPG-mail-key"),
          Map.entry("tenant.ar.mail-sender", "AR@example.test"),
          Map.entry("tenant.pnpg.mail-sender", "PNPG@example.test"),
          Map.entry("user-ms.retry.min-backoff", "1"),
          Map.entry("user-ms.retry.max-backoff", "1"),
          Map.entry("user-ms.retry", "1"));
    }

    @Override
    public void stop() {
      if (server != null) {
        server.stop(0);
      }
      REQUESTS.clear();
    }
  }

  record Request(String method, String target, List<String> keys, byte[] body) {}
}
