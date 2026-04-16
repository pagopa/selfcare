package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

public interface OrchestrationService {

    Uni<OrchestrationResponse> triggerOrchestration(String currentOnboardingId, String timeout);

    Uni<OrchestrationResponse> triggerOrchestrationDeleteInstitutionAndUser(String currentOnboardingId);

}