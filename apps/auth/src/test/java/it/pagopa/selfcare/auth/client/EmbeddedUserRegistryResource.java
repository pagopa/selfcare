package it.pagopa.selfcare.auth.client;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EmbeddedUserRegistryResource implements QuarkusTestResourceLifecycleManager {

  static final BlockingQueue<String> RECEIVED_KEYS = new LinkedBlockingQueue<>();

  private HttpServer server;

  @Override
  public Map<String, String> start() {
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot start User Registry test server", exception);
    }
    server.createContext(
        "/users",
        exchange -> {
          RECEIVED_KEYS.add(exchange.getRequestHeaders().getFirst("x-api-key"));
          byte[] response =
              "{\"id\":\"00000000-0000-0000-0000-000000000001\"}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          try (var body = exchange.getResponseBody()) {
            body.write(response);
          }
        });
    server.start();
    return Map.of(
        "quarkus.rest-client.\"tenant.user-registry\".url",
        "http://127.0.0.1:" + server.getAddress().getPort());
  }

  @Override
  public void stop() {
    if (server != null) {
      server.stop(0);
    }
    RECEIVED_KEYS.clear();
  }
}
