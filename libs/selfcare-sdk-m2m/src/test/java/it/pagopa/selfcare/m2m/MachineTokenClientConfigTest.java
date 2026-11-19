package it.pagopa.selfcare.m2m;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(MachineTokenClientConfigTest.CustomTenants.class)
class MachineTokenClientConfigTest {
  @Inject MachineTokenClient client;

  @Test
  void injectsConfiguredTenantList() {
    assertEquals("TENANT_3", client.token("TENANT_3").toCompletableFuture().join());
    assertThrows(CompletionException.class,
        () -> client.token("AR").toCompletableFuture().join());
  }

  @AfterAll
  static void stopServer() {
    if (CustomTenants.server != null) {
      CustomTenants.server.stop(0);
    }
  }

  public static class CustomTenants implements QuarkusTestProfile {
    static HttpServer server;

    @Override
    public Map<String, String> getConfigOverrides() {
      try {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token/service", exchange -> {
          var mapper = new ObjectMapper();
          var request = mapper.readTree(exchange.getRequestBody());
          byte[] response = mapper.writeValueAsBytes(Map.of(
              "access_token", request.path("tenantId").asText(),
              "token_type", "Bearer", "expires_in", 300));
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
        server.start();
        return Map.of(
            "selfcare.m2m.enabled", "true",
            "selfcare.m2m.endpoint", "http://127.0.0.1:" + server.getAddress().getPort() + "/token/service",
            "selfcare.m2m.allow-insecure-http", "true",
            "selfcare.m2m.allowed-tenants", "TENANT_3");
      } catch (IOException exception) {
        throw new IllegalStateException("Cannot configure M2M client test", exception);
      }
    }
  }
}
