package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

@Slf4j
@ApplicationScoped
public class UserServiceDefault implements UserService {

    private final InstitutionApi userInstitutionApi;

    public UserServiceDefault(@RestClient InstitutionApi userInstitutionApi) {
        this.userInstitutionApi = userInstitutionApi;
    }

    @Override
    public Uni<List<UserInstitutionResponse>> retrieveUserInstitutions(String institutionId, List<String> productRoles, List<String> products, List<String> roles, List<String> states, String userId) {
        log.info("Calling userInstitutionApi.retrieveUserInstitutions");
        log.debug("retrieveUserInstitutions params: institutionId={}, productRoles={}, products={}, roles={}, states={}, userId={}",
                institutionId,
                productRoles.size(),
                products.size(),
                roles.size(),
                states.size(),
                userId);
        return userInstitutionApi.retrieveUserInstitutions(institutionId, productRoles, products, roles, states, userId);
    }

}
