package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.UpdateUserRequest;
import org.openapi.quarkus.user_registry_json.model.*;

import java.util.List;

public interface UserRegistryService {
    Uni<List<UserInstitution>> updateUserRegistry(UpdateUserRequest updateUserRequest, String userId, String institutionId);
    Uni<UserResource> findByIdUsingGET( String fl, String id);
    Uni<UserId> saveUsingPATCH(SaveUserDto saveUserDto);
    Uni<UserResource> searchUsingPOST(String fl, UserSearchDto userSearchDto);
    Uni<jakarta.ws.rs.core.Response> updateUsingPATCH(String id, MutableUserFieldsDto mutableUserFieldsDto );

}
