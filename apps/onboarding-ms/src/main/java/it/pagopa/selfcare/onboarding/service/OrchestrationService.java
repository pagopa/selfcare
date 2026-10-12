package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

public interface OrchestrationService {

    String ONBOARDING_MS_CALLER_ID = "m2m:onboarding-ms";

    default Uni<OrchestrationResponse> triggerOrchestrationIfEnabled(
            String currentOnboardingId, String timeout) {
        return triggerOrchestrationIfEnabled(currentOnboardingId, timeout, ONBOARDING_MS_CALLER_ID);
    }

    Uni<OrchestrationResponse> triggerOrchestrationIfEnabled(
            String currentOnboardingId, String timeout, String requesterUserId);

    default Uni<OrchestrationResponse> triggerOrchestrationDeleteInstitutionAndUser(
            String currentOnboardingId) {
        return triggerOrchestrationDeleteInstitutionAndUser(currentOnboardingId, ONBOARDING_MS_CALLER_ID);
    }

    Uni<OrchestrationResponse> triggerOrchestrationDeleteInstitutionAndUser(
            String currentOnboardingId, String requesterUserId);

}
