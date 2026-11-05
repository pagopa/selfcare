package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.common.DocumentType;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.product_json.model.ProductResponse;

class DocumentBuilderRequestMapperTest {

  private final DocumentBuilderRequestMapper mapper = new DocumentBuilderRequestMapperImpl();

  @Test
  void rootOnboardingId_isSetForContractUpdate() {
    assertEquals("rootId", toRequest(WorkflowType.CONTRACT_UPDATE, DocumentType.INSTITUTION).getRootOnboardingId());
  }

  @Test
  void rootOnboardingId_isSetForUserDocuments() {
    assertEquals("rootId", toRequest(WorkflowType.USERS, DocumentType.USER).getRootOnboardingId());
  }

  @Test
  void rootOnboardingId_isNotSetForOtherInstitutionDocuments() {
    assertNull(toRequest(WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR, DocumentType.INSTITUTION).getRootOnboardingId());
  }

  private DocumentBuilderRequest toRequest(WorkflowType workflowType, DocumentType documentType) {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setReferenceOnboardingId("rootId");
    onboarding.setWorkflowType(workflowType);
    OnboardingWorkflow workflow = mock(OnboardingWorkflow.class);
    when(workflow.getDocumentType()).thenReturn(documentType);
    return mapper.toRequest(onboarding, new ProductResponse(), workflow);
  }
}
