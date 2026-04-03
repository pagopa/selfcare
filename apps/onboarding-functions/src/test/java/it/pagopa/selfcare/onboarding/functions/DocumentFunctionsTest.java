package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.dto.EntityFilter;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;

import java.util.logging.Logger;

import static org.mockito.Mockito.*;

@QuarkusTest
public class DocumentFunctionsTest {

  @Inject
  DocumentFunctions function;

  @RestClient
  @InjectMock
  DocumentContentControllerApi documentContentControllerApi;


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
    when(documentContentControllerApi.deleteContract("123")).thenReturn(Response.ok().build());
    function.deleteContract(params, executionContext);
    verify(documentContentControllerApi, times(1)).deleteContract("123");

  }

  @Test
  void deleteContract_shouldThrowWhenDocumentServiceFails() throws JsonProcessingException {
    EntityFilter entity = EntityFilter.builder().value("123").build();
    String params = objectMapper.writeValueAsString(entity);
    when(documentContentControllerApi.deleteContract("123")).thenReturn(Response.status(500).build());

    org.junit.jupiter.api.Assertions.assertThrows(
        GenericOnboardingException.class,
        () -> function.deleteContract(params, executionContext));
  }
}
