package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.List;
import it.pagopa.selfcare.user.entity.UserInstitution;


public interface UserService {
    Uni<List<String>> getUsersEmails(String institutionId, String productId);
    Multi<UserProductResponse> getUserProductsByInstitution(String institutionId);
    Uni<UserResource> retrievePerson(String userId, String productId, String institutionId);
    Uni<List<UserInstitution>> retrieveBindings(String institutionId, String userId, String[] states);
}
