package it.pagopa.selfcare.onboarding.parity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Call;
import it.pagopa.selfcare.onboarding.parity.Scenario.Exchange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/** Collects every mismatch of one scenario so the owner of the failing behaviour sees them all at once. */
public final class Check {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Scenario scenario;
  private final Exchange exchange;
  private final List<String> failures = new ArrayList<>();
  private boolean allowUnstubbed;

  Check(Scenario scenario, Exchange exchange) {
    this.scenario = scenario;
    this.exchange = exchange;
  }

  public Exchange exchange() {
    return exchange;
  }

  public Check status(int expected) {
    if (exchange.status() != expected) {
      failures.add("status: expected " + expected + " but was " + exchange.status());
    }
    return this;
  }

  public Check status4xx() {
    if (exchange.status() < 400 || exchange.status() > 499) {
      failures.add("status: expected a 4xx but was " + exchange.status());
    }
    return this;
  }

  /** Compares only the media type; use {@link #header} to assert parameters too. */
  public Check contentType(String expectedMediaType) {
    String actual = exchange.header("content-type");
    String actualType =
        actual == null ? null : actual.split(";")[0].trim().toLowerCase(Locale.ROOT);
    if (!expectedMediaType.toLowerCase(Locale.ROOT).equals(actualType)) {
      failures.add("content-type: expected " + expectedMediaType + " but was " + actual);
    }
    return this;
  }

  public Check noBody() {
    if (exchange.body().length != 0) {
      failures.add("body: expected empty but was " + abbreviate(exchange.bodyText()));
    }
    return this;
  }

  public Check header(String name, String expected) {
    String actual = exchange.header(name);
    if (!Objects.equals(expected, actual)) {
      failures.add("header " + name + ": expected " + expected + " but was " + actual);
    }
    return this;
  }

  public Check headerContains(String name, String fragment) {
    String actual = exchange.header(name);
    if (actual == null || !actual.contains(fragment)) {
      failures.add("header " + name + ": expected to contain " + fragment + " but was " + actual);
    }
    return this;
  }

  /** All lines of a comma-separated header form one unordered set, e.g. Allow or Vary. */
  public Check headerItems(String name, String... expected) {
    List<String> actual = exchange.headers().getOrDefault(name.toLowerCase(Locale.ROOT), List.of());
    java.util.Set<String> want = new java.util.TreeSet<>(java.util.List.of(expected));
    java.util.Set<String> got = new java.util.TreeSet<>();
    for (String line : actual) {
      for (String item : line.split(",")) {
        got.add(item.trim());
      }
    }
    if (!want.equals(got)) {
      failures.add("header " + name + ": expected the items " + want + " but was " + actual);
    }
    return this;
  }

  public Check headerAbsent(String name) {
    if (exchange.header(name) != null) {
      failures.add("header " + name + ": expected absent but was " + exchange.header(name));
    }
    return this;
  }

  public Check bodyBytes(byte[] expected) {
    if (!Arrays.equals(expected, exchange.body())) {
      failures.add(
          "body bytes: expected "
              + expected.length
              + " bytes but received "
              + exchange.body().length);
    }
    return this;
  }

  public Check bodyContains(String fragment) {
    if (!exchange.bodyText().contains(fragment)) {
      failures.add(
          "body: expected to contain '" + fragment + "' but was " + abbreviate(exchange.bodyText()));
    }
    return this;
  }

  /** Asserts a JSON value addressed with a JSON pointer ("/detail", "/0/id"). */
  public Check json(String pointer, Object expected) {
    JsonNode node = node(pointer);
    if (node == null || node.isMissingNode()) {
      failures.add("json " + pointer + ": expected " + expected + " but it is missing");
    } else if (!Objects.equals(normalize(expected), toJava(node))) {
      failures.add("json " + pointer + ": expected " + expected + " but was " + toJava(node));
    }
    return this;
  }

  public Check jsonPresent(String pointer) {
    JsonNode node = node(pointer);
    if (node == null || node.isMissingNode() || node.isNull()) {
      failures.add("json " + pointer + ": expected present");
    }
    return this;
  }

