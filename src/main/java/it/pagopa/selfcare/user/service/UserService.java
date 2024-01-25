package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import io.smallrye.mutiny.Uni;

import java.util.List;

public interface UserService {
    Uni<List<String>> getUsersEmails(String institutionId, String productId);
    Multi<UserProductResponse> getUserProductsByInstitution(String institutionId);
    Uni<UserResource> retrievePerson(String userId, String productId, String institutionId);
    Multi<UserInstitutionResponse> findAllUserInstitutions(String institutionId,
                                                           String userId,
                                                           List<PartyRole> roles,
                                                           List<OnboardedProductState> states,
                                                           List<String> products,
                                                           List<String> productRoles);
}
