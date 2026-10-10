package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The stub is the evidence base of every parity verdict: it must record and answer exactly. */
class DownstreamStubTest {

  private static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  private DownstreamStub stub;

  @BeforeEach
  void start() {
    stub = new DownstreamStub();
  }

  @AfterEach
  void stop() {
    stub.close();
  }

  private HttpResponse<byte[]> send(String method, String url, String body, String... headers) throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
    for (int i = 0; i < headers.length; i += 2) {
      request.header(headers[i], headers[i + 1]);
    }
    return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
  }

  @Test
  void answersTheMatchingRuleAndRecordsTheCall() throws Exception {
    stub.on(DownstreamStub.MS_CORE, "GET", "/institutions/[^/]+", Reply.json(200, "{\"id\":\"i1\"}"));

    HttpResponse<byte[]> response =
        send("GET", stub.url(DownstreamStub.MS_CORE) + "/institutions/i1?a=1&a=2&b=x%20y", null, "X-Tenant-Id", "PNPG");

    assertEquals(200, response.statusCode());
    assertEquals("application/json", response.headers().firstValue("content-type").orElseThrow());
    assertEquals("{\"id\":\"i1\"}", new String(response.body(), StandardCharsets.UTF_8));
    assertEquals(1, stub.calls().size());
    DownstreamStub.Call call = stub.calls().get(0);
    assertEquals(DownstreamStub.MS_CORE, call.service());
    assertEquals("GET", call.method());
    assertEquals("/institutions/i1", call.path());
    assertEquals("PNPG", call.header("x-tenant-id"));
    assertEquals("PNPG", call.header("X-TENANT-ID"));
    assertEquals(List.of("1", "2"), call.queryParams().get("a"));
    assertEquals(List.of("x y"), call.queryParams().get("b"));
    assertTrue(call.matched());
  }

  @Test
  void unmatchedRequestsGet501AndAreFlagged() throws Exception {
    stub.on(DownstreamStub.MS_CORE, "GET", "/known", Reply.status(204));

    HttpResponse<byte[]> wrongMethod = send("POST", stub.url(DownstreamStub.MS_CORE) + "/known", "{}");
    HttpResponse<byte[]> wrongService = send("GET", stub.url(DownstreamStub.MS_USER) + "/known", null);

    assertEquals(501, wrongMethod.statusCode());
    assertEquals(501, wrongService.statusCode());
    assertEquals(2, stub.calls().size());
    assertFalse(stub.calls().get(0).matched());
    assertTrue(stub.calls().get(0).toString().endsWith("[UNSTUBBED]"));
  }

  @Test
  void recordsTheExactEncodedPathWhileMatchingTheDecodedPath() throws Exception {
    stub.on(DownstreamStub.MS_CORE, "GET", "/names/a/b c%2F\\+", Reply.status(204));

    HttpResponse<byte[]> response =
        send("GET", stub.url(DownstreamStub.MS_CORE) + "/names/a%2Fb%20c%252F%2B?q=%2F+%252F", null);

    assertEquals(204, response.statusCode());
    DownstreamStub.Call call = stub.calls().get(0);
    assertEquals("/names/a/b c%2F+", call.path());
    assertEquals("/names/a%2Fb%20c%252F%2B", call.rawPath());
    assertEquals("q=%2F+%252F", call.rawQuery());
    assertTrue(call.matched());
  }

  @Test
  void firstMatchingRuleWinsAndRulesCanComputeTheReply() throws Exception {
    stub.on(DownstreamStub.MS_USER, "POST", "/users/.*", call -> Reply.json(201, call.bodyText()));
    stub.on(DownstreamStub.MS_USER, "POST", "/users/special", Reply.status(500));

    HttpResponse<byte[]> response = send("POST", stub.url(DownstreamStub.MS_USER) + "/users/special", "{\"echo\":true}");

    assertEquals(201, response.statusCode());
    assertEquals("{\"echo\":true}", new String(response.body(), StandardCharsets.UTF_8));
  }

  @Test
  void callsAreFilteredPerServiceAndResetClearsEverything() throws Exception {
    stub.on(DownstreamStub.MS_CORE, "GET", ".*", Reply.status(204));
    stub.on(DownstreamStub.MS_USER, "GET", ".*", Reply.status(204));
    send("GET", stub.url(DownstreamStub.MS_CORE) + "/a", null);
    send("GET", stub.url(DownstreamStub.MS_USER) + "/b", null);
    send("GET", stub.url(DownstreamStub.MS_USER) + "/c", null);

    assertEquals(1, stub.callsTo(DownstreamStub.MS_CORE).size());
    assertEquals(2, stub.callsTo(DownstreamStub.MS_USER).size());
    assertEquals(0, stub.callsTo(DownstreamStub.MS_IAM).size());

    stub.reset();

    assertTrue(stub.calls().isEmpty());
    assertEquals(501, send("GET", stub.url(DownstreamStub.MS_CORE) + "/a", null).statusCode());
  }

  @Test
  void repliesCarryHeadersBinaryBodiesAndHeadHasNoBody() throws Exception {
    byte[] pdf = {'%', 'P', 'D', 'F', 0, (byte) 0xff, 10};
    stub.on(
        DownstreamStub.MS_DOCUMENT,
        "GET",
        "/doc",
        Reply.bytes(200, "application/octet-stream", pdf).header("Content-Disposition", "attachment; filename=a.pdf"));
    stub.on(DownstreamStub.MS_DOCUMENT, "HEAD", "/doc", Reply.status(200).header("X-Doc", "yes"));

    HttpResponse<byte[]> get = send("GET", stub.url(DownstreamStub.MS_DOCUMENT) + "/doc", null);
    HttpResponse<byte[]> head = send("HEAD", stub.url(DownstreamStub.MS_DOCUMENT) + "/doc", null);

    assertArrayEquals(pdf, get.body());
    assertEquals("attachment; filename=a.pdf", get.headers().firstValue("content-disposition").orElseThrow());
    assertEquals(200, head.statusCode());
    assertEquals(0, head.body().length);
    assertEquals("yes", head.headers().firstValue("x-doc").orElseThrow());
  }

  @Test
  void problemRepliesUseTheProblemMediaType() throws Exception {
    stub.on(DownstreamStub.MS_PRODUCT, "GET", ".*", Reply.problem(404, "No product found"));

    HttpResponse<byte[]> response = send("GET", stub.url(DownstreamStub.MS_PRODUCT) + "/product/x", null);

    assertEquals(404, response.statusCode());
    assertEquals("application/problem+json", response.headers().firstValue("content-type").orElseThrow());
    assertEquals("{\"status\":404,\"detail\":\"No product found\"}", new String(response.body(), StandardCharsets.UTF_8));
  }

  @Test
  void problemRepliesEscapeDetailsAsJson() throws Exception {
    String detail = "Invalid \"value\" at C:\\contracts\nTry again\t\u0000";
    stub.on(DownstreamStub.MS_PRODUCT, "GET", ".*", Reply.problem(400, detail));

    HttpResponse<byte[]> response = send("GET", stub.url(DownstreamStub.MS_PRODUCT) + "/product/x", null);
    JsonNode body = new ObjectMapper().readTree(response.body());

    assertEquals(400, response.statusCode());
    assertEquals("application/problem+json", response.headers().firstValue("content-type").orElseThrow());
    assertEquals(400, body.get("status").intValue());
    assertEquals(detail, body.get("detail").textValue());
  }

  @Test
  void delayedRepliesAreDelayed() throws Exception {
    stub.on(DownstreamStub.MS_IAM, "GET", ".*", Reply.status(204).delay(400));

    long start = System.nanoTime();
    send("GET", stub.url(DownstreamStub.MS_IAM) + "/slow", null);

    assertTrue((System.nanoTime() - start) / 1_000_000 >= 380);
  }

  @Test
  void abortClosesTheConnectionWithoutAnswering() {
    stub.on(DownstreamStub.MS_ONBOARDING, "GET", ".*", Reply.abort());

    assertThrows(IOException.class, () -> send("GET", stub.url(DownstreamStub.MS_ONBOARDING) + "/x", null));
    // an idempotent request may be repeated by the HTTP client: how many times is client specific
    assertFalse(stub.calls().isEmpty());
  }

  @Test
  void recordsRequestBodiesByteForByte() throws Exception {
    stub.on(DownstreamStub.PARTY_REGISTRY_PROXY, "POST", ".*", Reply.status(204));
    Multipart.Builder body = Multipart.body().field("note", "ciao").file("contract", "c.pdf", "application/pdf", new byte[] {1, 2, 3});

    HttpRequest request =
        HttpRequest.newBuilder(URI.create(stub.url(DownstreamStub.PARTY_REGISTRY_PROXY) + "/upload"))
            .header("Content-Type", body.contentType())
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.bytes()))
            .build();
    CLIENT.send(request, HttpResponse.BodyHandlers.discarding());

    DownstreamStub.Call call = stub.calls().get(0);
    List<Multipart.Part> parts = Multipart.parse(call.header("content-type"), call.body());
    assertEquals(2, parts.size());
    assertEquals("note", parts.get(0).name());
    assertEquals("ciao", parts.get(0).text());
    assertEquals("contract", parts.get(1).name());
    assertEquals("c.pdf", parts.get(1).filename());
    assertArrayEquals(new byte[] {1, 2, 3}, parts.get(1).content());
  }

  @Test
  void truncatedRepliesCloseBeforeTheirAdvertisedLength() {
    stub.on(DownstreamStub.MS_PRODUCT, "GET", "/product",
        Reply.json(200, "{\"id\":").truncated().header("X-Stub", "truncated").delay(1));

    assertTimeoutPreemptively(Duration.ofSeconds(3),
        () -> assertThrows(IOException.class, () -> send("GET", stub.url(DownstreamStub.MS_PRODUCT) + "/product", null)));
    assertEquals(1, stub.calls().size());
  }
}
