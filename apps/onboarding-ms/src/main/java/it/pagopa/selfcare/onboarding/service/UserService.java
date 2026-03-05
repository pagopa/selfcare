package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

import java.util.List;

public interface UserService {

    Uni<List<UserInstitutionResponse>> retrieveUserInstitutions(String institutionId, List<String> productRoles, List<String> products, List<String> roles, List<String> states, String userId);

}