  public Check jsonAbsent(String pointer) {
    JsonNode node = node(pointer);
    if (node != null && !node.isMissingNode() && !node.isNull()) {
      failures.add("json " + pointer + ": expected absent but was " + toJava(node));
    }
    return this;
  }

  public Check jsonSize(String pointer, int expected) {
    JsonNode node = node(pointer);
    if (node == null || !node.isContainerNode()) {
      failures.add("json " + pointer + ": expected a container of size " + expected);
    } else if (node.size() != expected) {
      failures.add("json " + pointer + ": expected size " + expected + " but was " + node.size());
    }
    return this;
  }

  /** The error body must be the commons Problem shape: status/title/detail present and consistent. */
  public Check problem(int status, String detail) {
    status(status).contentType("application/problem+json").json("/status", status);
    if (detail != null) {
      json("/detail", detail);
    }
    jsonPresent("/title");
    return this;
  }

  public Check allowUnstubbed() {
    this.allowUnstubbed = true;
    return this;
  }

  /** Exact ordered list of downstream interactions, as {@code "service METHOD /path"}. */
  public Check exactCalls(String... expected) {
    List<String> actual =
        exchange.calls().stream()
            .map(c -> c.service() + " " + c.method() + " " + c.path())
            .collect(Collectors.toList());
    if (!actual.equals(Arrays.asList(expected))) {
      failures.add(
          "downstream calls: expected "
              + Arrays.asList(expected)
              + " but was "
              + actual);
    }
    return this;
  }

  public Check noCalls(String service) {
    long count = exchange.calls().stream().filter(c -> c.service().equals(service)).count();
    if (count != 0) {
      failures.add("downstream " + service + ": expected no calls but received " + count);
    }
    return this;
  }

  public Check callCount(String service, int expected) {
    long count = exchange.calls().stream().filter(c -> c.service().equals(service)).count();
    if (count != expected) {
      failures.add("downstream " + service + ": expected " + expected + " calls but received " + count);
    }
    return this;
  }

  /** Every downstream call must carry the caller's bearer token and X-Tenant-Id. */
  public Check propagatesIdentity() {
    String expectedTenant =
        scenario.headers.stream()
            .filter(h -> h[0].equalsIgnoreCase("X-Tenant-Id"))
            .map(h -> h[1])
            .findFirst()
            .orElse(null);
    for (Call call : exchange.calls()) {
      if (!("Bearer " + scenario.bearer).equals(call.header("authorization"))) {
        failures.add("downstream " + call + ": caller bearer token was not propagated");
      }
      if (expectedTenant != null && !expectedTenant.equals(call.header("x-tenant-id"))) {
        failures.add(
            "downstream "
                + call
                + ": X-Tenant-Id expected "
                + expectedTenant
                + " but was "
                + call.header("x-tenant-id"));
      }
    }
    if (exchange.calls().isEmpty()) {
      failures.add("propagation cannot be verified: no downstream call was made");
    }
    return this;
  }

  public Check downstreamApiKey(String expected) {
    for (Call call : exchange.calls()) {
      if (!List.of(expected).equals(call.headers().get("x-api-key"))) {
        failures.add("downstream " + call + ": configured x-api-key missing, duplicated or different");
      }
      if (call.headers().containsKey("x-functions-key")) {
        failures.add("downstream " + call + ": unexpected x-functions-key");
      }
    }
    return this;
  }

  public Check callsAtLeast(String service, int min) {
    int actual = (int) exchange.calls().stream().filter(c -> c.service().equals(service)).count();
    if (actual < min) {
      failures.add(service + ": expected at least " + min + " calls but received " + actual);
    }
    return this;
  }

  public Check callsAtMost(String service, int max) {
    int actual = (int) exchange.calls().stream().filter(c -> c.service().equals(service)).count();
    if (actual > max) {
      failures.add(service + ": expected at most " + max + " calls but received " + actual);
    }
    return this;
  }

  /** The exchange lasted at least {@code millis}: the BFF waited (retries with a pause). */
  public Check elapsedAtLeast(long millis) {
    if (exchange.elapsedMs() < millis) {
      failures.add("elapsed: expected at least " + millis + " ms but was " + exchange.elapsedMs());
    }
    return this;
  }

