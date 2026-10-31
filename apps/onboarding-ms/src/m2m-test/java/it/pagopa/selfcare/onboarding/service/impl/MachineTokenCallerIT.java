package it.pagopa.selfcare.onboarding.service.impl;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.m2m.MachineTokenCallerContract;
import it.pagopa.selfcare.onboarding.service.OrchestrationService;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;

@QuarkusTest
@TestProfile(MachineTokenCallerIT.Profile.class)
class MachineTokenCallerIT extends MachineTokenCallerContract {
  @Inject OrchestrationService orchestration;
  @Inject TenantContext tenantContext;

  public static class Profile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return configuration("org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi");
    }
  }

  @Override
  protected void invoke(String tenant) {
    tenantContext.setTenantId(tenant);
    orchestration.triggerOrchestrationIfEnabled(
        "m2m-integration-" + tenant, null, "m2m:onboarding-ms")
        .await().atMost(Duration.ofSeconds(15));
  }
}
