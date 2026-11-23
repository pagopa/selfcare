package it.pagopa.selfcare.user.integration_test;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class IntegrationProfile implements QuarkusTestProfile {
  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
        "tenant.enforcement.enabled", "true",
        "%test.tenant.enforcement.enabled", "true",
        "tenant.ar.mail.enabled", "false",
        "tenant.pnpg.mail.enabled", "false",
        "tenant.ar.eventhub.users.enabled", "false",
        "tenant.ar.webhook.enabled", "false");
  }
}
