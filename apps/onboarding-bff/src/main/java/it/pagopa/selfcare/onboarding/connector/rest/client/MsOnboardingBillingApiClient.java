package it.pagopa.selfcare.onboarding.connector.rest.client;

import org.openapi.quarkus.onboarding_json.api.BillingPortalApi;
import org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingBillingApiClient extends BillingPortalApi {

    default RecipientCodeStatus _checkRecipientCode(String originId, String recipientCode) {
        return checkRecipientCode(originId, recipientCode).await().indefinitely();
    }
}
