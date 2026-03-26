package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.dto.EntityFilter;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.mockito.Mockito.*;

@QuarkusTest
public class TokenFunctionsTest {

  @Inject
  TokenFunctions function;

  @InjectMock
  OnboardingService onboardingService;


  @Inject
  ObjectMapper objectMapper;

  static ExecutionContext executionContext;

  static {
    executionContext = mock(ExecutionContext.class);
    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
  }

  @Test
  void deleteContract() throws JsonProcessingException {
    Token tokenOriginal = new Token();
    tokenOriginal.setContractSigned("parties/docs/123/file.pdf");
    Token tokenDeleted = new Token();
    tokenDeleted.setContractSigned("parties/deleted/123/file.pdf");


    EntityFilter entity = EntityFilter.builder().value("123").build();
    String params = objectMapper.writeValueAsString(entity);
    function.deleteContract(params, executionContext);

  }
}
