package it.pagopa.selfcare.onboarding.client.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.m2m.MachineTokenCallerContract;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.onboarding_functions_json.api.NotificationsApi;
import org.openapi.quarkus.onboarding_functions_json.model.Onboarding;

@QuarkusTest
@TestProfile(MachineTokenCallerIT.Profile.class)
class MachineTokenCallerIT extends MachineTokenCallerContract {
  @Inject @RestClient NotificationsApi notifications;

  public static class Profile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return configuration("org.openapi.quarkus.onboarding_functions_json.api.NotificationsApi");
    }
  }

  @Override
  protected void invoke(String tenant) {
    try (TenantContext.Scope ignored = TenantContext.open(tenant)) {
      notifications.apiNotificationPost("ADD", "m2m:onboarding-cdc", new Onboarding())
          .await().atMost(Duration.ofSeconds(15));
    }
  }
}
