package it.pagopa.selfcare.onboarding.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OnboardingWorkflowInstitutionTest {

  private static final String CONFIRM_URL = "https://host/onboarding/confirm?jwt=";
  private static final String CONTRACT_UPDATE_URL = "https://host/onboarding/confirm?contract-update=true&jwt=";

  private final MailTemplatePlaceholdersConfig config = mock(MailTemplatePlaceholdersConfig.class);

  @Test
  void getConfirmTokenUrl_shouldReturnContractUpdateLinkForContractUpdate() {
    mockConfig();

    assertEquals(CONTRACT_UPDATE_URL, workflow(WorkflowType.CONTRACT_UPDATE).getConfirmTokenUrl(config));
  }

  @Test
  void getConfirmTokenUrl_shouldReturnDefaultLinkForOtherWorkflows() {
    mockConfig();

    assertEquals(CONFIRM_URL, workflow(WorkflowType.CONTRACT_REGISTRATION).getConfirmTokenUrl(config));
  }

  private void mockConfig() {
    when(config.confirmTokenPlaceholder()).thenReturn(CONFIRM_URL);
    when(config.confirmTokenContractUpdatePlaceholder()).thenReturn(CONTRACT_UPDATE_URL);
  }

  private static OnboardingWorkflowInstitution workflow(WorkflowType workflowType) {
    Onboarding onboarding = new Onboarding();
    onboarding.setWorkflowType(workflowType);
    return new OnboardingWorkflowInstitution(onboarding, "INSTITUTION");
  }
}
