package it.pagopa.selfcare.onboarding.functions;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.FORMAT_LOGGER_ONBOARDING_STRING;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.DELETE_TOKEN_CONTRACT_ACTIVITY_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import it.pagopa.selfcare.onboarding.dto.EntityFilter;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.service.DocumentService;
import it.pagopa.selfcare.onboarding.service.TelemetryService;
import jakarta.ws.rs.core.Response;
import org.openapi.quarkus.document_json.model.DocumentResponse;

import java.util.Map;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.GET_LATEST_DOCUMENT_ACTIVITY;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class DocumentFunctions {
  private static final String FORMAT_LOGGER_INSTITUTION_STRING = "%s: %s";
  private final ObjectMapper objectMapper;
  private final TelemetryService telemetryService;
  private final DocumentService documentService;

  public DocumentFunctions(
      ObjectMapper objectMapper,
      TelemetryService telemetryService,
      DocumentService documentService) {
    this.objectMapper = objectMapper;
    this.telemetryService = telemetryService;
    this.documentService = documentService;
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
    context
      .getLogger()
      .info(() -> String.format("Deleting contract for onboardingId=%s", onboardingId));
    try (Response response = documentService.deleteContract(onboardingId)) {
      ensureSuccessfulDocumentResponse(response, "delete contract", onboardingId);
    }
  }

  @FunctionName(GET_LATEST_DOCUMENT_ACTIVITY)
  public DocumentResponse getLatestDocument(
          @DurableActivityTrigger(name = "onboardingString") String onboardingString,
          final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
            GET_LATEST_DOCUMENT_ACTIVITY,
            String.format(
                    FORMAT_LOGGER_ONBOARDING_STRING,
                    GET_LATEST_DOCUMENT_ACTIVITY,
                    onboardingString),
            SeverityLevel.Information,
            Map.of(
                    "onboardingId", onboarding.getId(),
                    "productId", onboarding.getProductId()));
    DocumentResponse document =
        documentService.getDocumentByOnboardingIdOrNull(onboarding.getId());
    if (document == null) {
      context
          .getLogger()
          .warning(() -> String.format("Document not found for onboardingId=%s", onboarding.getId()));
    }
    return document;
  }

  private void ensureSuccessfulDocumentResponse(
      Response response, String operation, String onboardingId) {
    int status = response != null ? response.getStatus() : -1;
    if (response == null
        || response.getStatusInfo() == null
        || !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
      throw new GenericOnboardingException(
          String.format(
              "Document service call failed while trying to %s for onboarding %s. status=%s",
              operation, onboardingId, status));
    }
  }
}
