package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import org.openapi.quarkus.user_registry_json.model.UserResource;

public interface UserService {
    Multi<UserProductResponse> getUserProductsByInstitution(String institutionId);
    Uni<UserResource> retrievePerson(String userId, String productId, String institutionId);

}
