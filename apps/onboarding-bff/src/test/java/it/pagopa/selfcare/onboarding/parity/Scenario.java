package it.pagopa.selfcare.onboarding.parity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * One black-box HTTP scenario: the request sent to the BFF, the controlled downstream behaviour,
 * and the externally visible contract (status, headers, body, downstream interaction) expected
 * from the reference implementation (the Spring BFF on main).
 */
public final class Scenario {

  public final String group;
  public final String id;
  final String method;
  final String path;
  String bearer;
  final List<String[]> headers = new ArrayList<>();
  byte[] body;
  String contentType;
  final List<Consumer<DownstreamStub>> stubbing = new ArrayList<>();
  Consumer<Check> expectation;

  private Scenario(String group, String id, String method, String path) {
    this.group = group;
    this.id = id;
    this.method = method;
    this.path = path;
  }

  public static Scenario of(String group, String id, String method, String path) {
    return new Scenario(group, id, method, path);
  }

  public static Scenario get(String group, String id, String path) {
    return of(group, id, "GET", path);
  }

  public static Scenario post(String group, String id, String path) {
    return of(group, id, "POST", path);
  }

  public Scenario as(ParityJwt.User user) {
    this.bearer = ParityJwt.token(user);
    return this;
  }

  public Scenario as(ParityJwt.User user, String tenantClaim) {
    this.bearer = ParityJwt.token(user, tenantClaim);
    return this;
  }

  public Scenario bearer(String token) {
    this.bearer = token;
    return this;
  }

  /** Adds a header; calling it twice with the same name sends the header twice. */
  public Scenario header(String name, String value) {
    headers.add(new String[] {name, value});
    return this;
  }

  /** Replaces any header of that name. */
  public Scenario setHeader(String name, String value) {
    headers.removeIf(h -> h[0].equalsIgnoreCase(name));
    return header(name, value);
  }

  /** Authenticated request as the default admin on the default tenant (the common case). */
  public static Scenario api(String group, String id, String method, String path) {
    return of(group, id, method, path).as(ParityJwt.User.ADMIN).header("X-Tenant-Id", "PNPG");
  }

  public static Scenario api(String group, String id, String path) {
    return api(group, id, "GET", path);
  }

  public Scenario user(ParityJwt.User user) {
    this.bearer = ParityJwt.token(user);
    return this;
  }

  /** Token with the given tenant_id claim and the matching X-Tenant-Id header. */
  public Scenario tenant(String tenant) {
    this.bearer = ParityJwt.token(ParityJwt.User.ADMIN, tenant);
    return setHeader("X-Tenant-Id", tenant);
  }

  public Scenario json(String body) {
    this.body = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    this.contentType = "application/json";
    return this;
  }

  public Scenario raw(String contentType, byte[] body) {
    this.body = body;
    this.contentType = contentType;
    return this;
  }

  public Scenario multipart(Multipart.Builder builder) {
    this.body = builder.bytes();
    this.contentType = builder.contentType();
    return this;
  }

  public Scenario stub(Consumer<DownstreamStub> setup) {
    stubbing.add(setup);
    return this;
  }

  public Scenario expect(Consumer<Check> expectation) {
    this.expectation = expectation;
    return this;
  }

  public String displayName() {
    return group + " :: " + id + " [" + method + " " + path + "]";
  }

  @Override
  public String toString() {
    return displayName();
  }

  /** What the BFF answered, plus everything the downstream stub saw while it did. */
  public record Exchange(
      int status,
      Map<String, List<String>> headers,
      byte[] body,
      List<DownstreamStub.Call> calls,
      long elapsedMs) {

    public String header(String name) {
      List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
      return values == null || values.isEmpty() ? null : values.get(0);
    }

    public String bodyText() {
      return new String(body, java.nio.charset.StandardCharsets.UTF_8);
    }
  }

  private static final HttpClient CLIENT =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .followRedirects(HttpClient.Redirect.NEVER)
          .connectTimeout(Duration.ofSeconds(10))
          .build();

  /** Runs the scenario against {@code baseUrl} using {@code stub}; throws AssertionError on any parity failure. */
  public Exchange run(String baseUrl, DownstreamStub stub) {
    if (expectation == null) {
      throw new IllegalStateException(displayName() + " - scenario has no expectation");
    }
    stub.reset();
    stubbing.forEach(s -> s.accept(stub));

    // an injected base URL may end with '/': the path starts with one and must not be sent doubled
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + path))
            .timeout(Duration.ofSeconds(60))
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(body));
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    if (contentType != null) {
      request.header("Content-Type", contentType);
    }
    headers.forEach(h -> request.header(h[0], h[1]));

    long start = System.nanoTime();
    HttpResponse<byte[]> response;
    try {
      response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(displayName() + " - request interrupted", e);
    } catch (IOException e) {
      throw new AssertionError(displayName() + " - request failed: " + e, e);
    }
    long elapsed = (System.nanoTime() - start) / 1_000_000;

    Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
    response
        .headers()
        .map()
        .forEach((k, v) -> responseHeaders.put(k.toLowerCase(Locale.ROOT), List.copyOf(v)));
    Exchange exchange =
        new Exchange(response.statusCode(), responseHeaders, response.body(), stub.calls(), elapsed);

    if (Boolean.getBoolean("parity.dump")) {
      System.out.println("DUMP " + displayName() + " -> " + exchange.status() + " " + exchange.headers());
      System.out.println("  body: " + exchange.bodyText());
      exchange.calls().forEach(c -> System.out.println("  call: " + c + " headers=" + c.headers()));
    }
    Check check = new Check(this, exchange);
    expectation.accept(check);
    check.verify();
    return exchange;
  }
}
