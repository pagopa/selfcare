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
import it.pagopa.selfcare.onboarding.service.TelemetryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentResponse;

import java.util.Map;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.GET_LATEST_DOCUMENT_ACTIVITY;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class DocumentFunctions {
  private static final String FORMAT_LOGGER_INSTITUTION_STRING = "%s: %s";
  private final ObjectMapper objectMapper;
  private final TelemetryService telemetryService;

  @RestClient @Inject DocumentContentControllerApi documentContentControllerApi;
  @RestClient @Inject DocumentControllerApi documentControllerApi;

  public DocumentFunctions(ObjectMapper objectMapper, TelemetryService telemetryService) {
    this.objectMapper = objectMapper;
    this.telemetryService =  telemetryService;
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
    try (Response response = documentContentControllerApi.deleteContract(onboardingId)) {
      if (response == null
          || response.getStatusInfo() == null
          || !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
        int status = response != null ? response.getStatus() : -1;
        context
          .getLogger()
          .warning(
            () ->
              String.format(
                "Document service deleteContract failed for onboardingId=%s status=%s",
                onboardingId,
                status));
        throw new GenericOnboardingException(
            String.format(
                "Unable to delete contract for onboarding %s. status=%s",
                onboardingId,
                status));
      }
      context
        .getLogger()
        .fine(
          () ->
            String.format(
              "Document service deleteContract succeeded for onboardingId=%s status=%s",
              onboardingId,
              response.getStatus()));
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
    try {
      return documentControllerApi.getDocumentByOnboardingId(onboarding.getId());
    } catch (WebApplicationException e) {
      if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
        context
            .getLogger()
            .warning(
                () -> String.format("Document not found for onboardingId=%s", onboarding.getId()));
        return null;
      }
      throw e;
    }
  }
}
