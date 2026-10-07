package it.pagopa.selfcare.onboarding.parity;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Controlled downstream used by the black-box parity harness. A single JDK {@link HttpServer}
 * plays every downstream service of the BFF: the first path segment selects the service
 * ({@code /ms-core/...}, {@code /ms-onboarding/...}), the rest is the path seen by the service.
 * Every request is recorded (including the unmatched ones, which receive a 501) so scenarios can
 * assert the exact downstream interaction: method, path, query, headers, body and call count.
 */
public final class DownstreamStub implements AutoCloseable {

  public static final String MS_ONBOARDING = "ms-onboarding";
  public static final String MS_USER = "ms-user";
  public static final String MS_PRODUCT = "ms-product";
  public static final String MS_CORE = "ms-core";
  public static final String MS_DOCUMENT = "ms-document";
  public static final String MS_IAM = "ms-iam";
  public static final String PARTY_PROCESS = "party-process";
  public static final String PARTY_REGISTRY_PROXY = "party-registry-proxy";
  public static final String USER_REGISTRY = "user-registry";
  public static final String ONBOARDING_FN = "onboarding-fn";

  /** A request received by the stub. Both path forms exclude the service prefix. */
  public record Call(
      String service,
      String method,
      String path,
      String rawPath,
      String rawQuery,
      Map<String, List<String>> headers,
      byte[] body,
      boolean matched) {

    public String header(String name) {
      List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
      return values == null || values.isEmpty() ? null : values.get(0);
    }

    public String bodyText() {
      return new String(body, StandardCharsets.UTF_8);
    }

    public Map<String, List<String>> queryParams() {
      return Multipart.queryParams(rawQuery);
    }

    @Override
    public String toString() {
      return service
          + " "
          + method
          + " "
          + path
          + (rawQuery == null || rawQuery.isEmpty() ? "" : "?" + rawQuery)
          + (matched ? "" : " [UNSTUBBED]");
    }
  }

  /** What the stub answers to a matched request. */
  public record Reply(int status, Map<String, String> headers, byte[] body, long delayMs) {

    public static Reply json(int status, String json) {
      return new Reply(
          status,
          Map.of("Content-Type", "application/json"),
          json.getBytes(StandardCharsets.UTF_8),
          0);
    }

    public static Reply status(int status) {
      return new Reply(status, Map.of(), new byte[0], 0);
    }

    public static Reply problem(int status, String detail) {
      return json(
              status,
              JsonNodeFactory.instance.objectNode().put("status", status).put("detail", detail).toString())
          .header("Content-Type", "application/problem+json");
    }

    /** The connection is closed without any response: a transport failure for the caller. */
    public static Reply abort() {
      return new Reply(-1, Map.of(), new byte[0], 0);
    }

    public static Reply bytes(int status, String contentType, byte[] body) {
      return new Reply(status, Map.of("Content-Type", contentType), body, 0);
    }

    public Reply header(String name, String value) {
      Map<String, String> copy = new LinkedHashMap<>(headers);
      copy.put(name, value);
      return new Reply(status, copy, body, delayMs);
    }

    public Reply delay(long millis) {
      return new Reply(status, headers, body, millis);
    }
  }

  private record Rule(String service, String method, Pattern path, Function<Call, Reply> reply) {}

  private final HttpServer server;
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final List<Rule> rules = new CopyOnWriteArrayList<>();
  private final List<Call> calls = Collections.synchronizedList(new ArrayList<>());

  public DownstreamStub() {
    try {
      server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot start downstream stub", e);
    }
    server.createContext("/", this::handle);
    server.setExecutor(executor);
    server.start();
  }

  public int port() {
    return server.getAddress().getPort();
  }

  /** Base URL of one downstream service, e.g. {@code http://127.0.0.1:41234/ms-core}. */
  public String url(String service) {
    return "http://127.0.0.1:" + port() + "/" + service;
  }

  public void reset() {
    rules.clear();
    calls.clear();
  }

  public DownstreamStub on(String service, String method, String pathRegex, Reply reply) {
    return on(service, method, pathRegex, call -> reply);
  }

  public DownstreamStub on(
      String service, String method, String pathRegex, Function<Call, Reply> reply) {
    rules.add(new Rule(service, method, Pattern.compile(pathRegex), reply));
    return this;
  }

  public List<Call> calls() {
    synchronized (calls) {
      return List.copyOf(calls);
    }
  }

  public List<Call> callsTo(String service) {
    return calls().stream().filter(c -> c.service().equals(service)).toList();
  }

  private void handle(HttpExchange exchange) throws IOException {
    byte[] body;
    try (InputStream in = exchange.getRequestBody()) {
      body = in.readAllBytes();
    }
    String decodedPath = exchange.getRequestURI().getPath();
    String rawQuery = exchange.getRequestURI().getRawQuery();
    String[] segments = decodedPath.split("/", 3);
    String[] rawSegments = exchange.getRequestURI().getRawPath().split("/", 3);
    String service = segments.length > 1 ? segments[1] : "";
    String path = segments.length > 2 ? "/" + segments[2] : "/";
    String rawPath = rawSegments.length > 2 ? "/" + rawSegments[2] : "/";

    Map<String, List<String>> headers = new LinkedHashMap<>();
    exchange
        .getRequestHeaders()
        .forEach((k, v) -> headers.put(k.toLowerCase(Locale.ROOT), List.copyOf(v)));

    String method = exchange.getRequestMethod();
    Rule matched = null;
    for (Rule rule : rules) {
      if (rule.service().equals(service)
          && rule.method().equalsIgnoreCase(method)
          && rule.path().matcher(path).matches()) {
        matched = rule;
        break;
      }
    }
    Call call = new Call(service, method, path, rawPath, rawQuery, headers, body, matched != null);
    calls.add(call);

    Reply reply =
        matched == null
            ? Reply.json(501, "{\"detail\":\"UNSTUBBED " + service + " " + path + "\"}")
            : matched.reply().apply(call);
    try {
      if (reply.delayMs() > 0) {
        Thread.sleep(reply.delayMs());
      }
      if (reply.status() < 0) {
        return;
      }
      reply.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
      if ("HEAD".equalsIgnoreCase(method) || reply.body().length == 0) {
        exchange.sendResponseHeaders(reply.status(), -1);
      } else {
        exchange.sendResponseHeaders(reply.status(), reply.body().length);
        try (OutputStream out = exchange.getResponseBody()) {
          out.write(reply.body());
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException ignored) {
      // The caller gave up (timeout): nothing to answer.
    } finally {
      exchange.close();
    }
  }

  @Override
  public void close() {
    server.stop(0);
    executor.shutdownNow();
  }
}
