package it.pagopa.selfcare.onboarding.connector.rest.client;

import org.openapi.quarkus.onboarding_json.api.BillingPortalApi;
import org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.springframework.http.ResponseEntity;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingBillingApiClient extends BillingPortalApi {

    default ResponseEntity<RecipientCodeStatus> _checkRecipientCode(String originId, String recipientCode) {
        return ResponseEntity.ok(checkRecipientCode(originId, recipientCode).await().indefinitely());
    }
}
