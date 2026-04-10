package it.pagopa.selfcare.onboarding.connector.rest.client;

import org.openapi.quarkus.institution_json.api.OnboardingApi;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Ms Core Rest Client
 */
@RegisterRestClient(configKey = "institution_json")
public interface MsCoreOnboardingApiClient extends OnboardingApi {
}
