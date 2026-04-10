package it.pagopa.selfcare.onboarding.connector.rest.client;

import java.util.List;
import org.openapi.quarkus.user_json.api.InstitutionControllerApi;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.springframework.http.ResponseEntity;

@RegisterRestClient(configKey = "user_json")
public interface MsUserInstitutionApiClient extends InstitutionControllerApi {

    default ResponseEntity<List<UserInstitutionResponse>> _institutionsInstitutionIdUserInstitutionsGet(
        String institutionId,
        List<String> productRoles,
        List<String> products,
        List<String> roles,
        List<String> states,
        String userId
    ) {
        return ResponseEntity.ok(
            institutionsInstitutionIdUserInstitutionsGet(institutionId, productRoles, products, roles, states, userId)
                .await().indefinitely()
        );
    }
}
