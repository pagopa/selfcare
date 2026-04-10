package it.pagopa.selfcare.onboarding.connector.rest.client;

import java.util.List;
import org.openapi.quarkus.user_json.api.UserControllerApi;
import org.openapi.quarkus.user_json.model.PartyRole;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.springframework.http.ResponseEntity;

@RegisterRestClient(configKey = "user_json")
public interface MsUserApiClient extends UserControllerApi {

    default ResponseEntity<List<UserInstitutionResponse>> _usersGet(
        String institutionId,
        Integer page,
        List<String> productRoles,
        List<String> products,
        List<PartyRole> roles,
        Integer size,
        List<String> states,
        String userId
    ) {
        return ResponseEntity.ok(usersGet(institutionId, page, productRoles, products, roles, size, states, userId).await().indefinitely());
    }
}
