package it.pagopa.selfcare.onboarding.connector.rest.client;

import java.util.List;
import org.openapi.quarkus.onboarding_json.api.SupportApi;
import org.openapi.quarkus.onboarding_json.model.OnboardingResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingStatus;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingSupportApiClient extends SupportApi {

    default List<OnboardingResponse> _onboardingInstitutionUsingGET(
        String origin,
        String originId,
        OnboardingStatus status,
        String subunitCode,
        String taxCode
    ) {
        return onboardingInstitutionUsingGET(origin, originId, status, subunitCode, taxCode).await().indefinitely();
    }
}
