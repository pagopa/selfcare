package it.pagopa.selfcare.onboarding.connector.rest.client;

import java.util.List;
import org.openapi.quarkus.user_json.api.InstitutionControllerApi;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "user_json")
public interface MsUserInstitutionApiClient extends InstitutionControllerApi {

    default List<UserInstitutionResponse> _institutionsInstitutionIdUserInstitutionsGet(
        String institutionId,
        List<String> productRoles,
        List<String> products,
        List<String> roles,
        List<String> states,
        String userId
    ) {
        return institutionsInstitutionIdUserInstitutionsGet(institutionId, productRoles, products, roles, states, userId)
            .await().indefinitely();
    }
}