  /** The exchange failed fast: no pausing retry. */
  public Check elapsedBelow(long millis) {
    if (exchange.elapsedMs() >= millis) {
      failures.add("elapsed: expected below " + millis + " ms but was " + exchange.elapsedMs());
    }
    return this;
  }

  public Check totalCalls(int expected) {
    if (exchange.calls().size() != expected) {
      failures.add(
          "downstream: expected "
              + expected
              + " calls but received "
              + exchange.calls().size()
              + " "
              + exchange.calls());
    }
    return this;
  }

  /** Looks up the first downstream call of the given service/method/path (literal, without prefix). */
  public CallCheck call(String service, String method, String path) {
    List<Call> matching =
        exchange.calls().stream()
            .filter(
                c ->
                    c.service().equals(service)
                        && c.method().equalsIgnoreCase(method)
                        && c.path().equals(path))
            .toList();
    if (matching.isEmpty()) {
      failures.add(
          "downstream: expected "
              + service
              + " "
              + method
              + " "
              + path
              + " but calls were "
              + exchange.calls());
    }
    return new CallCheck(service + " " + method + " " + path, matching);
  }

  /** Assertions over the recorded calls matching one downstream interaction. */
  public final class CallCheck {
    private final String label;
    private final List<Call> calls;

    private CallCheck(String label, List<Call> calls) {
      this.label = label;
      this.calls = calls;
    }

    private Call first() {
      return calls.isEmpty() ? null : calls.get(0);
    }

    /** Continues with another downstream interaction of the same exchange. */
    public CallCheck call(String service, String method, String path) {
      return Check.this.call(service, method, path);
    }

    public CallCheck times(int expected) {
      if (calls.size() != expected) {
        failures.add(label + ": expected " + expected + " calls but received " + calls.size());
      }
      return this;
    }

    public CallCheck query(String name, String expected) {
      if (first() != null) {
        List<String> values = first().queryParams().get(name);
        if (values == null || !values.contains(expected)) {
          failures.add(
              label + ": expected query " + name + "=" + expected + " but was " + first().rawQuery());
        }
      }
      return this;
    }

    /**
     * A list parameter, whether sent repeated ({@code fl=a&fl=b}) or comma separated ({@code
     * fl=a,b}): the requested elements must be exactly the expected ones.
     */
    public CallCheck queryList(String name, String... expected) {
      if (first() != null) {
        List<String> values = first().queryParams().getOrDefault(name, List.of());
        java.util.Set<String> actual = new java.util.TreeSet<>();
        for (String value : values) {
          actual.addAll(java.util.Arrays.asList(value.split(",")));
        }
        java.util.Set<String> wanted = new java.util.TreeSet<>(java.util.Arrays.asList(expected));
        if (!actual.equals(wanted)) {
          failures.add(label + ": expected query list " + name + "=" + wanted + " but was " + first().rawQuery());
        }
      }
      return this;
    }

    public CallCheck queryAbsent(String name) {
      if (first() != null && first().queryParams().containsKey(name)) {
        failures.add(label + ": expected no query " + name + " but was " + first().rawQuery());
      }
      return this;
    }

    public CallCheck header(String name, String expected) {
      if (first() != null && !Objects.equals(expected, first().header(name))) {
        failures.add(
            label + ": expected header " + name + "=" + expected + " but was " + first().header(name));
      }
      return this;
    }

    public CallCheck headerAbsent(String name) {
      if (first() != null && first().header(name) != null) {
        failures.add(
            label + ": expected header " + name + " absent but was " + first().header(name));
      }
      return this;
    }

    public CallCheck bearerOf(String token) {
      return header("authorization", "Bearer " + token);
    }

