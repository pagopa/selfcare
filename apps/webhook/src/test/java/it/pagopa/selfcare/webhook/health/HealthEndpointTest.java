package it.pagopa.selfcare.webhook.health;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards the health check wiring: the outbox lag check reports a <b>global</b> condition, so it
 * must stay out of the readiness probe (otherwise a backlog would make every replica unready at
 * once and the API would start answering 503) and must be reachable on the non-gating {@code
 * diagnostics} group instead.
 */
@QuarkusTest
@TestProfile(HealthEndpointTest.NoMongoHealthProfile.class)
class HealthEndpointTest {

  /**
   * Disables the built-in MongoDB health check: no MongoDB instance runs during unit tests, so it
   * would time out and make the readiness endpoint unresponsive. The checks under test do not need
   * MongoDB, since the outbox check short-circuits while the Storage Queue is disabled.
   */
  public static class NoMongoHealthProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("quarkus.mongodb.health.enabled", "false");
    }
  }

  private static final String OUTBOX_CHECK = "webhook-outbox-lag";

  @Test
  void readinessShouldNotIncludeOutboxLagCheck() {
    given()
        .when()
        .get("/q/health/ready")
        .then()
        .statusCode(200)
        .body("checks.name", not(hasItem(OUTBOX_CHECK)));
  }

  @Test
  void diagnosticsGroupShouldIncludeOutboxLagCheck() {
    given()
        .when()
        .get("/q/health/group/" + WebhookOutboxLagCheck.DIAGNOSTICS_GROUP)
        .then()
        .statusCode(200)
        .body("checks.name", hasItem(OUTBOX_CHECK));
  }
}
