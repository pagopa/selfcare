package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.model.error.UserClaims;
import org.openapi.quarkus.internal_json.model.UserResource;

public interface UserService {
    Uni<UserClaims> patchUser(String fiscalNumber, String name, String familyName, Boolean sameIdp);

    Uni<UserResource> getUserInfo(UserClaims userClaims);

}
