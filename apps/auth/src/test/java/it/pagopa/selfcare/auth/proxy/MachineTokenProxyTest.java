package it.pagopa.selfcare.auth.proxy;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MachineTokenProxyTest {
  private final HttpClient client = HttpClient.newHttpClient();
  private HttpServer upstream;
  private MachineTokenProxy proxy;

  @BeforeEach
  void setUp() throws Exception {
    upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    URI upstreamUri =
        URI.create("http://127.0.0.1:" + upstream.getAddress().getPort() + "/token/service");
    proxy = new MachineTokenProxy(new InetSocketAddress("127.0.0.1", 0), upstreamUri);
    upstream.start();
    proxy.start();
  }

  @AfterEach
  void tearDown() {
    proxy.close();
    upstream.stop(0);
  }

  @Test
  void forwardsRequestBytesHeadersAndUpstreamResponse() throws Exception {
    var seenBody = new AtomicReference<byte[]>();
    var seenType = new AtomicReference<String>();
    var seenTrace = new AtomicReference<String>();
    byte[] responseBody = "{\"access_token\":\"session\"}".getBytes(StandardCharsets.UTF_8);
    upstream.createContext(
        "/token/service",
        exchange -> {
          seenBody.set(exchange.getRequestBody().readAllBytes());
          seenType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
          seenTrace.set(exchange.getRequestHeaders().getFirst("traceparent"));
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.getResponseHeaders().set("Cache-Control", "no-store");
          exchange.getResponseHeaders().set("Pragma", "no-cache");
          exchange.sendResponseHeaders(201, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
    byte[] body =
        "{ \"tenantId\" : \"PNPG\", \"example\" : \"è\" }".getBytes(StandardCharsets.UTF_8);
    HttpResponse<byte[]> response =
        send(
            "POST",
            "/token/service",
            body,
            "application/json",
            "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");

    assertEquals(201, response.statusCode());
    assertArrayEquals(body, seenBody.get());
    assertEquals("application/json", seenType.get());
    assertEquals("00-0123456789abcdef0123456789abcdef-0123456789abcdef-01", seenTrace.get());
    assertArrayEquals(responseBody, response.body());
    assertEquals("application/json", response.headers().firstValue("Content-Type").orElseThrow());
    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
    assertEquals("no-cache", response.headers().firstValue("Pragma").orElseThrow());
  }

  @Test
  void rejectsOtherMethodsPathsQueriesAndOversizedBodies() throws Exception {
    var calls = new AtomicInteger();
    upstream.createContext(
        "/token/service",
        exchange -> {
          calls.incrementAndGet();
          exchange.getRequestBody().readAllBytes();
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    assertEquals(404, send("POST", "/other", new byte[0], null, null).statusCode());
    assertEquals(404, send("POST", "/token/service/other", new byte[0], null, null).statusCode());
    assertEquals(
        404, send("POST", "/token/service?tenantId=AR", new byte[0], null, null).statusCode());
    HttpResponse<byte[]> method = send("GET", "/token/service", new byte[0], null, null);
    assertEquals(405, method.statusCode());
    assertEquals("POST", method.headers().firstValue("Allow").orElseThrow());
    assertEquals(413, send("POST", "/token/service", new byte[8193], null, null).statusCode());
    assertEquals(0, calls.get());
    assertEquals(204, send("POST", "/token/service", new byte[8192], null, null).statusCode());
    assertEquals(1, calls.get());
  }

  @Test
  void preservesUpstreamErrorsAndDoesNotFollowRedirects() throws Exception {
    var redirected = new AtomicInteger();
    upstream.createContext(
        "/token/service",
        exchange -> {
          exchange.getRequestBody().readAllBytes();
          String type = exchange.getRequestHeaders().getFirst("Content-Type");
          if ("redirect".equals(type)) {
            exchange.getResponseHeaders().set("Location", "/elsewhere");
            exchange.sendResponseHeaders(302, -1);
          } else {
            byte[] body = "{\"detail\":\"invalid\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
            exchange.sendResponseHeaders(400, body.length);
            exchange.getResponseBody().write(body);
          }
          exchange.close();
        });
    upstream.createContext(
        "/elsewhere",
        exchange -> {
          redirected.incrementAndGet();
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });

    HttpResponse<byte[]> error = send("POST", "/token/service", new byte[0], "invalid", null);
    assertEquals(400, error.statusCode());
    assertEquals("{\"detail\":\"invalid\"}", new String(error.body(), StandardCharsets.UTF_8));
    assertEquals(
        "application/problem+json", error.headers().firstValue("Content-Type").orElseThrow());
    assertEquals("no-store", error.headers().firstValue("Cache-Control").orElseThrow());
    HttpResponse<byte[]> redirect = send("POST", "/token/service", new byte[0], "redirect", null);
    assertEquals(302, redirect.statusCode());
    assertEquals("/elsewhere", redirect.headers().firstValue("Location").orElseThrow());
    assertEquals(0, redirected.get());
  }

  @Test
  void reportsUnavailableAndOversizedUpstreamResponses() throws Exception {
    upstream.createContext(
        "/token/service",
        exchange -> {
          exchange.getRequestBody().readAllBytes();
          byte[] body = new byte[16385];
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    assertEquals(502, send("POST", "/token/service", new byte[0], null, null).statusCode());
    upstream.stop(0);
    assertEquals(502, send("POST", "/token/service", new byte[0], null, null).statusCode());
  }

  @Test
  void rejectsIncompleteUpstreamResponses() throws Exception {
    upstream.createContext(
        "/token/service",
        exchange -> {
          exchange.getRequestBody().readAllBytes();
          exchange.sendResponseHeaders(200, 20);
          exchange.getResponseBody().write("partial".getBytes(StandardCharsets.UTF_8));
          exchange.close();
        });
    assertEquals(502, send("POST", "/token/service", new byte[0], null, null).statusCode());
  }

  @Test
  void reportsUpstreamTimeout() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    upstream.createContext(
        "/token/service",
        exchange -> {
          try {
            release.await(10, TimeUnit.SECONDS);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    try {
      assertEquals(504, send("POST", "/token/service", new byte[0], null, null).statusCode());
    } finally {
      release.countDown();
    }
  }

  @Test
  void refusesUnsafeUpstreamUrls() {
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
    assertThrows(
        IllegalArgumentException.class,
        () -> new MachineTokenProxy(address, URI.create("http://localhost:8082/other")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MachineTokenProxy(address, URI.create("http://user@localhost:8082/token/service")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MachineTokenProxy(address, URI.create("http://localhost:8082/token/service?a=1")));
  }

  private HttpResponse<byte[]> send(
      String method, String path, byte[] body, String contentType, String traceparent)
      throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + proxy.port() + path))
            .timeout(Duration.ofSeconds(6))
            .method(method, HttpRequest.BodyPublishers.ofByteArray(body));
    if (contentType != null) {
      request.header("Content-Type", contentType);
    }
    if (traceparent != null) {
      request.header("traceparent", traceparent);
    }
    return client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
  }
}
