package it.pagopa.selfcare.auth.proxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class MachineTokenProxy implements AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(MachineTokenProxy.class.getName());
  private static final String PATH = "/token/service";
  private static final int MAX_REQUEST_BYTES = 8192;
  private static final int MAX_RESPONSE_BYTES = 16384;
  private static final List<String> REQUEST_HEADERS =
      List.of("Content-Type", "Content-Encoding", "Accept", "traceparent", "tracestate");
  private static final List<String> RESPONSE_HEADERS =
      List.of("Content-Type", "Cache-Control", "Pragma", "Retry-After", "Location");

  private final URI upstream;
  private final HttpServer server;
  private final ThreadPoolExecutor executor;

  MachineTokenProxy(InetSocketAddress address, URI upstream) throws IOException {
    if (!"http".equalsIgnoreCase(upstream.getScheme())
        || upstream.getHost() == null
        || upstream.getPort() < 1
        || !PATH.equals(upstream.getRawPath())
        || upstream.getRawQuery() != null
        || upstream.getRawFragment() != null
        || upstream.getRawUserInfo() != null) {
      throw new IllegalArgumentException("Invalid M2M proxy upstream URL");
    }
    this.upstream = upstream;
    this.server = HttpServer.create(address, 64);
    this.executor =
        new ThreadPoolExecutor(
            8,
            8,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),
            new ThreadPoolExecutor.CallerRunsPolicy());
    server.setExecutor(executor);
    server.createContext("/", this::handle);
  }

  public static void main(String[] args) throws IOException {
    String upstream = System.getenv("M2M_PROXY_UPSTREAM_URL");
    if (upstream == null || upstream.isBlank()) {
      throw new IllegalStateException("M2M_PROXY_UPSTREAM_URL is required");
    }
    MachineTokenProxy proxy =
        new MachineTokenProxy(new InetSocketAddress(8080), URI.create(upstream));
    Runtime.getRuntime().addShutdownHook(new Thread(proxy::close, "m2m-proxy-shutdown"));
    proxy.start();
    LOGGER.info("event=m2m_proxy_started port=8080");
  }

  void start() {
    server.start();
  }

  int port() {
    return server.getAddress().getPort();
  }

  private void handle(HttpExchange exchange) throws IOException {
    try {
      URI requestUri = exchange.getRequestURI();
      if (!PATH.equals(requestUri.getRawPath()) || requestUri.getRawQuery() != null) {
        problem(exchange, 404, "Resource not found");
        return;
      }
      if (!"POST".equals(exchange.getRequestMethod())) {
        exchange.getResponseHeaders().set("Allow", "POST");
        problem(exchange, 405, "Method not allowed");
        return;
      }

      byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
      if (body.length > MAX_REQUEST_BYTES) {
        problem(exchange, 413, "Request body too large");
        return;
      }

      UpstreamResponse response;
      try {
        response = forward(exchange, body);
      } catch (SocketTimeoutException exception) {
        LOGGER.warning("event=m2m_proxy_upstream_timeout");
        problem(exchange, 504, "Upstream timed out");
        return;
      } catch (IOException exception) {
        LOGGER.warning(
            "event=m2m_proxy_upstream_failed type=" + exception.getClass().getSimpleName());
        problem(exchange, 502, "Upstream unavailable");
        return;
      }

      response.headers().forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
      exchange.getResponseHeaders().putIfAbsent("Cache-Control", List.of("no-store"));
      exchange.getResponseHeaders().putIfAbsent("Pragma", List.of("no-cache"));
      respond(exchange, response.status(), response.body());
    } finally {
      exchange.close();
    }
  }

  private UpstreamResponse forward(HttpExchange exchange, byte[] body) throws IOException {
    HttpURLConnection connection =
        (HttpURLConnection) upstream.toURL().openConnection(Proxy.NO_PROXY);
    try {
      connection.setRequestMethod("POST");
      connection.setConnectTimeout(1000);
      connection.setReadTimeout(3000);
      connection.setInstanceFollowRedirects(false);
      connection.setUseCaches(false);
      connection.setDoOutput(true);
      connection.setFixedLengthStreamingMode(body.length);
      for (String name : REQUEST_HEADERS) {
        String value = exchange.getRequestHeaders().getFirst(name);
        if (value != null) {
          connection.setRequestProperty(name, value);
        }
      }
      try (var output = connection.getOutputStream()) {
        output.write(body);
      }
      int status = connection.getResponseCode();
      if (status < 100 || status > 599) {
        throw new IOException("Invalid upstream status");
      }
      long contentLength = connection.getContentLengthLong();
      if (contentLength > MAX_RESPONSE_BYTES) {
        throw new IOException("Upstream response too large");
      }
      byte[] responseBody;
      try (InputStream input =
          status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
        responseBody = input == null ? new byte[0] : input.readNBytes(MAX_RESPONSE_BYTES + 1);
      }
      if (responseBody.length > MAX_RESPONSE_BYTES) {
        throw new IOException("Upstream response too large");
      }
      if (contentLength >= 0 && responseBody.length != contentLength) {
        throw new IOException("Incomplete upstream response");
      }
      Map<String, String> headers = new LinkedHashMap<>();
      for (String name : RESPONSE_HEADERS) {
        String value = connection.getHeaderField(name);
        if (value != null) {
          headers.put(name, value);
        }
      }
      return new UpstreamResponse(status, responseBody, headers);
    } finally {
      connection.disconnect();
    }
  }

  private void problem(HttpExchange exchange, int status, String detail) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.getResponseHeaders().set("Pragma", "no-cache");
    byte[] body =
        ("{\"title\":\"Request rejected\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}")
            .getBytes(StandardCharsets.UTF_8);
    respond(exchange, status, body);
  }

  private void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
    if (body.length == 0 || "HEAD".equals(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(status, -1);
    } else {
      exchange.sendResponseHeaders(status, body.length);
      exchange.getResponseBody().write(body);
    }
  }

  @Override
  public void close() {
    server.stop(0);
    executor.shutdownNow();
  }

  private record UpstreamResponse(int status, byte[] body, Map<String, String> headers) {}
}
