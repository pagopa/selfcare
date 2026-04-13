package it.pagopa.selfcare.onboarding.connector.rest.client;

import org.openapi.quarkus.onboarding_functions_json.api.OrganizationApi;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "onboarding_functions_json")
public interface OnboardingFunctionsApiClient extends OrganizationApi {

    default void _checkOrganization(String fiscalCode, String vatNumber) {
        checkOrganization(fiscalCode, vatNumber).await().indefinitely();
    }
}
