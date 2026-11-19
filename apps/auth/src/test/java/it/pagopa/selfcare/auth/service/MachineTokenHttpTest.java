package it.pagopa.selfcare.auth.service;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(MachineTokenHttpTest.HttpProfile.class)
class MachineTokenHttpTest {
  @Inject Config config;

  @Test
  void explicitlyEnabledHttpIssuesSessionWithoutTlsSecretsOrGrants() throws Exception {
    var response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                        URI.create(
                            "http://localhost:"
                                + config.getValue("auth.m2m.port", Integer.class)
                                + "/token/service"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            """
                            {"tenantId":"PNPG"}
                            """))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
    assertTrue(response.body().contains("access_token"));
    assertFalse(response.body().contains("\"scope\""));
  }

  @Test
  void httpOptInDoesNotExposeTokenOnMainListener() {
    given()
        .contentType("application/json")
        .body(
            """
            {"tenantId":"PNPG"}
            """)
        .post("/token/service")
        .then()
        .statusCode(404);
  }

  public static class HttpProfile extends MachineTokenTenantTest.PnpgProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      var values = super.getConfigOverrides();
      values.put("auth.m2m.tls-enabled", "false");
      values.remove("auth.m2m.certificate-pem");
      values.remove("auth.m2m.private-key-pem");
      return values;
    }
  }
}
