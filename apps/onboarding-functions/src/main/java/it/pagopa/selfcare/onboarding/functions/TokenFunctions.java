package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import io.quarkus.mongodb.panache.common.PanacheUpdate;
import it.pagopa.selfcare.onboarding.dto.EntityFilter;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.service.ContractService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.DELETE_TOKEN_CONTRACT_ACTIVITY_NAME;

public class TokenFunctions {
  private static final String FORMAT_LOGGER_INSTITUTION_STRING = "%s: %s";
  private final OnboardingService onboardingService;
  private final ContractService contractService;
  private final ObjectMapper objectMapper;

  public TokenFunctions(ObjectMapper objectMapper, OnboardingService onboardingService, ContractService contractService) {
    this.objectMapper = objectMapper;
    this.onboardingService = onboardingService;
    this.contractService = contractService;
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
    Optional<Token> token = onboardingService.getToken(entityFilter.getValue());
    token.ifPresent(t -> {
      t.setContractSigned(contractService.deleteContract(Objects.requireNonNullElse(t.getContractSigned(), ""), true));
      t.setContractFilename(contractService.deleteContract(t.getOnboardingId() + "/" + Objects.requireNonNullElse(t.getContractFilename(), ""), false));
      long response = onboardingService.updateTokenContractFiles(t);
      context
        .getLogger()
        .info(() -> String.format(FORMAT_LOGGER_INSTITUTION_STRING, DELETE_TOKEN_CONTRACT_ACTIVITY_NAME, "Update token status: " + (response > 0 ? "OK" : "KO")));
    });
    context
      .getLogger()
      .info(() -> String.format(FORMAT_LOGGER_INSTITUTION_STRING, DELETE_TOKEN_CONTRACT_ACTIVITY_NAME, "complete"));
  }
}
