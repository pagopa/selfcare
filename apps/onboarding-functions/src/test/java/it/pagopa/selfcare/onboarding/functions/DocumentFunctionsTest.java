package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.dto.EntityFilter;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.DocumentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.document_json.model.DocumentResponse;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class DocumentFunctionsTest {

  @Inject
  DocumentFunctions function;

  @InjectMock DocumentService documentService;

  @Inject
  ObjectMapper objectMapper;

  static ExecutionContext executionContext;

  static {
    executionContext = mock(ExecutionContext.class);
    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
  }

  @Test
  void deleteContract() throws JsonProcessingException {
    EntityFilter entity = EntityFilter.builder().value("123").build();
    String params = objectMapper.writeValueAsString(entity);
    when(documentService.deleteContract("123")).thenReturn(Response.ok().build());
    function.deleteContract(params, executionContext);
    verify(documentService, times(1)).deleteContract("123");

  }

  @Test
  void deleteContract_shouldThrowWhenDocumentServiceFails() throws JsonProcessingException {
    EntityFilter entity = EntityFilter.builder().value("123").build();
    String params = objectMapper.writeValueAsString(entity);
    when(documentService.deleteContract("123")).thenReturn(Response.status(500).build());

    assertThrows(
        GenericOnboardingException.class, () -> function.deleteContract(params, executionContext));
  }

  @Test
  void getLatestDocument_success() throws JsonProcessingException {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onb-1");
    onboarding.setProductId("prod-1");
    String onboardingString = objectMapper.writeValueAsString(onboarding);

    DocumentResponse response = DocumentResponse.builder().signingStep(2).build();
    when(documentService.getDocumentByOnboardingIdOrNull("onb-1")).thenReturn(response);

    DocumentResponse result = function.getLatestDocument(onboardingString, executionContext);

    assertNotNull(result);
    assertEquals(2, result.getSigningStep());
    verify(documentService, times(1)).getDocumentByOnboardingIdOrNull("onb-1");
  }

  @Test
  void getLatestDocument_shouldThrowWhenDocumentServiceFails() throws JsonProcessingException {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onb-2");
    onboarding.setProductId("prod-2");
    String onboardingString = objectMapper.writeValueAsString(onboarding);

    when(documentService.getDocumentByOnboardingIdOrNull("onb-2"))
        .thenThrow(new WebApplicationException(Response.status(500).build()));

    assertThrows(
        WebApplicationException.class, () -> function.getLatestDocument(onboardingString, executionContext));
  }
}
