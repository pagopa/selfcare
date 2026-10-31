package it.pagopa.selfcare.m2m;

import static org.junit.jupiter.api.Assertions.*;

import com.azure.core.credential.AccessToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusMock;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/** Shared contract, compiled only by the two caller integration-test profiles. */
public abstract class MachineTokenCallerContract {
  private final HttpClient http = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();
  private final MutableClock clock = new MutableClock();
  private final AtomicReference<String> credential = new AtomicReference<>();

  protected abstract void invoke(String tenant);

  public static Map<String, String> configuration(String api) {
    Map<String, String> values = new HashMap<>();
    values.put("quarkus.http.test-port", "0");
    values.put("quarkus.mongodb.devservices.enabled", "false");
    values.put("onboarding-cdc.mongodb.watch.enabled", "false");
    values.put("selfcare.m2m.enabled", "true");
    values.put("selfcare.m2m.endpoint", "https://unused.invalid/token/service");
    values.put("selfcare.m2m.entra-scope", "api://auth/.default");
    values.put("selfcare.m2m.audience", "onboarding-functions");
    values.put("selfcare.m2m.scope", "onboarding-functions.invoke");
    values.put("onboarding.orchestration.enabled", "true");
    values.put("onboarding-ms.onboarding-functions-api-key", "m2m-test-function-key");
    values.put("onboarding-cdc.onboarding-functions-api-key", "m2m-test-function-key");
    values.put("onboarding-ms.jwt-bearer-token", "must-not-be-used");
    values.put("onboarding-cdc.jwt-bearer-token", "must-not-be-used");
    values.put("mp.jwt.verify.publickey", required("M2M_TEST_PUBLIC_KEY"));
    values.put("quarkus.rest-client.\"" + api + "\".url", required("M2M_TEST_FUNCTIONS_URL"));
    return values;
  }

  @BeforeEach
  void installRealClientWithLocalIdentitySource() throws Exception {
    credential.set(required("M2M_TEST_IDENTITY"));
    MachineTokenClient client = new MachineTokenClient(
        context -> {
          assertEquals(java.util.List.of("api://auth/.default"), context.getScopes());
          return Mono.just(new AccessToken(credential.get(), OffsetDateTime.now().plusHours(1)));
        },
        http, URI.create(required("M2M_TEST_AUTH_URL") + "/token/service"),
        "api://auth/.default", clock);
    QuarkusMock.installMockForType(client, MachineTokenClient.class);
    assertEquals(200, http.send(HttpRequest.newBuilder(receiptsUri()).DELETE().build(),
        HttpResponse.BodyHandlers.discarding()).statusCode());
  }

  @Test
  void usesRealIssuerAndValidatorForBothTenantsAndRenewsAfterFourMinutes() throws Exception {
    invoke("AR");
    invoke("PNPG");
    clock.advance(239);
    invoke("AR");
    invoke("PNPG");
    clock.advance(1);
    invoke("AR");
    invoke("PNPG");

    JsonNode receipts = receipts();
    assertEquals(6, receipts.size());
    for (int i = 0; i < receipts.size(); i++) {
      assertEquals(200, receipts.get(i).path("status").asInt());
      assertEquals(required("M2M_TEST_SERVICE"), receipts.get(i).path("service").asText());
      assertEquals(i % 2 == 0 ? "AR" : "PNPG", receipts.get(i).path("tenant").asText());
      assertEquals(300, receipts.get(i).path("lifetime").asInt());
    }
    assertEquals(receipts.get(0).path("jti"), receipts.get(2).path("jti"));
    assertEquals(receipts.get(1).path("jti"), receipts.get(3).path("jti"));
    assertNotEquals(receipts.get(0).path("jti"), receipts.get(4).path("jti"));
    assertNotEquals(receipts.get(1).path("jti"), receipts.get(5).path("jti"));
    assertNotEquals(receipts.get(0).path("jti"), receipts.get(1).path("jti"));
  }

  @Test
  void renewalFailureDoesNotReuseCachedTokenOrSendLegacyCredentials() throws Exception {
    invoke("AR");
    credential.set("invalid-identity");
    clock.advance(240);
    assertThrows(RuntimeException.class, () -> invoke("AR"));
    assertEquals(1, receipts().size());

    credential.set(required("M2M_TEST_IDENTITY"));
    invoke("AR");
    JsonNode receipts = receipts();
    assertEquals(2, receipts.size());
    assertNotEquals(receipts.get(0).path("jti"), receipts.get(1).path("jti"));
  }

  private JsonNode receipts() throws Exception {
    var response = http.send(HttpRequest.newBuilder(receiptsUri()).GET().build(),
        HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    return mapper.readTree(response.body());
  }

  private URI receiptsUri() {
    return URI.create(required("M2M_TEST_FUNCTIONS_URL") + "/_requests");
  }

  private static String required(String name) {
    return Objects.requireNonNull(System.getenv(name), "Run the M2M suite launcher: missing " + name);
  }

  private static final class MutableClock extends Clock {
    private Instant now = Instant.now();

    synchronized void advance(long seconds) {
      now = now.plusSeconds(seconds);
    }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) {
      if (!ZoneOffset.UTC.equals(zone)) {
        throw new IllegalArgumentException("Only UTC is supported by this test clock");
      }
      return this;
    }
    @Override public synchronized Instant instant() { return now; }
  }
}
