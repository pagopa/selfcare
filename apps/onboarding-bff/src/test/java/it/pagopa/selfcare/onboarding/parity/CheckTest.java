package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A parity verdict is only worth something if a deviation makes it fail: every kind of expectation is
 * run against a tiny fake BFF that answers differently from what is expected.
 */
class CheckTest {

  private static DownstreamStub stub;
  private static HttpServer bff;
  private static String baseUrl;

  @BeforeAll
  static void start() throws Exception {
    stub = new DownstreamStub();
    bff = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    bff.createContext(
        "/ok",
        exchange -> {
          byte[] body = "{\"a\":{\"b\":1},\"list\":[1,2,3]}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
          exchange.getResponseHeaders().add("X-Test", "present");
          exchange.getResponseHeaders().add("Allow", "HEAD, GET, OPTIONS");
          exchange.getResponseHeaders().add("Vary", "Origin");
          exchange.getResponseHeaders().add("Vary", "Access-Control-Request-Method, Access-Control-Request-Headers");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    bff.createContext(
        "/problem",
        exchange -> {
          byte[] body = "{\"title\":\"Bad Request\",\"status\":400,\"detail\":\"boom\"}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/problem+json");
          exchange.sendResponseHeaders(400, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    bff.createContext(
        "/empty",
        exchange -> {
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    bff.createContext(
        "/forwards",
        exchange -> {
          HttpRequest.Builder downstream = HttpRequest.newBuilder(URI.create(stub.url(DownstreamStub.MS_PRODUCT) + "/product?valid=true"));
          String authorization = exchange.getRequestHeaders().getFirst("Authorization");
          if (authorization != null) {
            downstream.header("Authorization", authorization);
          }
          try {
            HttpClient.newHttpClient().send(downstream.build(), HttpResponse.BodyHandlers.discarding());
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    bff.start();
    baseUrl = "http://127.0.0.1:" + bff.getAddress().getPort();
  }

  @AfterAll
  static void stop() {
    bff.stop(0);
    stub.close();
  }

  private static final Consumer<DownstreamStub> PRODUCT_MS =
      s -> s.on(DownstreamStub.MS_PRODUCT, "GET", "/product", Reply.json(200, "[]"));

  private static void passes(String path, Consumer<Check> expectation) {
    assertDoesNotThrow(() -> Scenario.get("self", "pass", path).stub(PRODUCT_MS).expect(expectation).run(baseUrl, stub));
  }

  private static void fails(String path, String expectedFailure, Consumer<Check> expectation) {
    AssertionError error =
        assertThrows(AssertionError.class, () -> Scenario.get("self", "fail", path).stub(PRODUCT_MS).expect(expectation).run(baseUrl, stub));
    assertTrue(error.getMessage().contains(expectedFailure), "expected the failure to mention '" + expectedFailure + "' but was: " + error.getMessage());
  }

  @Test
  void aBaseUrlEndingWithASlashDoesNotDoubleTheLeadingSlashOfThePath() {
    assertDoesNotThrow(
        () -> Scenario.get("self", "slash", "/ok").expect(c -> c.status(200)).run(baseUrl + "/", stub));
  }

  @Test
  void aScenarioWithoutExpectationsCannotPass() {
    IllegalStateException error =
        assertThrows(
            IllegalStateException.class,
            () -> Scenario.get("self", "missing-expectation", "/ok").run(baseUrl, stub));
    assertTrue(error.getMessage().contains("no expectation"));
  }

  @Test
  void statusAndContentTypeAreCompared() {
    passes("/ok", c -> c.status(200).contentType("application/json"));
    fails("/ok", "status: expected 201", c -> c.status(201));
    fails("/ok", "content-type", c -> c.contentType("application/problem+json"));
    passes("/problem", c -> c.status4xx());
    fails("/ok", "4xx", Check::status4xx);
  }

  @Test
  void headersAreComparedExactlyContainedAbsentOrAsItems() {
    passes("/ok", c -> c.header("X-Test", "present").headerContains("X-Test", "res").headerAbsent("X-Missing").headerItems("Allow", "GET", "HEAD", "OPTIONS"));
    fails("/ok", "header X-Test", c -> c.header("X-Test", "other"));
    fails("/ok", "header X-Test", c -> c.headerContains("X-Test", "zzz"));
    fails("/ok", "expected absent", c -> c.headerAbsent("X-Test"));
    fails("/ok", "header Allow", c -> c.headerItems("Allow", "GET", "HEAD"));
  }

  @Test
  void headerItemsIncludeAllRepeatedLinesAndCommaSeparatedValues() {
    passes("/ok", c -> c.headerItems("VARY", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
    fails("/ok", "header Vary", c -> c.headerItems("Vary", "Origin"));
    fails("/ok", "header Vary", c -> c.headerItems("Vary", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", "Accept"));
  }

  @Test
  void jsonBodyIsComparedByPointer() {
    passes("/ok", c -> c.json("/a/b", 1).jsonPresent("/a").jsonAbsent("/zzz").jsonSize("/list", 3).bodyContains("\"list\""));
    fails("/ok", "/a/b", c -> c.json("/a/b", 2));
    fails("/ok", "/zzz", c -> c.jsonPresent("/zzz"));
    fails("/ok", "/a", c -> c.jsonAbsent("/a"));
    fails("/ok", "/list", c -> c.jsonSize("/list", 2));
    fails("/ok", "body", c -> c.bodyContains("not in the body"));
  }

  @Test
  void problemChecksStatusContentTypeDetailAndTitle() {
    passes("/problem", c -> c.problem(400, "boom"));
    fails("/problem", "/detail", c -> c.problem(400, "other"));
    fails("/problem", "status: expected 404", c -> c.problem(404, null));
    fails("/ok", "content-type", c -> c.problem(200, null));
  }

  @Test
  void emptyBodyIsCheckedBothWays() {
    passes("/empty", c -> c.status(204).noBody());
    fails("/ok", "body: expected empty", Check::noBody);
  }

  @Test
  void downstreamCallsAreCountedAndCompared() {
    passes("/ok", c -> c.totalCalls(0).noCalls(DownstreamStub.MS_PRODUCT));
    passes("/forwards", c -> c.totalCalls(1).callCount(DownstreamStub.MS_PRODUCT, 1).exactCalls("ms-product GET /product").callsAtLeast(DownstreamStub.MS_PRODUCT, 1).callsAtMost(DownstreamStub.MS_PRODUCT, 1));
    passes("/forwards", c -> c.call(DownstreamStub.MS_PRODUCT, "GET", "/product").times(1).query("valid", "true").queryAbsent("rootOnly"));
    fails("/forwards", "expected 0 calls but received 1", c -> c.totalCalls(0));
    fails("/forwards", "expected no calls", c -> c.noCalls(DownstreamStub.MS_PRODUCT));
    fails("/ok", "downstream calls: expected", c -> c.exactCalls("ms-product GET /product"));
    fails("/forwards", "downstream: expected ms-product GET /other", c -> c.call(DownstreamStub.MS_PRODUCT, "GET", "/other"));
    fails("/forwards", "rootOnly", c -> c.call(DownstreamStub.MS_PRODUCT, "GET", "/product").query("rootOnly", "false"));
    fails("/forwards", "valid", c -> c.call(DownstreamStub.MS_PRODUCT, "GET", "/product").query("valid", "false"));
    fails("/forwards", "downstream", c -> c.call(DownstreamStub.MS_PRODUCT, "GET", "/product").times(2));
  }

  @Test
  void identityPropagationIsVerifiedOnEveryDownstreamCall() {
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> Scenario.api("self", "no-tenant-forwarded", "/forwards").stub(PRODUCT_MS).expect(Check::propagatesIdentity).run(baseUrl, stub));
    assertTrue(error.getMessage().contains("X-Tenant-Id expected PNPG but was null"), error.getMessage());
    assertTrue(!error.getMessage().contains("bearer token was not propagated"), error.getMessage());
    fails("/ok", "propagation cannot be verified", Check::propagatesIdentity);
  }

  @Test
  void anUnstubbedDownstreamCallFailsTheScenarioUnlessAllowed() {
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> Scenario.get("self", "unstubbed", "/forwards").expect(c -> c.status(204)).run(baseUrl, stub));
    assertTrue(error.getMessage().contains("UNSTUBBED"), error.getMessage());
    assertDoesNotThrow(() -> Scenario.get("self", "allowed", "/forwards").expect(c -> c.status(204).allowUnstubbed()).run(baseUrl, stub));
  }

  @Test
  void elapsedTimeBoundsAreEnforced() {
    passes("/ok", c -> c.elapsedBelow(30_000));
    fails("/ok", "elapsed: expected at least", c -> c.elapsedAtLeast(60_000));
    fails("/ok", "elapsed: expected below", c -> c.elapsedBelow(0));
  }
}
