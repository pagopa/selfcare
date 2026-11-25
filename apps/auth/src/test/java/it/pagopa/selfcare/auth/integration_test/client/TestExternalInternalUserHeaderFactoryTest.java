package it.pagopa.selfcare.auth.integration_test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TestExternalInternalUserHeaderFactoryTest {

  @ParameterizedTest
  @ValueSource(strings = {"AR", "PNPG"})
  void forwardsScenarioTenantWithTokenAndSubscriptionKey(String tenantId) {
    TestExternalInternalUserHeaderFactory factory = factoryForTenant(tenantId);

    var headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

    assertEquals(tenantId, headers.getFirst("X-Tenant-Id"));
    assertEquals("Bearer token", headers.getFirst("Authorization"));
    assertEquals("subscription", headers.getFirst("Ocp-Apim-Subscription-Key"));
    assertEquals(3, headers.size());
  }

  @Test
  void replacesStaleHeadersWithScenarioValues() {
    TestExternalInternalUserHeaderFactory factory = factoryForTenant("AR");
    MultivaluedMap<String, String> existingHeaders = new MultivaluedHashMap<>();
    existingHeaders.putSingle("X-Tenant-Id", "PNPG");
    existingHeaders.putSingle("Authorization", "Bearer stale-token");
    existingHeaders.putSingle("Ocp-Apim-Subscription-Key", "stale-key");

    var headers = factory.update(existingHeaders, existingHeaders);

    assertEquals("AR", headers.getFirst("X-Tenant-Id"));
    assertEquals("Bearer token", headers.getFirst("Authorization"));
    assertEquals("subscription", headers.getFirst("Ocp-Apim-Subscription-Key"));
    assertEquals(3, headers.size());
  }

  private TestExternalInternalUserHeaderFactory factoryForTenant(String tenantId) {
    SharedStepData sharedStepData = new SharedStepData();
    sharedStepData.setTenantId(tenantId);
    sharedStepData.setToken("token");
    TestExternalInternalUserHeaderFactory factory = new TestExternalInternalUserHeaderFactory();
    factory.apiKey = "subscription";
    factory.sharedStepData = sharedStepData;
    return factory;
  }
}
