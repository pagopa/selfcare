package it.pagopa.selfcare.onboarding.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.client.auth.FunctionTenantContext;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.OnboardingAttachment;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the hand-off that lets a background activity act for the right tenant: the tenant travels
 * inside the orchestration payload and is published when that payload is deserialised.
 */
class UtilsTenantTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final FunctionTenantContext tenantContext = new FunctionTenantContext();

  @AfterEach
  void tearDown() {
    FunctionTenantContext.clear();
  }

  @Test
  void publishesTheTenantCarriedByTheOnboarding() {
    String payload = "{\"id\":\"onboarding-1\",\"productId\":\"prod-io\",\"tenantId\":\"PNPG\"}";

    Onboarding onboarding = Utils.readOnboardingValue(objectMapper, payload);

    assertEquals("PNPG", onboarding.getTenantId());
    assertEquals("PNPG", tenantContext.currentTenantId());
  }

  /**
   * A previous activity's tenant must never survive into this one: the value is overwritten even
   * when the new onboarding carries none, otherwise calls would be made for the wrong tenant.
   */
  @Test
  void clearsThePreviousTenantWhenTheOnboardingCarriesNone() {
    FunctionTenantContext.set("AR");
    String legacyPayload = "{\"id\":\"onboarding-legacy\",\"productId\":\"prod-io\"}";

    Utils.readOnboardingValue(objectMapper, legacyPayload);

    assertNull(tenantContext.currentTenantId());
  }

  @Test
  void publishesTheTenantCarriedByAnAggregateInput() {
    String payload = "{\"id\":\"aggregate-1\",\"productId\":\"prod-io\",\"tenantId\":\"AR\"}";

    OnboardingAggregateOrchestratorInput input =
        Utils.readOnboardingAggregateOrchestratorInputValue(objectMapper, payload);

    assertEquals("AR", input.getTenantId());
    assertEquals("AR", tenantContext.currentTenantId());
  }

  @Test
  void clearsThePreviousTenantWhenAnAggregateInputCarriesNone() {
    FunctionTenantContext.set("PNPG");

    Utils.readOnboardingAggregateOrchestratorInputValue(
        objectMapper, "{\"id\":\"aggregate-legacy\",\"productId\":\"prod-io\"}");

    assertNull(tenantContext.currentTenantId());
  }

  @Test
  void publishesTheTenantCarriedByAWorkflowInput() {
    OnboardingWorkflow workflow =
        Utils.readOnboardingWorkflowValue(
            objectMapper,
            "{\"type\":\"INSTITUTION\",\"onboarding\":{\"tenantId\":\"PNPG\"}}");

    assertEquals("PNPG", workflow.getOnboarding().getTenantId());
    assertEquals("PNPG", tenantContext.currentTenantId());
  }

  @Test
  void publishesTheTenantCarriedByAnAttachmentInput() {
    OnboardingAttachment attachment =
        Utils.readOnboardingAttachmentValue(
            objectMapper, "{\"onboarding\":{\"tenantId\":\"AR\"}}");

    assertEquals("AR", attachment.getOnboarding().getTenantId());
    assertEquals("AR", tenantContext.currentTenantId());
  }
}