    public CallCheck jsonBody(String pointer, Object expected) {
      if (first() != null) {
        try {
          JsonNode node = MAPPER.readTree(first().body()).at(pointer);
          if (node.isMissingNode() || !Objects.equals(normalize(expected), toJava(node))) {
            failures.add(
                label
                    + ": expected body "
                    + pointer
                    + "="
                    + expected
                    + " but body was "
                    + abbreviate(first().bodyText()));
          }
        } catch (Exception e) {
          failures.add(label + ": body is not JSON: " + abbreviate(first().bodyText()));
        }
      }
      return this;
    }

    public CallCheck bodyEmpty() {
      if (first() != null && first().body().length != 0) {
        failures.add(label + ": expected empty body but was " + abbreviate(first().bodyText()));
      }
      return this;
    }

    /** Names of the multipart parts received by the downstream, in order. */
    public CallCheck multipartParts(String... names) {
      if (first() != null) {
        List<String> actual =
            Multipart.parse(first().header("content-type"), first().body()).stream()
                .map(Multipart.Part::name)
                .toList();
        if (!actual.equals(Arrays.asList(names))) {
          failures.add(
              label + ": expected multipart parts " + Arrays.asList(names) + " but was " + actual);
        }
      }
      return this;
    }

    public CallCheck multipartPart(
        String name, String expectedFilename, String expectedContentType, byte[] expectedContent) {
      if (first() != null) {
        Multipart.Part part =
            Multipart.parse(first().header("content-type"), first().body()).stream()
                .filter(p -> name.equals(p.name()))
                .findFirst()
                .orElse(null);
        if (part == null) {
          failures.add(label + ": multipart part " + name + " is missing");
        } else {
          if (expectedFilename != null && !expectedFilename.equals(part.filename())) {
            failures.add(
                label + ": part " + name + " filename expected " + expectedFilename + " but was " + part.filename());
          }
          if (expectedContentType != null
              && (part.contentType() == null
                  || !part.contentType().toLowerCase(Locale.ROOT).startsWith(expectedContentType))) {
            failures.add(
                label + ": part " + name + " content-type expected " + expectedContentType + " but was " + part.contentType());
          }
          if (expectedContent != null && !Arrays.equals(expectedContent, part.content())) {
            failures.add(label + ": part " + name + " content differs");
          }
        }
      }
      return this;
    }

    public CallCheck multipartField(String name, String expectedValue) {
      if (first() != null) {
        Multipart.Part part =
            Multipart.parse(first().header("content-type"), first().body()).stream()
                .filter(p -> name.equals(p.name()))
                .findFirst()
                .orElse(null);
        if (part == null || !expectedValue.equals(part.text())) {
          failures.add(
              label
                  + ": multipart field "
                  + name
                  + " expected "
                  + expectedValue
                  + " but was "
                  + (part == null ? "<missing>" : part.text()));
        }
      }
      return this;
    }
  }

  void verify() {
    if (!allowUnstubbed) {
      exchange.calls().stream()
          .filter(c -> !c.matched())
          .forEach(c -> failures.add("unexpected downstream call: " + c));
    }
    if (!failures.isEmpty()) {
      throw new AssertionError(
          scenario.displayName()
              + "\n  - "
              + String.join("\n  - ", failures)
              + "\n  actual: HTTP "
              + exchange.status()
              + " "
              + exchange.header("content-type")
              + " "
              + abbreviate(exchange.bodyText())
              + "\n  downstream: "
              + exchange.calls()
              + "\n  elapsed(ms): "
              + exchange.elapsedMs());
    }
  }

  private JsonNode node(String pointer) {
    try {
      return MAPPER.readTree(exchange.body()).at(pointer);
    } catch (Exception e) {
      failures.add("json " + pointer + ": body is not JSON: " + abbreviate(exchange.bodyText()));
      return null;
    }
  }

  private static Object normalize(Object value) {
    return value instanceof Integer i ? Long.valueOf(i) : value;
  }

  private static Object toJava(JsonNode node) {
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isBoolean()) {
      return node.asBoolean();
    }
    if (node.isIntegralNumber()) {
      return node.asLong();
    }
    if (node.isNumber()) {
      return node.asDouble();
    }
    return node.toString();
  }

  private static String abbreviate(String text) {
    return text.length() > 500 ? text.substring(0, 500) + "..." : text;
  }
}
