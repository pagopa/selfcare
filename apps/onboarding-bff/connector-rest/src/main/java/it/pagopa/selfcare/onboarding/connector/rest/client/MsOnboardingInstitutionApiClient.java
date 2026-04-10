package it.pagopa.selfcare.onboarding.connector.rest.client;

import java.util.List;
import org.openapi.quarkus.onboarding_json.api.InstitutionControllerApi;
import org.openapi.quarkus.onboarding_json.model.GetInstitutionRequest;
import org.openapi.quarkus.onboarding_json.model.InstitutionResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.springframework.http.ResponseEntity;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingInstitutionApiClient extends InstitutionControllerApi {

    default ResponseEntity<List<InstitutionResponse>> _getInstitutions(GetInstitutionRequest request) {
        return ResponseEntity.ok(getInstitutions(request).await().indefinitely());
    }
}
