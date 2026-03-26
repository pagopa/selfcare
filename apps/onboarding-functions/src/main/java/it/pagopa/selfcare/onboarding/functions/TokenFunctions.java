package it.pagopa.selfcare.onboarding.functions;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.DELETE_TOKEN_CONTRACT_ACTIVITY_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import it.pagopa.selfcare.onboarding.dto.EntityFilter;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;

public class TokenFunctions {
  private static final String FORMAT_LOGGER_INSTITUTION_STRING = "%s: %s";
  private final ObjectMapper objectMapper;

  @RestClient @Inject DocumentContentControllerApi documentContentControllerApi;

  public TokenFunctions(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }


  /**
   * Deletion contract from document storage after copy this blob to deleted contract storage
   */
  @FunctionName(DELETE_TOKEN_CONTRACT_ACTIVITY_NAME)
  public void deleteContract(
    @DurableActivityTrigger(name = "filtersString") String filtersString,
    final ExecutionContext context) throws JsonProcessingException {

    context
      .getLogger()
      .info(() -> String.format(FORMAT_LOGGER_INSTITUTION_STRING, DELETE_TOKEN_CONTRACT_ACTIVITY_NAME, filtersString));

    EntityFilter entityFilter = objectMapper.readValue(filtersString, EntityFilter.class);
    String onboardingId = entityFilter.getValue();
    documentContentControllerApi.deleteContract(onboardingId);
  }
}
