package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;

import java.util.List;

public interface UserService {
    Multi<UserProductResponse> getUserProductsByInstitution(String institutionId);

    Uni<List<UserInstitution>> retrieveBindings(String institutionId, String userId, String[] states);

}
